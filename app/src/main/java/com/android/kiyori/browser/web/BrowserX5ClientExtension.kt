package com.android.kiyori.browser.web

import android.util.Log
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.web.BrowserNetworkLogManager
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.sniffer.DetectedVideo
import com.android.kiyori.sniffer.UrlDetector
import com.android.kiyori.sniffer.VideoSnifferManager
import com.tencent.smtt.export.external.extension.proxy.ProxyWebViewClientExtension
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse

class BrowserX5ClientExtension(
    private val pageStateProvider: () -> BrowserPageState,
    private val onMainFrameContentReady: (String?) -> Unit = {}
) : ProxyWebViewClientExtension() {

    override fun onResponseReceived(
        request: WebResourceRequest?,
        response: WebResourceResponse?,
        loadType: Int
    ) {
        val url = request?.url?.toString().orEmpty()
        if (url.isBlank()) {
            return
        }

        val requestHeaders = request?.requestHeaders.orEmpty()
        val responseHeaders = response?.responseHeaders.orEmpty()
        observeResource(
            url = url,
            requestHeaders = requestHeaders,
            responseHeaders = responseHeaders,
            mimeType = response?.mimeType,
            statusCode = response?.statusCode,
            source = "x5:onResponseReceived"
        )
    }

    override fun onReportResponseHeaders(
        url: String?,
        statusCode: Int,
        headers: HashMap<String, String>?
    ) {
        val safeUrl = url.orEmpty()
        if (safeUrl.isBlank()) {
            return
        }

        observeResource(
            url = safeUrl,
            responseHeaders = headers.orEmpty(),
            statusCode = statusCode,
            source = "x5:onReportResponseHeaders"
        )
    }

    override fun onReportMemoryCachedResponse(
        url: String?,
        statusCode: Int,
        headers: HashMap<String, String>?
    ) {
        val safeUrl = url.orEmpty()
        if (safeUrl.isBlank()) {
            return
        }

        observeResource(
            url = safeUrl,
            responseHeaders = headers.orEmpty(),
            statusCode = statusCode,
            source = "x5:onReportMemoryCachedResponse"
        )
    }

    override fun documentAvailableInMainFrame() {
        val currentUrl = pageStateProvider().currentUrl
        Log.d(LOG_TAG, "X5 main frame document available @$currentUrl")
        onMainFrameContentReady(currentUrl)
    }

    override fun didFirstVisuallyNonEmptyPaint() {
        val currentUrl = pageStateProvider().currentUrl
        Log.d(LOG_TAG, "X5 first visually non-empty paint @$currentUrl")
        onMainFrameContentReady(currentUrl)
    }

    override fun onReceivedSslErrorCancel() {
        Log.w(LOG_TAG, "X5 reported SSL error cancellation @${pageStateProvider().currentUrl}")
    }

    private fun observeResource(
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        responseHeaders: Map<String, String> = emptyMap(),
        mimeType: String? = null,
        statusCode: Int? = null,
        source: String
    ) {
        if (url.isBlank()) {
            return
        }

        val mergedHeaders = LinkedHashMap<String, String>()
        requestHeaders.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                mergedHeaders[key] = value
            }
        }
        responseHeaders.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                mergedHeaders[key] = value
            }
        }
        mimeType?.takeIf { it.isNotBlank() }?.let {
            mergedHeaders.putIfAbsent("Content-Type", it)
        }
        statusCode?.takeIf { it > 0 }?.let {
            mergedHeaders["X-Kiyori-Status-Code"] = it.toString()
        }

        val pageState = pageStateProvider()
        val normalizedHeaders = RemotePlaybackHeaders.normalizeForBrowserPlayback(mergedHeaders)
        BrowserNetworkLogManager.addResource(
            url = url,
            headers = mergedHeaders,
            pageUrl = pageState.currentUrl,
            pageTitle = pageState.title
        )
        if (UrlDetector.isVideo(url, mergedHeaders)) {
            VideoSnifferManager.addVideo(
                DetectedVideo(
                    url = url,
                    title = pageState.title,
                    pageUrl = pageState.currentUrl,
                    headers = normalizedHeaders
                )
            )
        }
        Log.d(
            LOG_TAG,
            "Detected media via $source: $url status=${statusCode ?: -1} headers=${normalizedHeaders.keys}"
        )
    }

    companion object {
        private const val LOG_TAG = "KiyoriBrowserX5"
    }
}
