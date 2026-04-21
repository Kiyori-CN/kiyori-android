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
        val format = when {
            url.contains(".m3u8", ignoreCase = true) -> "M3U8"
            url.contains(".mpd", ignoreCase = true) -> "DASH"
            url.contains(".mp4", ignoreCase = true) -> "MP4"
            url.contains(".flv", ignoreCase = true) -> "FLV"
            url.contains(".avi", ignoreCase = true) -> "AVI"
            url.contains(".mkv", ignoreCase = true) -> "MKV"
            url.contains("rtmp://", ignoreCase = true) -> "RTMP"
            url.contains("rtmps://", ignoreCase = true) -> "RTMPS"
            url.contains("rtsp://", ignoreCase = true) -> "RTSP"
            else -> "VIDEO"
        }

        return if (title.isNotEmpty()) {
            "$title ($format)"
        } else {
            format
        }
    }
}

