package com.android.kiyori.browser.playback

import android.content.Context
import android.net.Uri
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.sniffer.DetectedVideo
import com.android.kiyori.sniffer.UrlDetector

object BrowserPlaybackInteractor {

    enum class BrowserVideoFilter {
        RECOMMENDED,
        MANIFEST,
        MP4,
        ALL
    }

    data class BrowserVideoCandidate(
        val video: DetectedVideo,
        val format: String,
        val score: Int,
        val summary: String,
        val isRecommended: Boolean,
        val canPreview: Boolean
    )

    private val noiseKeywords = listOf(
        "cover", "poster", "thumb", "thumbnail", "sprite", "preview", "sample",
        "subtitle", "subtitles", ".vtt", ".srt", ".ass", "danmaku",
        "advert", "ads", "doubleclick", "tracker", "analytics", "beacon",
        "captcha", "logo", "banner", ".jpg", ".jpeg", ".png", ".webp"
    )

    private val segmentKeywords = listOf(
        ".ts", ".m4s", "init.mp4", "segment", "segments", "chunk", "frag", "fragment"
    )

    private val streamingFormats = setOf("M3U8", "DASH", "RTMP", "RTSP")
    private val videoFormats = setOf(
        "MP4", "FLV", "WEBM", "MKV", "MOV", "AVI", "3GP", "ASF", "WMV",
        "RMVB", "RM", "M4V", "MPEG", "MPG", "MPE", "OGV", "QT", "VIDEO"
    )
    private val audioFormats = setOf(
        "AAC", "AIF", "M4A", "MP3", "MPA", "OGG", "RA", "WAV", "WMA", "AUDIO"
    )
    private val archiveFormats = setOf(
        "7Z", "ACE", "ARJ", "BZ2", "GZIP", "GZ", "LZH", "RAR", "SEA",
        "SIT", "SITX", "TAR", "Z", "ZIP", "R0", "R1"
    )
    private val packageFormats = setOf("APK", "BIN", "EXE", "IMG", "ISO", "MSI", "MSU")
    private val documentFormats = setOf("PDF", "PLJ", "PPS", "PPT", "TIF", "TIFF")

    fun play(context: Context, video: DetectedVideo) {
        val request = video.toRemotePlaybackRequest().copy(
            title = video.title.ifEmpty { "在线视频" }
        )
        RemotePlaybackLauncher.start(context, request)
    }

    fun buildCandidates(
        videos: List<DetectedVideo>,
        currentPageUrl: String = ""
    ): List<BrowserVideoCandidate> {
        if (videos.isEmpty()) {
            return emptyList()
        }

        val pageHost = extractHost(currentPageUrl)
        val candidatesByKey = LinkedHashMap<String, BrowserVideoCandidate>()
        for (video in videos) {
            val candidate = buildCandidate(video, pageHost)
            val similarityKey = buildSimilarityKey(video.url)
            val existing = candidatesByKey[similarityKey]
            if (existing == null || candidate.score > existing.score) {
                candidatesByKey[similarityKey] = candidate
            }
        }

        return candidatesByKey.values
            .sortedWith(
                compareByDescending<BrowserVideoCandidate> { it.isRecommended }
                    .thenByDescending { it.score }
                    .thenByDescending { it.video.timestamp }
                    .thenByDescending { it.video.url.length }
            )
            .take(40)
    }

    fun filterCandidates(
        candidates: List<BrowserVideoCandidate>,
        filter: BrowserVideoFilter
    ): List<BrowserVideoCandidate> {
        val filtered = when (filter) {
            BrowserVideoFilter.RECOMMENDED -> candidates.filter { it.isRecommended }
            BrowserVideoFilter.MANIFEST -> candidates.filter {
                it.format == "M3U8" || it.format == "DASH"
            }
            BrowserVideoFilter.MP4 -> candidates.filter { it.format == "MP4" }
            BrowserVideoFilter.ALL -> candidates
        }
        return if (filter == BrowserVideoFilter.RECOMMENDED && filtered.isEmpty()) {
            candidates.take(20)
        } else {
            filtered
        }
    }

    fun selectBestVideo(videos: List<DetectedVideo>, currentPageUrl: String = ""): DetectedVideo? {
        val candidates = buildCandidates(videos, currentPageUrl)
        return filterCandidates(candidates, BrowserVideoFilter.RECOMMENDED)
            .firstOrNull()
            ?.video
            ?: candidates.firstOrNull()?.video
    }

    private fun buildCandidate(video: DetectedVideo, pageHost: String): BrowserVideoCandidate {
        val url = video.url
        val lowerUrl = url.lowercase()
        val format = UrlDetector.getDetectedResourceFormat(url, video.headers)
        val contentType = RemotePlaybackHeaders.get(video.headers, "Content-Type")
            .orEmpty()
            .lowercase()
        val contentDisposition = RemotePlaybackHeaders.get(video.headers, "Content-Disposition")
            .orEmpty()
            .lowercase()
        val requestHost = extractHost(url)
        val canPreview = UrlDetector.isPlayableFormat(format)

        val reasons = mutableListOf<String>()
        var score = 0

        when (format) {
            in streamingFormats -> {
                score += 120
                reasons += "流媒体清单"
            }

            in videoFormats -> {
                score += 90
                reasons += "直链媒体"
            }

            in audioFormats -> {
                score += 78
                reasons += "音频资源"
            }

            in archiveFormats -> {
                score += 70
                reasons += "压缩包"
            }

            in packageFormats -> {
                score += 66
                reasons += "安装包/镜像"
            }

            in documentFormats -> {
                score += 62
                reasons += "文档资源"
            }

            else -> {
                if (format != "UNKNOWN") {
                    score += 54
                    reasons += "可下载文件"
                } else if (contentType.contains("video/")) {
                    score += 55
                    reasons += "服务端标记为视频"
                } else if (contentType.contains("audio/")) {
                    score += 48
                    reasons += "服务端标记为音频"
                }
            }
        }

        if (contentType.contains("mpegurl") || contentType.contains("dash+xml")) {
            score += 70
            reasons += "播放清单类型"
        }

        if (contentDisposition.contains("attachment")) {
            score += 28
            reasons += "下载响应"
        }

        if (pageHost.isNotBlank() && requestHost.isNotBlank() && isSameSite(pageHost, requestHost)) {
            score += 12
            reasons += "同站资源"
        }

        if (RemotePlaybackHeaders.normalize(video.headers).isNotEmpty()) {
            score += 8
            reasons += "已携带请求头"
        }

        val qualityScore = calculateQualityScore(lowerUrl)
        if (canPreview && qualityScore > 0) {
            score += qualityScore
            reasons += "质量关键词"
        }

        val hasSegmentPattern = canPreview && segmentKeywords.any { lowerUrl.contains(it) }
        if (hasSegmentPattern) {
            score -= 130
            reasons += "疑似分片"
        }

        val hasNoisePattern = noiseKeywords.any { lowerUrl.contains(it) }
        if (hasNoisePattern) {
            score -= 110
            reasons += "疑似封面/广告/字幕"
        }

        if (format == "UNKNOWN" && contentType.isBlank()) {
            score -= 15
        }

        val recommended = canPreview &&
            !hasSegmentPattern &&
            !hasNoisePattern &&
            score >= 60 &&
            (format != "UNKNOWN" || contentType.contains("video/") || contentType.contains("audio/"))

        return BrowserVideoCandidate(
            video = video,
            format = format,
            score = score,
            summary = reasons.distinct().joinToString(" / ").ifBlank { "普通候选资源" },
            isRecommended = recommended,
            canPreview = canPreview
        )
    }

    private fun buildSimilarityKey(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        val filteredQuery = uri.query
            ?.split("&")
            ?.filterNot { query ->
                val lowerQuery = query.lowercase()
                lowerQuery.startsWith("range=") ||
                    lowerQuery.startsWith("start=") ||
                    lowerQuery.startsWith("end=") ||
                    lowerQuery.startsWith("bytes=")
            }
            ?.sorted()
            ?.joinToString("&")
            .orEmpty()
        val base = buildString {
            append(uri.scheme.orEmpty().lowercase())
            append("://")
            append(uri.host.orEmpty().lowercase())
            append(uri.path.orEmpty())
        }
        return if (filteredQuery.isBlank()) base else "$base?$filteredQuery"
    }

    private fun extractHost(url: String): String {
        return runCatching { Uri.parse(url).host.orEmpty().lowercase() }.getOrDefault("")
    }

    private fun isSameSite(pageHost: String, requestHost: String): Boolean {
        return pageHost == requestHost ||
            pageHost.endsWith(".$requestHost") ||
            requestHost.endsWith(".$pageHost")
    }

    private fun calculateQualityScore(lowerUrl: String): Int {
        val highQualityKeywords = listOf(
            "1080p", "1080", "4k", "2160p", "2160", "uhd",
            "hd", "high", "best", "master", "premium", "vip"
        )
        val mediumQualityKeywords = listOf("720p", "720", "fhd", "fullhd")
        val lowQualityKeywords = listOf("360p", "360", "480p", "480", "sd", "low", "mobile")

        var score = 0
        highQualityKeywords.forEach { keyword ->
            if (lowerUrl.contains(keyword)) {
                score += 10
            }
        }
        mediumQualityKeywords.forEach { keyword ->
            if (lowerUrl.contains(keyword)) {
                score += 5
            }
        }
        lowQualityKeywords.forEach { keyword ->
            if (lowerUrl.contains(keyword)) {
                score -= 5
            }
        }

        if (
            lowerUrl.contains("php?") ||
            lowerUrl.contains("?url=") ||
            lowerUrl.contains("redirect")
        ) {
            score -= 10
        }

        val resolution = Regex("(\\d{3,4})p").find(lowerUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (resolution != null) {
            score += when {
                resolution >= 1080 -> 10
                resolution >= 720 -> 7
                resolution >= 480 -> 4
                resolution >= 360 -> 3
                else -> 1
            }
        }

        return score
    }
}
