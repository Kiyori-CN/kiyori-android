package com.fam4k007.videoplayer.browser.web

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fam4k007.videoplayer.browser.data.BrowserHistoryRepository
import com.fam4k007.videoplayer.browser.data.BrowserPreferencesRepository
import com.fam4k007.videoplayer.browser.data.BrowserSearchHistoryRepository
import com.fam4k007.videoplayer.browser.domain.BrowserHistoryEntry
import com.fam4k007.videoplayer.browser.domain.BrowserPageState
import com.fam4k007.videoplayer.browser.domain.BrowserSearchRecord
import com.fam4k007.videoplayer.browser.domain.BrowserSearchEngine
import com.fam4k007.videoplayer.browser.security.BrowserSecurityPolicy
import com.fam4k007.videoplayer.sniffer.VideoSnifferManager

data class BrowserWebViewCallbacks(
    val onStateChanged: (BrowserPageState) -> Unit,
    val onExternalUrlBlocked: (String) -> Unit = {}
)

class BrowserWebViewController(
    private val preferencesRepository: BrowserPreferencesRepository,
    private val historyRepository: BrowserHistoryRepository,
    private val searchHistoryRepository: BrowserSearchHistoryRepository,
    private val callbacks: BrowserWebViewCallbacks
) {
    private var webView: WebView? = null

    private var pageState = BrowserPageState(
        inputUrl = preferencesRepository.getLastInputUrl(),
        currentUrl = BrowserSecurityPolicy.BLANK_HOME_URL,
        searchEngine = preferencesRepository.getSearchEngine(),
        isDesktopMode = preferencesRepository.isDesktopModeEnabled(),
        showUrlBar = false,
        historyEntries = historyRepository.getHistory().map { it.toEntry() },
        searchRecords = searchHistoryRepository.getSearchHistory().map { it.toRecord() }
    )

    init {
        callbacks.onStateChanged(pageState)
    }

    fun currentState(): BrowserPageState = pageState

    fun attach(webView: WebView) {
        if (this.webView === webView) {
            return
        }

        this.webView = webView
        BrowserWebViewFactory.configure(webView)
        applyUserAgent()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                updateState {
                    copy(
                        progress = newProgress.coerceIn(0, 100),
                        isLoading = newProgress in 1..99
                    )
                }
                syncNavigationState()
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                updateState {
                    copy(title = title?.takeIf { it.isNotBlank() } ?: "内置浏览器")
                }
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
                if (BrowserSecurityPolicy.shouldLoadInsideWebView(url)) {
                    applyUserAgent(url)
                    return false
                }
                updateState { copy(blockedExternalUrl = url) }
                callbacks.onExternalUrlBlocked(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val safeUrl = url ?: BrowserSecurityPolicy.BLANK_HOME_URL
                VideoSnifferManager.startNewPage()
                updateState {
                    copy(
                        currentUrl = safeUrl,
                        inputUrl = "",
                        isLoading = true,
                        progress = 0,
                        showUrlBar = false,
                        blockedExternalUrl = null
                    )
                }
                syncNavigationState()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val currentUrl = url ?: BrowserSecurityPolicy.BLANK_HOME_URL
                view?.let(::ensureZoomableViewport)
                updateState {
                    copy(
                        currentUrl = currentUrl,
                        inputUrl = "",
                        title = view?.title?.takeIf { it.isNotBlank() } ?: "内置浏览器",
                        isLoading = false,
                        progress = 100,
                        showUrlBar = false,
                        blockedExternalUrl = null
                    )
                }
                syncNavigationState()
                view?.let { scheduleHistoryCapture(it, currentUrl) }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let { req ->
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

        loadHome()
    }

    fun onResume() {
        webView?.onResume()
    }

    fun onPause() {
        webView?.onPause()
    }

    fun destroy() {
        webView?.apply {
            stopLoading()
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
        val rawInput = pageState.inputUrl.trim()
        val normalizedUrl = BrowserSecurityPolicy.normalizeUserInput(
            input = rawInput,
            searchEngine = pageState.searchEngine
        ) ?: return
        if (normalizedUrl != BrowserSecurityPolicy.BLANK_HOME_URL) {
            searchHistoryRepository.addSearchHistory(
                query = rawInput,
                targetUrl = normalizedUrl
            )
            syncSearchRecords()
        }
        preferencesRepository.setLastInputUrl("")
        updateState { copy(inputUrl = "") }
        loadUrl(normalizedUrl)
    }

    fun loadUrl(url: String) {
        val normalizedUrl = BrowserSecurityPolicy.normalizeUserInput(
            input = url,
            searchEngine = pageState.searchEngine
        ) ?: return
        updateState {
            copy(
                currentUrl = normalizedUrl,
                inputUrl = "",
                showUrlBar = false
            )
        }
        applyUserAgent(normalizedUrl)
        webView?.loadUrl(normalizedUrl)
    }

    fun selectSearchEngine(engine: BrowserSearchEngine) {
        preferencesRepository.setSearchEngine(engine)
        updateState { copy(searchEngine = engine) }
    }

    fun loadHome() {
        preferencesRepository.setLastInputUrl("")
        loadUrl(preferencesRepository.getHomeUrl())
        updateState { copy(showUrlBar = false, inputUrl = "") }
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
        if (webView?.canGoBack() == true) {
            applyUserAgent()
            webView?.goBack()
            syncNavigationState()
        }
    }

    fun goForward() {
        if (webView?.canGoForward() == true) {
            applyUserAgent()
            webView?.goForward()
            syncNavigationState()
        }
    }

    fun reload() {
        if (pageState.currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            loadHome()
            return
        }
        applyUserAgent()
        webView?.reload()
    }

    fun toggleDesktopMode() {
        val enabled = !pageState.isDesktopMode
        preferencesRepository.setDesktopModeEnabled(enabled)
        updateState { copy(isDesktopMode = enabled) }
        applyUserAgent()
        webView?.reload()
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

    private fun applyUserAgent(targetUrl: String = pageState.currentUrl) {
        val targetWebView = webView ?: return
        targetWebView.settings.userAgentString =
            BrowserSecurityPolicy.resolveUserAgent(targetUrl, pageState.isDesktopMode)
    }

    private fun ensureZoomableViewport(targetWebView: WebView) {
        if (pageState.currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            return
        }
        targetWebView.evaluateJavascript(
            """
            (function() {
              var content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
              var meta = document.querySelector('meta[name="viewport"]');
              if (!meta) {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                document.head && document.head.appendChild(meta);
              }
              if (meta) {
                meta.setAttribute('content', content);
              }
            })();
            """.trimIndent(),
            null
        )
    }

    private fun syncNavigationState() {
        val targetWebView = webView ?: return
        updateState {
            copy(
                canGoBack = targetWebView.canGoBack(),
                canGoForward = targetWebView.canGoForward(),
                currentUrl = targetWebView.url ?: currentUrl
            )
        }
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
        if (currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
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

    companion object {
        private const val PREVIEW_WIDTH = 432
        private const val PREVIEW_HEIGHT = 912
        private const val CAPTURE_DELAY_MS = 220L
    }
}
