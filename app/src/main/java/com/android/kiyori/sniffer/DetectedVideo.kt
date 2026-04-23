package com.android.kiyori.sniffer

import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackRequest

/**
 * 视频 URL 检测结果
 */
data class DetectedVideo(
    val url: String,
    val title: String = "",
    val pageUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toRemotePlaybackRequest(): RemotePlaybackRequest {
        return RemotePlaybackRequest(
            url = url,
            title = title,
            sourcePageUrl = pageUrl,
            headers = RemotePlaybackHeaders.normalizeForBrowserPlayback(headers),
            source = RemotePlaybackRequest.Source.WEB_SNIFFER
        )
    }

    fun toFullUrlString(): String = url

    fun getDisplayText(): String {
        val format = UrlDetector.getDetectedResourceFormat(url, headers)
            .takeUnless { it == "UNKNOWN" }
            ?: "FILE"

        return if (title.isNotEmpty()) {
            "$title ($format)"
        } else {
            format
        }
    }
}

