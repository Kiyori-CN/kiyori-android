package com.android.kiyori.browser.web

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Message
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.kiyori.browser.data.BrowserHistoryRepository
import com.android.kiyori.browser.data.BrowserPreferencesRepository
import com.android.kiyori.browser.data.BrowserSearchHistoryRepository
import com.android.kiyori.browser.domain.BrowserHistoryEntry
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.domain.BrowserSearchRecord
import com.android.kiyori.browser.domain.BrowserUserAgentMode
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.browser.web.BrowserNetworkLogManager
import com.android.kiyori.browser.web.BrowserRequestBlocklistManager
import com.android.kiyori.sniffer.VideoSnifferManager
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.HttpAuthHandler
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.export.external.interfaces.JsPromptResult
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.CookieManager
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import org.json.JSONArray
import java.io.ByteArrayInputStream

data class BrowserWebViewCallbacks(
    val onStateChanged: (BrowserPageState) -> Unit,
    val onExternalUrlBlocked: (String) -> Boolean = { false },
    val onGeolocationPermissionRequest: (
        origin: String?,
        callback: GeolocationPermissionsCallback?
    ) -> Unit = { origin, callback ->
        callback?.invoke(origin, true, false)
    },
    val onDownloadRequested: (
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        sourcePageUrl: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    val onShowFileChooser: (
        fileChooserParams: WebChromeClient.FileChooserParams?,
        filePathCallback: ValueCallback<Array<android.net.Uri>>?
    ) -> Boolean = { _, callback ->
        callback?.onReceiveValue(null)
        false
    }
)

class BrowserWebViewController(
    private val preferencesRepository: BrowserPreferencesRepository,
    private val historyRepository: BrowserHistoryRepository,
    private val searchHistoryRepository: BrowserSearchHistoryRepository,
    private val callbacks: BrowserWebViewCallbacks,
    private val initialShowUrlBar: Boolean = false
) {
    private var webView: WebView? = null
    private var defaultUserAgent: String = ""
    private var customView: View? = null
    private var customViewCallback: IX5WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: FrameLayout? = null
    private var previousOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private val preferredHomeUrl: String
        get() = preferencesRepository.getHomeUrl()
    private val x5ClientExtension = BrowserX5ClientExtension(
        pageStateProvider = ::currentState,
        onMainFrameContentReady = ::onMainFrameContentReady
    )
    private var navigationGeneration = 0

    private var pageState = BrowserPageState(
        inputUrl = preferencesRepository.getLastInputUrl(),
        currentUrl = if (initialShowUrlBar) {
            BrowserSecurityPolicy.BLANK_HOME_URL
        } else {
            preferencesRepository.getHomeUrl()
        },
        searchEngine = preferencesRepository.getSearchEngine(),
        userAgentMode = preferencesRepository.getUserAgentMode(),
        isIncognitoMode = preferencesRepository.isIncognitoModeEnabled(),
        isDesktopMode = preferencesRepository.getUserAgentMode() == BrowserUserAgentMode.PC_DESKTOP,
        showUrlBar = initialShowUrlBar,
        historyEntries = historyRepository.getHistory().map { it.toEntry() },
        searchRecords = searchHistoryRepository.getSearchHistory().map { it.toRecord() }
    )

    init {
        callbacks.onStateChanged(pageState)
    }

    fun currentState(): BrowserPageState = pageState

    fun getCustomGlobalUserAgent(): String {
        return preferencesRepository.getCustomGlobalUserAgent()
    }

    fun getCustomSiteUserAgent(url: String = pageState.currentUrl): String {
        return preferencesRepository.getCustomSiteUserAgent(url)
    }

    fun getCurrentSiteHost(url: String = pageState.currentUrl): String {
        return preferencesRepository.getCurrentSiteHost(url)
    }

    fun saveCustomGlobalUserAgent(value: String) {
        preferencesRepository.setCustomGlobalUserAgent(value)
        if (pageState.userAgentMode == BrowserUserAgentMode.CUSTOM_GLOBAL) {
            refreshCurrentPageForUserAgent(pageState.currentUrl)
        }
    }

    fun saveCustomSiteUserAgent(value: String, url: String = pageState.currentUrl) {
        preferencesRepository.setCustomSiteUserAgent(url, value)
        if (pageState.userAgentMode == BrowserUserAgentMode.CUSTOM_SITE) {
            refreshCurrentPageForUserAgent(url)
        }
    }

    fun attach(webView: WebView) {
        if (this.webView === webView) {
            return
        }

        this.webView = webView
        BrowserWebViewFactory.configure(
            webView = webView,
            geolocationEnabled = preferencesRepository.isWebPageGeolocationEnabled()
        )
        webView.onResume()
        webView.resumeTimers()
        defaultUserAgent = webView.settings.userAgentString.orEmpty()
        Log.i(
            LOG_TAG,
            "WebView package=${WebView.getCurrentWebViewPackage()?.packageName} " +
                "version=${WebView.getCurrentWebViewPackage()?.versionName} " +
                "isX5Core=${webView.isX5Core}"
        )
        applyUserAgent()
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            callbacks.onDownloadRequested(
                url.orEmpty(),
                userAgent.orEmpty(),
                contentDisposition.orEmpty(),
                mimeType.orEmpty(),
                contentLength,
                pageState.currentUrl
            )
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val safeProgress = newProgress.coerceIn(0, 100)
                if (safeProgress >= 100 && view != null) {
                    completePageLoad(view = view, url = view.url, captureHistory = false)
                } else {
                    updateState {
                        copy(
                            progress = safeProgress,
                            isLoading = safeProgress in 1..99 || isLoading
                        )
                    }
                    syncNavigationState()
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                updateState {
                    copy(title = title?.takeIf { it.isNotBlank() } ?: "Kiyori")
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage ?: return false
                val messageLevel = consoleMessage.messageLevel()
                if (messageLevel == ConsoleMessage.MessageLevel.ERROR ||
                    messageLevel == ConsoleMessage.MessageLevel.WARNING
                ) {
                    Log.w(
                        LOG_TAG,
                        "JS ${messageLevel.name}: ${consoleMessage.message()} " +
                            "@${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                    )
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissionsCallback?
            ) {
                if (!preferencesRepository.isWebPageGeolocationEnabled()) {
                    callback?.invoke(origin, false, false)
                    return
                }
                callbacks.onGeolocationPermissionRequest(origin, callback)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val sourceView = view ?: return false
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val popupWebView = WebView(sourceView.context)
                BrowserWebViewFactory.configure(
                    webView = popupWebView,
                    geolocationEnabled = preferencesRepository.isWebPageGeolocationEnabled()
                )
                popupWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val popupUrl = request?.url?.toString().orEmpty()
                        if (popupUrl.isNotBlank()) {
                            loadUrl(popupUrl)
                        }
                        view?.destroy()
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrBlank() && url != BrowserSecurityPolicy.BLANK_HOME_URL) {
                            loadUrl(url)
                        }
                        view?.stopLoading()
                        view?.destroy()
                    }
                }
                transport.webView = popupWebView
                resultMsg.sendToTarget()
                return true
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                return showJavaScriptAlert(
                    view = view,
                    title = view?.title?.takeIf { it.isNotBlank() },
                    message = message.orEmpty(),
                    onConfirm = { result?.confirm() },
                    onCancel = { result?.cancel() }
                )
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                return showJavaScriptConfirm(
                    view = view,
                    title = view?.title?.takeIf { it.isNotBlank() },
                    message = message.orEmpty(),
                    onConfirm = { result?.confirm() },
                    onCancel = { result?.cancel() }
                )
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                return showJavaScriptPrompt(
                    view = view,
                    title = view?.title?.takeIf { it.isNotBlank() },
                    message = message.orEmpty(),
                    defaultValue = defaultValue.orEmpty(),
                    onConfirm = { value -> result?.confirm(value) },
                    onCancel = { result?.cancel() }
                )
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?
            ): Boolean {
                return callbacks.onShowFileChooser(fileChooserParams, filePathCallback)
            }

            override fun onShowCustomView(
                view: View?,
                callback: IX5WebChromeClient.CustomViewCallback?
            ) {
                showCustomView(view, callback)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString().orEmpty()
                if (url.isBlank()) {
                    return true
                }
                if (BrowserRequestBlocklistManager.isBlocked(url)) {
                    BrowserNetworkLogManager.addBlocked(
                        url = url,
                        pageUrl = pageState.currentUrl,
                        pageTitle = pageState.title,
                        headers = request?.requestHeaders.orEmpty()
                    )
                    updateState { copy(blockedExternalUrl = url) }
                    return true
                }
                if (BrowserSecurityPolicy.shouldLoadInsideWebView(url)) {
                    applyUserAgent(url)
                    return false
                }
                if (!BrowserSecurityPolicy.shouldAttemptExternalNavigation(url)) {
                    return true
                }
                val isMainFrameNavigation = request?.isForMainFrame != false
                if (!isMainFrameNavigation) {
                    return true
                }
                val fallbackUrl = resolveBrowserFallbackUrl(url)
                val hasUserGesture = request?.hasGesture() == true
                if (!hasUserGesture) {
                    if (!fallbackUrl.isNullOrBlank()) {
                        loadUrl(fallbackUrl)
                    } else {
                        Log.d(LOG_TAG, "Silently blocked external navigation without user gesture: $url")
                    }
                    return true
                }
                BrowserNetworkLogManager.addBlocked(
                    url = url,
                    pageUrl = pageState.currentUrl,
                    pageTitle = pageState.title
                )
                val handledExternally = callbacks.onExternalUrlBlocked(url)
                if (!handledExternally) {
                    updateState { copy(blockedExternalUrl = url) }
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val safeUrl = url ?: BrowserSecurityPolicy.BLANK_HOME_URL
                applyUserAgent(safeUrl)
                BrowserNetworkLogManager.startNewPage()
                VideoSnifferManager.startNewPage()
                beginNavigation(targetUrl = safeUrl)
                syncNavigationState()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view != null) {
                    enforceUniversalPinchZoom(view)
                    completePageLoad(view = view, url = url, captureHistory = true)
                    return
                }
                val currentUrl = url ?: BrowserSecurityPolicy.BLANK_HOME_URL
                updateState {
                    copy(
                        currentUrl = currentUrl,
                        inputUrl = "",
                        title = view?.title?.takeIf { it.isNotBlank() } ?: "Kiyori",
                        isLoading = false,
                        progress = 100,
                        showUrlBar = showUrlBar,
                        blockedExternalUrl = null
                    )
                }
                syncNavigationState()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    val failedUrl = request.url?.toString()
                    Log.w(
                        LOG_TAG,
                        "Page load error ${error?.errorCode}: ${error?.description} @$failedUrl"
                    )
                    if (view != null) {
                        finishLoadingState(
                            view = view,
                            url = failedUrl,
                            captureHistory = false,
                            reason = "main-frame-error"
                        )
                    }
                }
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    val failedUrl = request.url?.toString()
                    val statusCode = errorResponse?.statusCode ?: -1
                    Log.w(
                        LOG_TAG,
                        "HTTP $statusCode @$failedUrl"
                    )
                    if (view != null && statusCode >= 400 && statusCode != 401 && statusCode != 407) {
                        finishLoadingState(
                            view = view,
                            url = failedUrl,
                            captureHistory = false,
                            reason = "main-frame-http-$statusCode"
                        )
                    }
                }
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.w(LOG_TAG, "SSL error on ${view?.url.orEmpty()}, proceeding for compatibility")
                handler?.proceed()
            }

            override fun onReceivedHttpAuthRequest(
                view: WebView?,
                handler: HttpAuthHandler?,
                host: String?,
                realm: String?
            ) {
                if (view == null || handler == null) {
                    handler?.cancel()
                    return
                }

                val savedCredentials = if (handler.useHttpAuthUsernamePassword()) {
                    view.getHttpAuthUsernamePassword(host.orEmpty(), realm.orEmpty())
                } else {
                    null
                }

                if (savedCredentials != null && savedCredentials.size == 2 &&
                    !savedCredentials[0].isNullOrBlank() && !savedCredentials[1].isNullOrBlank()
                ) {
                    handler.proceed(savedCredentials[0], savedCredentials[1])
                    return
                }

                showHttpAuthDialog(
                    view = view,
                    handler = handler,
                    host = host.orEmpty(),
                    realm = realm.orEmpty()
                )
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: WebViewClient.RenderProcessGoneDetail?
            ): Boolean {
                Log.e(
                    LOG_TAG,
                    "WebView render process gone. didCrash=${detail?.didCrash()}"
                )
                view?.destroy()
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let { req ->
                    if (BrowserRequestBlocklistManager.isBlocked(req.url.toString())) {
                        BrowserNetworkLogManager.addBlocked(
                            url = req.url.toString(),
                            headers = req.requestHeaders,
                            pageUrl = pageState.currentUrl,
                            pageTitle = pageState.title
                        )
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            403,
                            "Blocked",
                            emptyMap(),
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                    BrowserNetworkLogManager.addResource(
                        url = req.url.toString(),
                        headers = req.requestHeaders,
                        pageUrl = pageState.currentUrl,
                        pageTitle = pageState.title
                    )
                    VideoSnifferManager.processRequest(
                        url = req.url.toString(),
                        headers = req.requestHeaders,
                        pageUrl = pageState.currentUrl,
                        pageTitle = pageState.title
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        runCatching {
            webView.x5WebViewExtension?.setWebViewClientExtension(x5ClientExtension)
            Log.i(LOG_TAG, "Installed X5 WebViewClientExtension")
        }.onFailure { error ->
            Log.w(LOG_TAG, "Failed to install X5 WebViewClientExtension", error)
        }

        if (initialShowUrlBar) {
            preferencesRepository.setLastInputUrl("")
            updateState {
                copy(
                    currentUrl = BrowserSecurityPolicy.BLANK_HOME_URL,
                    inputUrl = "",
                    isLoading = false,
                    progress = 0,
                    blockedExternalUrl = null
                )
            }
            syncNavigationState()
        }
    }

    fun onResume() {
        webView?.onResume()
    }

    fun onPause() {
        webView?.onPause()
    }

    fun exitCustomViewIfNeeded(): Boolean {
        if (customView == null) {
            return false
        }
        hideCustomView()
        return true
    }

    fun destroy() {
        hideCustomView()
        navigationGeneration += 1
        runCatching {
            webView?.x5WebViewExtension?.setWebViewClientExtension(null)
        }
        webView?.apply {
            runCatching { stopLoading() }
            webChromeClient = null
            webViewClient = null
            setDownloadListener(null)
            onPause()
            removeAllViews()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        webView = null
    }

    fun updateInputUrl(value: String) {
        preferencesRepository.setLastInputUrl(value)
        updateState { copy(inputUrl = value) }
    }

    fun setUrlBarVisible(visible: Boolean) {
        updateState { copy(showUrlBar = visible) }
    }

    fun submitInputUrl() {
        submitBrowserInput(
            rawInput = pageState.inputUrl.trim(),
            targetEngine = pageState.searchEngine
        )
    }

    fun loadUrl(url: String) {
        loadUrl(url = url, closeUrlBar = true)
    }

    private fun loadUrl(url: String, closeUrlBar: Boolean) {
        val targetWebView = webView
        val normalizedInputUrl = BrowserSecurityPolicy.normalizeUserInput(
            input = url,
            searchEngine = pageState.searchEngine
        ) ?: return
        val normalizedUrl = if (
            normalizedInputUrl == BrowserSecurityPolicy.BLANK_HOME_URL &&
            preferredHomeUrl != BrowserSecurityPolicy.BLANK_HOME_URL
        ) {
            preferredHomeUrl
        } else {
            normalizedInputUrl
        }
        if (normalizedUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            navigationGeneration += 1
            updateState {
                copy(
                    currentUrl = normalizedUrl,
                    inputUrl = "",
                    showUrlBar = if (closeUrlBar) false else showUrlBar,
                    isLoading = false,
                    progress = 0,
                    blockedExternalUrl = null
                )
            }
            cancelPendingLoadIfNeeded(targetWebView, normalizedUrl)
            loadIntoWebView(targetWebView, normalizedUrl)
            syncNavigationState()
            return
        }
        cancelPendingLoadIfNeeded(targetWebView, normalizedUrl)
        beginNavigation(
            targetUrl = normalizedUrl,
            closeUrlBar = closeUrlBar
        )
        updateState {
            copy(
                currentUrl = normalizedUrl,
                inputUrl = "",
                showUrlBar = if (closeUrlBar) false else showUrlBar
            )
        }
        applyUserAgent(normalizedUrl)
        loadIntoWebView(targetWebView, normalizedUrl)
    }

    fun selectSearchEngine(engine: BrowserSearchEngine) {
        preferencesRepository.setSearchEngine(engine)
        updateState { copy(searchEngine = engine) }
    }

    fun searchAgainWithEngine(engine: BrowserSearchEngine) {
        val query = pageState.lastSearchQuery.trim()
        if (query.isBlank()) {
            selectSearchEngine(engine)
            return
        }
        submitBrowserInput(
            rawInput = query,
            targetEngine = engine
        )
    }

    fun dismissSearchEngineQuickSwitchBar() {
        updateState {
            copy(showSearchEngineQuickSwitchBar = false)
        }
    }

    fun toggleIncognitoMode() {
        val enabled = !pageState.isIncognitoMode
        preferencesRepository.setIncognitoModeEnabled(enabled)
        updateState { copy(isIncognitoMode = enabled) }
    }

    fun loadHome() {
        loadHome(closeUrlBar = true)
    }

    private fun loadHome(closeUrlBar: Boolean) {
        preferencesRepository.setLastInputUrl("")
        loadUrl(
            url = preferredHomeUrl,
            closeUrlBar = closeUrlBar
        )
        updateState {
            copy(
                showUrlBar = if (closeUrlBar) false else showUrlBar,
                inputUrl = ""
            )
        }
    }

    fun clearSearchHistory() {
        searchHistoryRepository.clearSearchHistory()
        syncSearchRecords()
    }

    fun deleteSearchRecord(id: Long) {
        searchHistoryRepository.deleteSearchHistory(id)
        syncSearchRecords()
    }

    fun goBack() {
        val targetWebView = webView
        if (targetWebView?.canGoBack() == true) {
            val backUrl = peekNavigationUrl(targetWebView, offset = -1)
            applyUserAgent(backUrl)
            beginNavigation(targetUrl = backUrl, closeUrlBar = false)
            targetWebView.goBack()
            scheduleNavigationStateSync(targetWebView)
        }
    }

    fun goForward() {
        val targetWebView = webView
        if (targetWebView?.canGoForward() == true) {
            val forwardUrl = peekNavigationUrl(targetWebView, offset = 1)
            applyUserAgent(forwardUrl)
            beginNavigation(targetUrl = forwardUrl, closeUrlBar = false)
            targetWebView.goForward()
            scheduleNavigationStateSync(targetWebView)
        }
    }

    fun reload() {
        val targetWebView = webView ?: return
        if (pageState.currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            loadHome()
            return
        }
        applyUserAgent(pageState.currentUrl)
        beginNavigation(targetUrl = pageState.currentUrl)
        cancelPendingLoadIfNeeded(targetWebView, pageState.currentUrl)
        targetWebView.reload()
    }

    fun toggleDesktopMode() {
        val nextMode = if (pageState.userAgentMode == BrowserUserAgentMode.PC_DESKTOP) {
            BrowserUserAgentMode.ANDROID
        } else {
            BrowserUserAgentMode.PC_DESKTOP
        }
        setUserAgentMode(nextMode)
    }

    fun setUserAgentMode(mode: BrowserUserAgentMode) {
        preferencesRepository.setUserAgentMode(mode)
        updateState {
            copy(
                userAgentMode = mode,
                isDesktopMode = mode == BrowserUserAgentMode.PC_DESKTOP
            )
        }
        refreshCurrentPageForUserAgent(pageState.currentUrl)
    }

    fun clearCookiesForCurrentPage() {
        val currentUrl = pageState.currentUrl
        if (currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            return
        }
        CookieManager.getInstance().setCookie(currentUrl, "Max-Age=0")
        CookieManager.getInstance().flush()
    }

    fun deleteHistoryItem(id: Long) {
        historyRepository.deleteHistory(id)
        syncHistoryEntries()
    }

    fun clearHistory() {
        historyRepository.clearHistory()
        syncHistoryEntries()
    }

    fun requestPageSource(onResult: (String) -> Unit) {
        val targetWebView = webView
        if (targetWebView == null || pageState.currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            onResult("")
            return
        }

        targetWebView.evaluateJavascript(
            "(function(){return document.documentElement ? document.documentElement.outerHTML : '';})();"
        ) { rawResult ->
            onResult(decodeJavaScriptStringResult(rawResult))
        }
    }

    private fun resolveActiveUserAgent(targetUrl: String = pageState.currentUrl): String {
        return resolveActiveUserAgentProfile(targetUrl).userAgent
    }

    private fun resolveActiveUserAgentProfile(
        targetUrl: String = pageState.currentUrl
    ): BrowserSecurityPolicy.UserAgentProfile {
        val customUserAgent = when (pageState.userAgentMode) {
            BrowserUserAgentMode.CUSTOM_GLOBAL -> {
                preferencesRepository.getCustomGlobalUserAgent()
            }
            BrowserUserAgentMode.CUSTOM_SITE -> {
                preferencesRepository.getCustomSiteUserAgent(targetUrl)
                    .ifBlank { preferencesRepository.getCustomGlobalUserAgent() }
            }
            else -> ""
        }
        return BrowserSecurityPolicy.resolveUserAgentProfile(
            mode = pageState.userAgentMode,
            defaultUserAgent = defaultUserAgent,
            customUserAgent = customUserAgent
        )
    }

    private fun applyUserAgent(targetUrl: String = pageState.currentUrl) {
        val targetWebView = webView ?: return
        BrowserWebViewFactory.applyIdentity(
            webView = targetWebView,
            profile = resolveActiveUserAgentProfile(targetUrl)
        )
    }

    private fun submitBrowserInput(
        rawInput: String,
        targetEngine: BrowserSearchEngine
    ) {
        if (rawInput.isBlank()) {
            return
        }

        val normalizedUrl = BrowserSecurityPolicy.normalizeUserInput(
            input = rawInput,
            searchEngine = targetEngine
        ) ?: return
        val searchQuery = BrowserSecurityPolicy.resolveSearchQuery(
            input = rawInput,
            searchEngine = targetEngine
        ).orEmpty()

        preferencesRepository.setSearchEngine(targetEngine)
        if (!pageState.isIncognitoMode && normalizedUrl != BrowserSecurityPolicy.BLANK_HOME_URL) {
            searchHistoryRepository.addSearchHistory(
                query = rawInput,
                targetUrl = normalizedUrl
            )
            syncSearchRecords()
        }
        preferencesRepository.setLastInputUrl("")
        updateState {
            copy(
                inputUrl = "",
                searchEngine = targetEngine,
                lastSearchQuery = searchQuery,
                showSearchEngineQuickSwitchBar = searchQuery.isNotBlank()
            )
        }
        loadUrl(normalizedUrl)
    }

    private fun refreshCurrentPageForUserAgent(targetUrl: String = pageState.currentUrl) {
        val targetWebView = webView ?: return
        applyUserAgent(targetUrl)
        if (targetUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            syncNavigationState()
            return
        }
        beginNavigation(targetUrl = targetUrl)
        cancelPendingLoadIfNeeded(targetWebView, targetUrl)
        loadIntoWebView(
            targetWebView = targetWebView,
            normalizedUrl = targetUrl,
            bypassCache = true
        )
    }

    private fun peekNavigationUrl(
        targetWebView: WebView,
        offset: Int
    ): String {
        return runCatching {
            val historyList = targetWebView.copyBackForwardList()
            val targetIndex = historyList.currentIndex + offset
            if (targetIndex >= 0 && targetIndex < historyList.size) {
                historyList.getItemAtIndex(targetIndex)?.url.orEmpty()
            } else {
                ""
            }
        }.getOrDefault("")
            .ifBlank { pageState.currentUrl }
    }

    private fun showJavaScriptAlert(
        view: WebView?,
        title: String?,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ): Boolean {
        val context = view?.context ?: return false
        AlertDialog.Builder(context)
            .setTitle(title ?: "网页提示")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setOnCancelListener { onCancel() }
            .show()
        return true
    }

    private fun showJavaScriptConfirm(
        view: WebView?,
        title: String?,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ): Boolean {
        val context = view?.context ?: return false
        AlertDialog.Builder(context)
            .setTitle(title ?: "网页确认")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                onCancel()
                dialog.dismiss()
            }
            .setOnCancelListener { onCancel() }
            .show()
        return true
    }

    private fun showJavaScriptPrompt(
        view: WebView?,
        title: String?,
        message: String,
        defaultValue: String,
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit
    ): Boolean {
        val context = view?.context ?: return false
        val input = EditText(context).apply {
            setText(defaultValue)
            setSelection(text.length)
        }
        AlertDialog.Builder(context)
            .setTitle(title ?: "网页输入")
            .setMessage(message)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                onConfirm(input.text?.toString().orEmpty())
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                onCancel()
                dialog.dismiss()
            }
            .setOnCancelListener { onCancel() }
            .show()
        return true
    }

    private fun showHttpAuthDialog(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        val context = view.context
        val usernameInput = EditText(context).apply {
            hint = "用户名"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val passwordInput = EditText(context).apply {
            hint = "密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(
                usernameInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                passwordInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        AlertDialog.Builder(context)
            .setTitle("网页登录")
            .setMessage("$host${if (realm.isNotBlank()) "\n$realm" else ""}")
            .setView(container)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val username = usernameInput.text?.toString().orEmpty()
                val password = passwordInput.text?.toString().orEmpty()
                if (username.isNotBlank() && password.isNotBlank()) {
                    view.setHttpAuthUsernamePassword(host, realm, username, password)
                    handler.proceed(username, password)
                } else {
                    handler.cancel()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                handler.cancel()
                dialog.dismiss()
            }
            .setOnCancelListener { handler.cancel() }
            .show()
    }

    private fun showCustomView(
        view: View?,
        callback: IX5WebChromeClient.CustomViewCallback?
    ) {
        if (view == null || callback == null) {
            callback?.onCustomViewHidden()
            return
        }
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        val activity = webView?.context as? Activity ?: run {
            callback.onCustomViewHidden()
            return
        }
        val decor = activity.window.decorView as? FrameLayout ?: run {
            callback.onCustomViewHidden()
            return
        }

        previousOrientation = activity.requestedOrientation
        val container = FrameLayout(activity).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        decor.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        fullscreenContainer = container
        customView = view
        customViewCallback = callback
        webView?.visibility = View.INVISIBLE
        WindowInsetsControllerCompat(activity.window, decor).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun hideCustomView() {
        val targetCustomView = customView ?: return
        val targetWebView = webView
        val activity = targetWebView?.context as? Activity
        val decor = activity?.window?.decorView as? FrameLayout

        fullscreenContainer?.removeView(targetCustomView)
        fullscreenContainer?.let { container ->
            decor?.removeView(container)
            container.removeAllViews()
        }
        fullscreenContainer = null
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        targetWebView?.visibility = View.VISIBLE
        if (activity != null && decor != null) {
            WindowInsetsControllerCompat(activity.window, decor)
                .show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = previousOrientation
            activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun syncNavigationState() {
        val targetWebView = webView ?: return
        val webProgress = targetWebView.progress.coerceIn(0, 100)
        val resolvedUrl = targetWebView.url ?: pageState.currentUrl
        updateState {
            copy(
                canGoBack = targetWebView.canGoBack(),
                canGoForward = targetWebView.canGoForward(),
                currentUrl = resolvedUrl,
                progress = when {
                    isLoading && webProgress == 0 -> progress
                    webProgress > 0 -> webProgress
                    else -> progress
                },
                isLoading = when {
                    webProgress in 1..99 -> true
                    resolvedUrl == BrowserSecurityPolicy.BLANK_HOME_URL -> false
                    isLoading && webProgress >= 100 -> false
                    else -> isLoading
                }
            )
        }
    }

    private fun beginNavigation(
        targetUrl: String? = null,
        closeUrlBar: Boolean? = null
    ) {
        navigationGeneration += 1
        val generation = navigationGeneration
        updateState {
            copy(
                currentUrl = targetUrl ?: currentUrl,
                inputUrl = "",
                showUrlBar = closeUrlBar?.let { shouldClose ->
                    if (shouldClose) false else showUrlBar
                } ?: showUrlBar,
                isLoading = true,
                progress = 0,
                blockedExternalUrl = null
            )
        }
        scheduleLoadStateRecovery(generation)
    }

    private fun scheduleLoadStateRecovery(
        generation: Int,
        attempt: Int = 0
    ) {
        val targetWebView = webView ?: return
        targetWebView.postDelayed({
            if (generation != navigationGeneration || this.webView !== targetWebView) {
                return@postDelayed
            }

            val resolvedUrl = targetWebView.url ?: pageState.currentUrl
            val currentProgress = targetWebView.progress.coerceIn(0, 100)
            if (pageState.isLoading && currentProgress >= 100 &&
                resolvedUrl != BrowserSecurityPolicy.BLANK_HOME_URL
            ) {
                finishLoadingState(
                    view = targetWebView,
                    url = resolvedUrl,
                    captureHistory = false,
                    reason = "progress-recovery"
                )
                return@postDelayed
            }

            val hasVisibleContent = hasRenderableMainFrameContent(
                targetWebView = targetWebView,
                progress = currentProgress
            )
            if (resolvedUrl != BrowserSecurityPolicy.BLANK_HOME_URL &&
                attempt >= DOM_READY_PROBE_START_ATTEMPT
            ) {
                probeDocumentReadyState(
                    targetWebView = targetWebView,
                    generation = generation,
                    attempt = attempt,
                    resolvedUrl = resolvedUrl,
                    fallbackHasVisibleContent = hasVisibleContent
                )
                return@postDelayed
            }

            if (pageState.isLoading && attempt >= MAX_LOAD_RECOVERY_ATTEMPTS) {
                if (resolvedUrl != BrowserSecurityPolicy.BLANK_HOME_URL && hasVisibleContent) {
                    Log.w(
                        LOG_TAG,
                        "Forcing load completion after recovery timeout: url=$resolvedUrl progress=$currentProgress contentHeight=${targetWebView.contentHeight}"
                    )
                    finishLoadingState(
                        view = targetWebView,
                        url = resolvedUrl,
                        captureHistory = false,
                        reason = "recovery-timeout"
                    )
                }
                return@postDelayed
            }

            if (pageState.isLoading && attempt < MAX_LOAD_RECOVERY_ATTEMPTS) {
                scheduleLoadStateRecovery(generation, attempt + 1)
            }
        }, LOAD_RECOVERY_DELAY_MS)
    }

    private fun probeDocumentReadyState(
        targetWebView: WebView,
        generation: Int,
        attempt: Int,
        resolvedUrl: String,
        fallbackHasVisibleContent: Boolean
    ) {
        runCatching {
            targetWebView.evaluateJavascript(DOCUMENT_READY_STATE_SCRIPT) { rawResult ->
                if (generation != navigationGeneration ||
                    this.webView !== targetWebView ||
                    !pageState.isLoading
                ) {
                    return@evaluateJavascript
                }

                val readyState = decodeJavaScriptStringResult(rawResult).lowercase()
                val latestProgress = targetWebView.progress.coerceIn(0, 100)
                val latestHasVisibleContent = hasRenderableMainFrameContent(
                    targetWebView = targetWebView,
                    progress = latestProgress
                ) || fallbackHasVisibleContent
                val isDocumentReady = readyState == "interactive" || readyState == "complete"
                if (resolvedUrl != BrowserSecurityPolicy.BLANK_HOME_URL &&
                    isDocumentReady &&
                    latestHasVisibleContent
                ) {
                    finishLoadingState(
                        view = targetWebView,
                        url = resolvedUrl,
                        captureHistory = false,
                        reason = "dom-ready-$readyState"
                    )
                } else if (attempt < MAX_LOAD_RECOVERY_ATTEMPTS) {
                    scheduleLoadStateRecovery(generation, attempt + 1)
                }
            }
        }.onFailure {
            if (attempt < MAX_LOAD_RECOVERY_ATTEMPTS) {
                scheduleLoadStateRecovery(generation, attempt + 1)
            }
        }
    }

    private fun scheduleNavigationStateSync(
        targetWebView: WebView,
        attempt: Int = 0
    ) {
        targetWebView.postDelayed({
            if (this.webView !== targetWebView) {
                return@postDelayed
            }

            syncNavigationState()
            val webProgress = targetWebView.progress.coerceIn(0, 100)
            if (pageState.isLoading && webProgress >= 100) {
                finishLoadingState(
                    view = targetWebView,
                    url = targetWebView.url,
                    captureHistory = false,
                    reason = "navigation-sync"
                )
                return@postDelayed
            }

            if (attempt < NAVIGATION_STATE_SYNC_ATTEMPTS &&
                (targetWebView.progress == 0 || pageState.currentUrl != (targetWebView.url ?: pageState.currentUrl))
            ) {
                scheduleNavigationStateSync(targetWebView, attempt + 1)
            }
        }, NAVIGATION_STATE_SYNC_DELAY_MS)
    }

    private fun onMainFrameContentReady(urlHint: String?) {
        val targetWebView = webView ?: return
        targetWebView.post {
            if (!pageState.isLoading) {
                return@post
            }

            val resolvedUrl = targetWebView.url ?: urlHint ?: pageState.currentUrl
            val resolvedProgress = maxOf(targetWebView.progress, pageState.progress)
            if (resolvedUrl == BrowserSecurityPolicy.BLANK_HOME_URL ||
                resolvedProgress < VISUAL_READY_PROGRESS_THRESHOLD
            ) {
                return@post
            }

            finishLoadingState(
                view = targetWebView,
                url = resolvedUrl,
                captureHistory = false,
                reason = "x5-visual-ready"
            )
        }
    }

    private fun hasRenderableMainFrameContent(
        targetWebView: WebView,
        progress: Int = targetWebView.progress.coerceIn(0, 100)
    ): Boolean {
        val title = targetWebView.title.orEmpty()
        return progress >= VISUAL_READY_PROGRESS_THRESHOLD ||
            targetWebView.contentHeight > 0 ||
            (title.isNotBlank() && title != "Kiyori")
    }

    private fun loadIntoWebView(
        targetWebView: WebView?,
        normalizedUrl: String,
        bypassCache: Boolean = false
    ) {
        targetWebView ?: return
        if (normalizedUrl == BrowserSecurityPolicy.BLANK_HOME_URL ||
            normalizedUrl.startsWith("javascript:", ignoreCase = true) ||
            normalizedUrl.startsWith("data:", ignoreCase = true) ||
            normalizedUrl.startsWith("blob:", ignoreCase = true)
        ) {
            targetWebView.loadUrl(normalizedUrl)
        } else {
            targetWebView.loadUrl(
                normalizedUrl,
                buildCompatibilityRequestHeaders(
                    targetUrl = normalizedUrl,
                    bypassCache = bypassCache
                )
            )
        }
    }

    private fun buildCompatibilityRequestHeaders(
        targetUrl: String,
        bypassCache: Boolean
    ): Map<String, String> {
        return linkedMapOf<String, String>().apply {
            put("X-Requested-With", "")
            resolveActiveUserAgent(targetUrl)
                .takeIf { it.isNotBlank() }
                ?.let { put("User-Agent", it) }
            if (bypassCache) {
                put("Cache-Control", "no-cache")
                put("Pragma", "no-cache")
            }
        }
    }

    private fun cancelPendingLoadIfNeeded(
        targetWebView: WebView?,
        nextUrl: String
    ) {
        targetWebView ?: return
        val currentWebUrl = targetWebView.url.orEmpty()
        val currentProgress = targetWebView.progress.coerceIn(0, 100)
        val hasInFlightLoad =
            pageState.isLoading ||
                currentProgress in 1..99 ||
                (currentWebUrl.isNotBlank() &&
                    currentWebUrl != BrowserSecurityPolicy.BLANK_HOME_URL &&
                    currentWebUrl != nextUrl)
        if (!hasInFlightLoad) {
            return
        }
        runCatching {
            targetWebView.stopLoading()
        }.onFailure {
            Log.w(LOG_TAG, "Failed to stop pending load before navigating to $nextUrl", it)
        }
    }

    private fun completePageLoad(
        view: WebView,
        url: String?,
        captureHistory: Boolean
    ) {
        val currentUrl = url ?: view.url ?: BrowserSecurityPolicy.BLANK_HOME_URL
        navigationGeneration += 1
        updateState {
            copy(
                currentUrl = currentUrl,
                inputUrl = "",
                title = view.title?.takeIf { it.isNotBlank() } ?: title,
                isLoading = false,
                progress = if (currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) 0 else 100,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
                blockedExternalUrl = null
            )
        }
        if (captureHistory) {
            scheduleHistoryCapture(view, currentUrl)
        }
    }

    private fun finishLoadingState(
        view: WebView,
        url: String?,
        captureHistory: Boolean,
        reason: String
    ) {
        Log.d(
            LOG_TAG,
            "Finishing loading state: reason=$reason url=${url ?: view.url.orEmpty()} progress=${view.progress} contentHeight=${view.contentHeight}"
        )
        completePageLoad(
            view = view,
            url = url,
            captureHistory = captureHistory
        )
    }

    private fun syncHistoryEntries() {
        updateState {
            copy(
                historyEntries = historyRepository.getHistory().map { it.toEntry() }
            )
        }
    }

    private fun syncSearchRecords() {
        updateState {
            copy(
                searchRecords = searchHistoryRepository.getSearchHistory().map { it.toRecord() }
            )
        }
    }

    private fun scheduleHistoryCapture(targetWebView: WebView, currentUrl: String) {
        if (pageState.isIncognitoMode || currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            return
        }

        targetWebView.postDelayed({
            if (targetWebView.url != currentUrl) {
                return@postDelayed
            }

            val previewBitmap = capturePreviewBitmap(targetWebView)
            val title = targetWebView.title?.takeIf { it.isNotBlank() } ?: pageState.title
            Thread {
                historyRepository.upsertHistory(
                    title = title,
                    url = currentUrl,
                    previewBitmap = previewBitmap
                )
                previewBitmap?.recycle()
                targetWebView.post { syncHistoryEntries() }
            }.start()
        }, CAPTURE_DELAY_MS)
    }

    private fun capturePreviewBitmap(targetWebView: WebView): Bitmap? {
        val sourceWidth = targetWebView.width
        val sourceHeight = targetWebView.height
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null
        }

        return runCatching {
            val bitmap = Bitmap.createBitmap(
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
                Bitmap.Config.RGB_565
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val scale = minOf(
                PREVIEW_WIDTH.toFloat() / sourceWidth.toFloat(),
                PREVIEW_HEIGHT.toFloat() / sourceHeight.toFloat()
            )
            val dx = (PREVIEW_WIDTH - sourceWidth * scale) / 2f
            val dy = (PREVIEW_HEIGHT - sourceHeight * scale) / 2f
            canvas.translate(dx, dy)
            canvas.scale(scale, scale)
            targetWebView.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun BrowserHistoryRepository.HistoryItem.toEntry(): BrowserHistoryEntry {
        return BrowserHistoryEntry(
            id = id,
            title = title,
            url = url,
            previewPath = previewPath,
            createdAt = createdAt
        )
    }

    private fun BrowserSearchHistoryRepository.SearchRecordItem.toRecord(): BrowserSearchRecord {
        return BrowserSearchRecord(
            id = id,
            query = query,
            targetUrl = targetUrl,
            createdAt = createdAt
        )
    }

    private fun updateState(transform: BrowserPageState.() -> BrowserPageState) {
        pageState = pageState.transform()
        callbacks.onStateChanged(pageState)
    }

    private fun decodeJavaScriptStringResult(rawResult: String?): String {
        if (rawResult.isNullOrBlank() || rawResult == "null") {
            return ""
        }
        return runCatching {
            JSONArray("[$rawResult]").getString(0)
        }.getOrDefault(rawResult)
    }

    private fun enforceUniversalPinchZoom(targetWebView: WebView) {
        val currentUrl = targetWebView.url.orEmpty()
        if (currentUrl.isBlank() || !BrowserSecurityPolicy.shouldLoadInsideWebView(currentUrl)) {
            return
        }
        runCatching {
            targetWebView.evaluateJavascript(FORCE_UNIVERSAL_PINCH_ZOOM_SCRIPT, null)
        }.onFailure {
            Log.d(LOG_TAG, "Failed to inject universal pinch-zoom script for $currentUrl", it)
        }
    }

    private fun resolveBrowserFallbackUrl(url: String): String? {
        if (!url.startsWith("intent:", ignoreCase = true)) {
            return null
        }
        return runCatching {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                .getStringExtra("browser_fallback_url")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    companion object {
        private const val LOG_TAG = "KiyoriBrowser"
        private const val PREVIEW_WIDTH = 432
        private const val PREVIEW_HEIGHT = 912
        private const val CAPTURE_DELAY_MS = 220L
        private const val LOAD_RECOVERY_DELAY_MS = 240L
        private const val MAX_LOAD_RECOVERY_ATTEMPTS = 18
        private const val DOM_READY_PROBE_START_ATTEMPT = 2
        private const val NAVIGATION_STATE_SYNC_DELAY_MS = 120L
        private const val NAVIGATION_STATE_SYNC_ATTEMPTS = 8
        private const val VISUAL_READY_PROGRESS_THRESHOLD = 85
        private const val DOCUMENT_READY_STATE_SCRIPT =
            "(function(){return document && document.readyState ? document.readyState : '';})();"
        private const val FORCE_UNIVERSAL_PINCH_ZOOM_SCRIPT = """
            (function() {
              try {
                var viewportContent = 'width=device-width, initial-scale=1, minimum-scale=0.25, maximum-scale=10, user-scalable=yes, viewport-fit=cover';
                var styleId = 'kiyori-force-pinch-zoom-style';
                var observerKey = '__kiyoriForcePinchZoomObserverInstalled';
                var styleText = 'html,body{touch-action:auto !important;}';
                function ensureViewport() {
                  if (!document) return;
                  var head = document.head || document.getElementsByTagName('head')[0] || document.documentElement;
                  if (!head) return;
                  var viewport = document.querySelector('meta[name="viewport"]');
                  if (!viewport) {
                    viewport = document.createElement('meta');
                    viewport.setAttribute('name', 'viewport');
                    head.appendChild(viewport);
                  }
                  if (viewport.getAttribute('content') !== viewportContent) {
                    viewport.setAttribute('content', viewportContent);
                  }
                }
                function ensureStyle() {
                  if (!document) return;
                  var head = document.head || document.getElementsByTagName('head')[0] || document.documentElement;
                  if (!head) return;
                  var style = document.getElementById(styleId);
                  if (!style) {
                    style = document.createElement('style');
                    style.id = styleId;
                    style.type = 'text/css';
                    head.appendChild(style);
                  }
                  if (style.textContent !== styleText) {
                    style.textContent = styleText;
                  }
                }
                function apply() {
                  ensureViewport();
                  ensureStyle();
                }
                apply();
                if (!window[observerKey]) {
                  var observerTarget = document.head || document.documentElement || document;
                  var observer = new MutationObserver(function() {
                    apply();
                  });
                  observer.observe(observerTarget, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ['content', 'name']
                  });
                  window[observerKey] = true;
                }
              } catch (e) {}
            })();
        """
    }
}
