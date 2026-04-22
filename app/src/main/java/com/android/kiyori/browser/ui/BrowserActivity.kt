package com.android.kiyori.browser.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.android.kiyori.browser.web.BrowserWebViewCallbacks
import com.android.kiyori.browser.web.BrowserWebViewController
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.sniffer.DetectedVideo
import com.android.kiyori.sniffer.UrlDetector
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.applyOpenActivityTransitionCompat
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
    private var pendingFileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingFileChooserCallback
            pendingFileChooserCallback = null
            callback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        if (initialUrlToLoad.isNotBlank()) {
                            val targetUrl = initialUrlToLoad
                            initialUrlToLoad = ""
                            browserWebViewController.loadUrl(targetUrl)
                        }
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
                        MainActivity.start(this, MainActivity.TAB_SETTINGS)
                        applyOpenActivityTransitionCompat(R.anim.no_anim, R.anim.no_anim)
                    },
                    onToggleUrlBar = {
                        browserWebViewController.setUrlBarVisible(!pageState.showUrlBar)
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
                    },
                    onOpenBookmarksPage = {
                        BrowserBookmarksActivity.start(this)
                        applyOpenActivityTransitionCompat(R.anim.no_anim, R.anim.no_anim)
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

        val isPlayableMedia = UrlDetector.isVideo(url, mapOf("Content-Type" to mimeType)) ||
            UrlDetector.isAudio(url, mapOf("Content-Type" to mimeType)) ||
            mimeType.contains("mpegurl", ignoreCase = true) ||
            mimeType.contains("dash+xml", ignoreCase = true)

        if (isPlayableMedia) {
            val detectedVideo = DetectedVideo(
                url = url,
                headers = headers,
                pageUrl = sourcePageUrl,
                title = pageState.title.takeIf { it.isNotBlank() }.orEmpty(),
                timestamp = System.currentTimeMillis()
            )
            BrowserPlaybackInteractor.play(this, detectedVideo)
            return
        }

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType.ifBlank { null })
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            addRequestHeader("User-Agent", headers["User-Agent"].orEmpty())
            headers["Referer"]?.let { addRequestHeader("Referer", it) }
            headers["Origin"]?.let { addRequestHeader("Origin", it) }
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            setTitle(fileName)
            setDescription(
                if (contentLength > 0L) "下载中" else "准备下载"
            )
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        runCatching {
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(this, "已加入下载队列", Toast.LENGTH_SHORT).show()
        }.getOrElse {
            Toast.makeText(this, "下载启动失败，已尝试交给外部应用", Toast.LENGTH_SHORT).show()
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
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

        return runCatching {
            startActivity(parsedIntent)
            true
        }.recoverCatching {
            val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url").orEmpty()
            when {
                fallbackUrl.isNotBlank() -> {
                    browserWebViewController.loadUrl(fallbackUrl)
                    true
                }
                else -> {
                    Toast.makeText(this, "未找到可处理该链接的应用", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }.getOrDefault(false)
    }
}

