package com.android.kiyori.browser.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.android.kiyori.R
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.app.MainActivity
import com.android.kiyori.browser.data.BrowserBookmarkRepository
import com.android.kiyori.browser.data.BrowserHistoryRepository
import com.android.kiyori.browser.data.BrowserPreferencesRepository
import com.android.kiyori.browser.data.BrowserSearchHistoryRepository
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor.BrowserVideoCandidate
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.browser.web.BrowserWebViewCallbacks
import com.android.kiyori.browser.web.BrowserWebViewController
import com.android.kiyori.download.InternalDownloadRequest
import com.android.kiyori.download.requestDownloadWithPreferences
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.sniffer.DetectedVideo
import com.android.kiyori.sniffer.UrlDetector
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.applyOpenActivityTransitionCompat
import com.android.kiyori.utils.enableTransparentSystemBars
import com.android.kiyori.utils.ThemeManager

class BrowserActivity : BaseActivity() {

    companion object {
        private const val EXTRA_INITIAL_URL = "extra_initial_url"
        private const val EXTRA_OPEN_SEARCH = "extra_open_search"

        fun start(context: Context, url: String = "", openSearch: Boolean = false) {
            val intent = Intent(context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_URL, url)
                putExtra(EXTRA_OPEN_SEARCH, openSearch)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var browserWebViewController: BrowserWebViewController
    private lateinit var bookmarkRepository: BrowserBookmarkRepository
    private lateinit var preferencesRepository: BrowserPreferencesRepository
    private lateinit var historyRepository: BrowserHistoryRepository
    private lateinit var searchHistoryRepository: BrowserSearchHistoryRepository
    private var pageState by mutableStateOf(BrowserPageState())
    private var bookmarkFolders by mutableStateOf<List<BrowserBookmarkFolderOption>>(emptyList())
    private var initialUrlToLoad = ""
    private var shouldOpenSearch = false
    private var initialNavigationDispatched = false
    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissionsCallback? = null
    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingFileChooserCallback
            pendingFileChooserCallback = null
            callback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }
    private val geolocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            completePendingGeolocationRequest(granted = hasLocationPermission())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentSystemBars()

        handleLaunchIntent(intent)
        bookmarkRepository = BrowserBookmarkRepository(this)
        preferencesRepository = BrowserPreferencesRepository(this)
        historyRepository = BrowserHistoryRepository(this)
        searchHistoryRepository = BrowserSearchHistoryRepository(this)
        bookmarkFolders = bookmarkRepository.getFolders().map {
            BrowserBookmarkFolderOption(id = it.id, title = it.title)
        }
        browserWebViewController = BrowserWebViewController(
            preferencesRepository = preferencesRepository,
            historyRepository = historyRepository,
            searchHistoryRepository = searchHistoryRepository,
            callbacks = BrowserWebViewCallbacks(
                onStateChanged = { state -> pageState = state },
                onExternalUrlBlocked = ::handleExternalUrl,
                onGeolocationPermissionRequest = ::handleGeolocationPermissionRequest,
                onDownloadRequested = { url, userAgent, contentDisposition, mimeType, contentLength, sourcePageUrl ->
                    handleDownloadRequest(
                        url = url,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        contentLength = contentLength,
                        sourcePageUrl = sourcePageUrl
                    )
                },
                onShowFileChooser = { fileChooserParams, filePathCallback ->
                    openFileChooser(fileChooserParams, filePathCallback)
                }
            ),
            initialShowUrlBar = shouldOpenSearch
        )

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = Color(0xFF212121),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFF4F6F8),
                    onSurface = Color(0xFF212121)
                )
            ) {
                BrowserScreen(
                    state = pageState,
                    onAttachWebView = { webView ->
                        browserWebViewController.attach(webView)
                        dispatchInitialNavigation(webView)
                    },
                    onBackPressed = {
                        if (!browserWebViewController.exitCustomViewIfNeeded()) {
                            finish()
                            applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    },
                    onCloseBrowser = {
                        if (!browserWebViewController.exitCustomViewIfNeeded()) {
                            finish()
                            applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                    },
                    onOpenSettingsPage = {
                        MainActivity.start(
                            this,
                            initialTab = MainActivity.TAB_SETTINGS,
                            initialSettingsPage = MainActivity.SETTINGS_PAGE_BROWSER
                        )
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.no_anim, R.anim.no_anim)
                    },
                    onToggleUrlBar = {
                        if (pageState.showUrlBar && pageState.isBlankPage) {
                            browserWebViewController.loadHome()
                        } else {
                            browserWebViewController.setUrlBarVisible(!pageState.showUrlBar)
                        }
                    },
                    onInputChanged = browserWebViewController::updateInputUrl,
                    onSubmitInput = browserWebViewController::submitInputUrl,
                    onReload = browserWebViewController::reload,
                    onCopyCurrentUrl = {
                        copyBrowserUrl(this, pageState.currentUrl)
                    },
                    onGoBack = browserWebViewController::goBack,
                    onGoForward = browserWebViewController::goForward,
                    onGoHome = browserWebViewController::loadHome,
                    onSelectSearchEngine = browserWebViewController::selectSearchEngine,
                    onSearchAgainWithEngine = browserWebViewController::searchAgainWithEngine,
                    onDismissSearchEngineQuickSwitchBar = browserWebViewController::dismissSearchEngineQuickSwitchBar,
                    onOpenCurrentUrl = { browserWebViewController.loadUrl(pageState.currentUrl) },
                    onEditCurrentUrl = { browserWebViewController.updateInputUrl(pageState.currentUrl) },
                    onOpenSearchRecord = browserWebViewController::loadUrl,
                    onDeleteSearchRecord = browserWebViewController::deleteSearchRecord,
                    onClearSearchRecords = browserWebViewController::clearSearchHistory,
                    onToggleIncognitoMode = browserWebViewController::toggleIncognitoMode,
                    onOpenHistoryItem = browserWebViewController::loadUrl,
                    onDeleteHistoryItem = browserWebViewController::deleteHistoryItem,
                    onClearHistory = browserWebViewController::clearHistory,
                    onSelectUserAgentMode = browserWebViewController::setUserAgentMode,
                    getCustomGlobalUserAgent = browserWebViewController::getCustomGlobalUserAgent,
                    getCustomSiteUserAgent = browserWebViewController::getCustomSiteUserAgent,
                    getCurrentSiteHost = browserWebViewController::getCurrentSiteHost,
                    onSaveCustomGlobalUserAgent = browserWebViewController::saveCustomGlobalUserAgent,
                    onSaveCustomSiteUserAgent = browserWebViewController::saveCustomSiteUserAgent,
                    onDownloadDetectedVideo = ::downloadDetectedVideo,
                    onRequestPageSource = browserWebViewController::requestPageSource,
                    bookmarkFolders = bookmarkFolders,
                    onSaveBookmark = { draft ->
                        bookmarkRepository.upsertBookmark(
                            title = draft.title,
                            url = draft.url,
                            iconUrl = draft.iconUrl,
                            folderId = draft.folderId
                        )
                        bookmarkFolders = bookmarkRepository.getFolders().map {
                            BrowserBookmarkFolderOption(id = it.id, title = it.title)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
        if (::browserWebViewController.isInitialized) {
            if (initialUrlToLoad.isNotBlank()) {
                val targetUrl = initialUrlToLoad
                initialUrlToLoad = ""
                browserWebViewController.loadUrl(targetUrl)
            } else if (shouldOpenSearch) {
                browserWebViewController.setUrlBarVisible(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bookmarkRepository.isInitialized) {
            bookmarkFolders = bookmarkRepository.getFolders().map {
                BrowserBookmarkFolderOption(id = it.id, title = it.title)
            }
        }
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.onResume()
        }
    }

    override fun onPause() {
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = null
        completePendingGeolocationRequest(granted = false)
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.destroy()
        }
        super.onDestroy()
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val externalUrl = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString.orEmpty()
        initialUrlToLoad = if (externalUrl.isNotBlank()) {
            externalUrl
        } else {
            intent?.getStringExtra(EXTRA_INITIAL_URL).orEmpty()
        }
        shouldOpenSearch = externalUrl.isBlank() &&
            (intent?.getBooleanExtra(EXTRA_OPEN_SEARCH, false) == true)
    }

    private fun dispatchInitialNavigation(webView: com.tencent.smtt.sdk.WebView) {
        if (initialNavigationDispatched) {
            return
        }
        initialNavigationDispatched = true
        webView.post {
            if (isFinishing || isDestroyed) {
                return@post
            }

            when {
                initialUrlToLoad.isNotBlank() -> {
                    val targetUrl = initialUrlToLoad
                    initialUrlToLoad = ""
                    browserWebViewController.loadUrl(targetUrl)
                }
                !shouldOpenSearch -> {
                    browserWebViewController.loadHome()
                }
            }
        }
    }

    private fun openFileChooser(
        fileChooserParams: WebChromeClient.FileChooserParams?,
        filePathCallback: ValueCallback<Array<Uri>>?
    ): Boolean {
        if (filePathCallback == null) {
            return false
        }

        pendingFileChooserCallback?.onReceiveValue(null)
        pendingFileChooserCallback = filePathCallback

        val intent = runCatching {
            fileChooserParams?.createIntent()
        }.getOrNull() ?: buildFallbackFileChooserIntent(fileChooserParams)

        return runCatching {
            fileChooserLauncher.launch(intent)
            true
        }.getOrElse {
            pendingFileChooserCallback?.onReceiveValue(null)
            pendingFileChooserCallback = null
            false
        }
    }

    private fun buildFallbackFileChooserIntent(
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Intent {
        val acceptTypes = fileChooserParams?.acceptTypes
            ?.filter { it.isNotBlank() }
            ?.toTypedArray()
            ?: arrayOf("*/*")

        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (acceptTypes.size == 1) acceptTypes.first() else "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes)
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
            )
        }
    }

    private fun handleDownloadRequest(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        sourcePageUrl: String
    ) {
        if (url.isBlank()) {
            return
        }

        val headers = linkedMapOf<String, String>().apply {
            userAgent.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
            sourcePageUrl.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            RemotePlaybackHeaders.deriveOrigin(sourcePageUrl)?.let { put("Origin", it) }
        }

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType.ifBlank { null })
        val sniffHeaders = buildMap {
            if (mimeType.isNotBlank()) {
                put("Content-Type", mimeType)
            }
            if (contentDisposition.isNotBlank()) {
                put("Content-Disposition", contentDisposition)
            }
        }

        requestDownloadWithPreferences(
            context = this,
            request = InternalDownloadRequest(
                url = url,
                title = fileName,
                fileName = fileName,
                mimeType = mimeType,
                description = if (contentLength > 0L) "浏览器下载任务" else "准备下载",
                sourcePageUrl = sourcePageUrl,
                sourcePageTitle = pageState.title,
                mediaType = when {
                    UrlDetector.isVideo(url, sniffHeaders) -> "video"
                    UrlDetector.isAudio(url, sniffHeaders) -> "audio"
                    else -> ""
                },
                headers = headers
            ),
            contentLength = contentLength
        )
    }

    private fun downloadDetectedVideo(candidate: BrowserVideoCandidate) {
        val video = candidate.video
        if (video.url.isBlank()) {
            return
        }

        val contentDisposition = RemotePlaybackHeaders.get(video.headers, "Content-Disposition")
        val headers = RemotePlaybackHeaders.enrich(video.headers, video.pageUrl)
        val mimeType = UrlDetector.getMimeTypeForFormat(candidate.format, video.headers)
        val fileName = URLUtil.guessFileName(video.url, contentDisposition, mimeType.ifBlank { null })

        requestDownloadWithPreferences(
            context = this,
            request = InternalDownloadRequest(
                url = video.url,
                title = video.title.takeIf { it.isNotBlank() } ?: fileName,
                fileName = fileName,
                mimeType = mimeType,
                description = "视频下载任务",
                sourcePageUrl = video.pageUrl,
                sourcePageTitle = video.title,
                mediaType = "video",
                headers = headers
            )
        )
    }

    private fun handleExternalUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }

        val parsedIntent = runCatching {
            if (url.startsWith("intent:", ignoreCase = true)) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            }
        }.getOrNull() ?: return false

        val fallbackUrl = resolveExternalFallbackUrl(url, parsedIntent).orEmpty()
        if (!preferencesRepository.isWebPageOpenAppEnabled()) {
            return if (fallbackUrl.isNotBlank()) {
                browserWebViewController.loadUrl(fallbackUrl)
                true
            } else {
                false
            }
        }
        val canResolve = parsedIntent.resolveActivity(packageManager) != null

        if (!canResolve) {
            return if (fallbackUrl.isNotBlank()) {
                browserWebViewController.loadUrl(fallbackUrl)
                true
            } else if (!BrowserSecurityPolicy.shouldShowExternalNavigationError(url)) {
                true
            } else {
                Toast.makeText(this, "未找到可处理该链接的应用", Toast.LENGTH_SHORT).show()
                true
            }
        }

        return runCatching {
            startActivity(parsedIntent)
            true
        }.recoverCatching {
            if (fallbackUrl.isNotBlank()) {
                browserWebViewController.loadUrl(fallbackUrl)
                true
            } else if (!BrowserSecurityPolicy.shouldShowExternalNavigationError(url)) {
                true
            } else {
                Toast.makeText(this, "未找到可处理该链接的应用", Toast.LENGTH_SHORT).show()
                true
            }
        }.getOrDefault(false)
    }

    private fun handleGeolocationPermissionRequest(
        origin: String?,
        callback: GeolocationPermissionsCallback?
    ) {
        callback ?: return
        if (!preferencesRepository.isWebPageGeolocationEnabled()) {
            callback.invoke(origin, false, false)
            return
        }
        if (hasLocationPermission()) {
            callback.invoke(origin, true, false)
            return
        }

        completePendingGeolocationRequest(granted = false)
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        geolocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun completePendingGeolocationRequest(granted: Boolean) {
        val callback = pendingGeolocationCallback ?: run {
            pendingGeolocationOrigin = null
            return
        }
        callback.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun resolveExternalFallbackUrl(
        rawUrl: String,
        parsedIntent: Intent
    ): String? {
        parsedIntent.getStringExtra("browser_fallback_url")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        if (rawUrl.startsWith("intent:", ignoreCase = true)) {
            parsedIntent.dataString
                ?.takeIf { BrowserSecurityPolicy.shouldLoadInsideWebView(it) }
                ?.let { return it }
        }

        return null
    }
}

