package com.android.kiyori.sniffer

import android.net.Uri
import android.util.Log

object UrlDetector {
    private const val TAG = "UrlDetector"

    enum class NetworkCategory {
        VIDEO,
        AUDIO,
        IMAGE,
        WEB,
        OTHER,
        BLOCKED
    }

    private val formatExtensions = linkedMapOf(
        "M3U8" to listOf(".m3u8"),
        "DASH" to listOf(".mpd"),
        "MP4" to listOf(".mp4"),
        "FLV" to listOf(".flv"),
        "WEBM" to listOf(".webm"),
        "MKV" to listOf(".mkv"),
        "MOV" to listOf(".mov"),
        "AVI" to listOf(".avi"),
        "3GP" to listOf(".3gp"),
        "ASF" to listOf(".asf"),
        "WMV" to listOf(".wmv"),
        "RMVB" to listOf(".rmvb"),
        "RM" to listOf(".rm"),
        "M4V" to listOf(".m4v"),
        "MPEG" to listOf(".mpeg"),
        "MPG" to listOf(".mpg"),
        "MPE" to listOf(".mpe"),
        "OGV" to listOf(".ogv"),
        "QT" to listOf(".qt"),
        "AAC" to listOf(".aac"),
        "AIF" to listOf(".aif", ".aiff"),
        "M4A" to listOf(".m4a"),
        "MP3" to listOf(".mp3"),
        "MPA" to listOf(".mpa"),
        "OGG" to listOf(".ogg"),
        "RA" to listOf(".ra"),
        "WAV" to listOf(".wav"),
        "WMA" to listOf(".wma"),
        "7Z" to listOf(".7z"),
        "ACE" to listOf(".ace"),
        "APK" to listOf(".apk"),
        "ARJ" to listOf(".arj"),
        "BIN" to listOf(".bin"),
        "BZ2" to listOf(".bz2"),
        "EXE" to listOf(".exe"),
        "GZIP" to listOf(".gzip"),
        "GZ" to listOf(".gz"),
        "IMG" to listOf(".img"),
        "ISO" to listOf(".iso"),
        "LZH" to listOf(".lzh"),
        "MSI" to listOf(".msi"),
        "MSU" to listOf(".msu"),
        "PDF" to listOf(".pdf"),
        "PLJ" to listOf(".plj"),
        "PPS" to listOf(".pps"),
        "PPT" to listOf(".ppt", ".pptx"),
        "RAR" to listOf(".rar"),
        "SEA" to listOf(".sea"),
        "SIT" to listOf(".sit"),
        "SITX" to listOf(".sitx"),
        "TAR" to listOf(".tar"),
        "TIF" to listOf(".tif"),
        "TIFF" to listOf(".tiff"),
        "Z" to listOf(".z"),
        "ZIP" to listOf(".zip")
    )

    private val formatMimeTypes = linkedMapOf(
        "M3U8" to listOf("application/vnd.apple.mpegurl", "application/x-mpegurl"),
        "DASH" to listOf("application/dash+xml"),
        "MP4" to listOf("video/mp4"),
        "FLV" to listOf("video/x-flv"),
        "WEBM" to listOf("video/webm"),
        "MKV" to listOf("video/x-matroska", "video/mkv"),
        "MOV" to listOf("video/quicktime"),
        "AVI" to listOf("video/x-msvideo"),
        "3GP" to listOf("video/3gpp", "audio/3gpp"),
        "ASF" to listOf("video/x-ms-asf"),
        "WMV" to listOf("video/x-ms-wmv"),
        "RMVB" to listOf("application/vnd.rn-realmedia-vbr"),
        "RM" to listOf("application/vnd.rn-realmedia"),
        "M4V" to listOf("video/x-m4v"),
        "MPEG" to listOf("video/mpeg"),
        "MPG" to listOf("video/mpeg"),
        "MPE" to listOf("video/mpeg"),
        "OGV" to listOf("video/ogg"),
        "QT" to listOf("video/quicktime"),
        "AAC" to listOf("audio/aac", "audio/x-aac"),
        "AIF" to listOf("audio/aiff", "audio/x-aiff"),
        "M4A" to listOf("audio/mp4", "audio/x-m4a"),
        "MP3" to listOf("audio/mpeg", "audio/mp3"),
        "MPA" to listOf("audio/mpeg"),
        "OGG" to listOf("audio/ogg", "application/ogg"),
        "RA" to listOf("audio/x-pn-realaudio"),
        "WAV" to listOf("audio/wav", "audio/x-wav"),
        "WMA" to listOf("audio/x-ms-wma"),
        "7Z" to listOf("application/x-7z-compressed"),
        "ACE" to listOf("application/x-ace-compressed"),
        "APK" to listOf("application/vnd.android.package-archive"),
        "ARJ" to listOf("application/x-arj"),
        "BIN" to emptyList(),
        "BZ2" to listOf("application/x-bzip2"),
        "EXE" to listOf("application/vnd.microsoft.portable-executable", "application/x-msdownload"),
        "GZIP" to listOf("application/gzip"),
        "GZ" to listOf("application/x-gzip"),
        "IMG" to listOf("application/x-raw-disk-image"),
        "ISO" to listOf("application/x-iso9660-image"),
        "LZH" to listOf("application/x-lzh-compressed"),
        "MSI" to listOf("application/x-msi"),
        "MSU" to emptyList(),
        "PDF" to listOf("application/pdf"),
        "PLJ" to emptyList(),
        "PPS" to listOf("application/vnd.ms-powerpoint"),
        "PPT" to listOf(
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        ),
        "RAR" to listOf("application/vnd.rar", "application/x-rar-compressed"),
        "SEA" to listOf("application/x-sea"),
        "SIT" to listOf("application/x-stuffit"),
        "SITX" to listOf("application/x-stuffitx"),
        "TAR" to listOf("application/x-tar"),
        "TIF" to listOf("image/tiff"),
        "TIFF" to listOf("image/tiff"),
        "Z" to listOf("application/x-compress"),
        "ZIP" to listOf("application/zip", "application/x-zip-compressed")
    )

    private val videoFormats = setOf(
        "M3U8", "DASH", "MP4", "FLV", "WEBM", "MKV", "MOV", "AVI",
        "3GP", "ASF", "WMV", "RMVB", "RM", "M4V", "MPEG", "MPG", "MPE",
        "OGV", "QT", "RTMP", "RTSP", "VIDEO"
    )

    private val audioFormats = setOf(
        "AAC", "AIF", "M4A", "MP3", "MPA", "OGG", "RA", "WAV", "WMA", "AUDIO"
    )

    private val nonDownloadExtensions = listOf(
        ".css", ".js", ".mjs", ".html", ".htm", ".json", ".xml", ".map",
        ".txt", ".ico", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg",
        ".bmp", ".avif", ".woff", ".woff2", ".ttf", ".otf", ".eot", ".md"
    )

    private val imageExtensions = listOf(
        ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".bmp",
        ".avif", ".ico", ".tif", ".tiff"
    )

    private val webExtensions = listOf(
        ".html", ".htm", ".css", ".js", ".mjs", ".json", ".xml", ".txt",
        ".php", ".asp", ".aspx", ".jsp", ".jspx", ".woff", ".woff2",
        ".ttf", ".otf", ".eot"
    )

    private val videoPathKeywords = listOf(
        "/video/", "/play/", "/stream/", "/media/", "/vod/", "/movie/",
        "/tv/", "/anime/", "/film/", "/clip/", "/episode/"
    )

    private val videoParamKeywords = listOf("video", "url", "src", "source", "stream")

    private val excludePatterns = listOf(
        ".mp4.jpg", ".mp4.png", ".mp4.webp",
        ".m3u8.jpg", ".m3u8.png", ".m3u8.webp",
        ".php?url=http", "/?url=http"
    )

    fun getFormatSortOrder(format: String): Int {
        return preferredFormats.indexOf(format.uppercase()).let { index ->
            if (index >= 0) index else Int.MAX_VALUE
        }
    }

    fun isDownloadCandidate(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        if (url.isBlank() || url.contains("ignoreVideo=true")) {
            return false
        }
        if (hasFailedStatus(headers)) {
            return false
        }

        val lowerUrl = url.lowercase()
        if (
            lowerUrl.startsWith("blob:") ||
            lowerUrl.startsWith("data:") ||
            lowerUrl.startsWith("javascript:")
        ) {
            return false
        }

        if (excludePatterns.any { lowerUrl.contains(it) }) {
            return false
        }

        if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://") || lowerUrl.startsWith("rtsp://")) {
            return true
        }

        if (url.contains("isVideo=true", ignoreCase = true)) {
            return true
        }

        val pathUrl = getNeedCheckUrl(url).substringBefore("?").lowercase()
        if (nonDownloadExtensions.any { pathUrl.contains(it) }) {
            return false
        }

        if (getDetectedResourceFormat(url, headers) != "UNKNOWN") {
            return true
        }

        val contentType = getHeader(headers, "Content-Type").orEmpty().substringBefore(";").trim().lowercase()
        if (
            contentType.startsWith("video/") ||
            contentType.startsWith("audio/") ||
            contentType == "application/vnd.apple.mpegurl" ||
            contentType == "application/x-mpegurl" ||
            contentType == "application/dash+xml"
        ) {
            return true
        }

        if (hasAttachmentDisposition(headers)) {
            return true
        }

        if (videoPathKeywords.any { keyword -> pathUrl.contains(keyword) }) {
            return true
        }

        val queryParams = url.substringAfter("?", "")
        if (queryParams.isNotBlank() && videoParamKeywords.any { keyword ->
                queryParams.contains("$keyword=", ignoreCase = true)
            }
        ) {
            return true
        }

        return false
    }

    fun isVideo(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        val format = getDetectedResourceFormat(url, headers)
        if (format in videoFormats) {
            return true
        }
        val contentType = getHeader(headers, "Content-Type").orEmpty().lowercase()
        return contentType.startsWith("video/") ||
            contentType.contains("mpegurl") ||
            contentType.contains("dash+xml")
    }

    fun isAudio(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        val format = getDetectedResourceFormat(url, headers)
        if (format in audioFormats) {
            return true
        }
        val contentType = getHeader(headers, "Content-Type").orEmpty().lowercase()
        return contentType.startsWith("audio/")
    }

    fun isPlayableFormat(format: String): Boolean {
        val normalizedFormat = format.uppercase()
        return normalizedFormat in videoFormats || normalizedFormat in audioFormats
    }

    fun isImage(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        val contentType = getHeader(headers, "Content-Type").orEmpty().lowercase()
        if (contentType.startsWith("image/")) {
            return true
        }

        val lowerUrl = getNeedCheckUrl(url).substringBefore("?").lowercase()
        return imageExtensions.any { lowerUrl.contains(it) }
    }

    fun isWebResource(url: String, headers: Map<String, String> = emptyMap()): Boolean {
        val contentType = getHeader(headers, "Content-Type").orEmpty().substringBefore(";").trim().lowercase()
        if (
            contentType.startsWith("text/") ||
            contentType.contains("javascript") ||
            contentType.contains("json") ||
            contentType.contains("xml") ||
            contentType.contains("font/") ||
            contentType == "application/xhtml+xml"
        ) {
            return true
        }

        val lowerUrl = getNeedCheckUrl(url).substringBefore("?").lowercase()
        return webExtensions.any { lowerUrl.contains(it) }
    }

    fun classifyNetworkResource(url: String, headers: Map<String, String> = emptyMap()): NetworkCategory {
        return when {
            isVideo(url, headers) -> NetworkCategory.VIDEO
            isAudio(url, headers) -> NetworkCategory.AUDIO
            isImage(url, headers) -> NetworkCategory.IMAGE
            isWebResource(url, headers) -> NetworkCategory.WEB
            else -> NetworkCategory.OTHER
        }
    }

    fun getNeedCheckUrl(url: String): String {
        return url.substringBefore("#")
    }

    fun getVideoFormat(url: String): String {
        return getDetectedResourceFormat(url).takeIf { it in videoFormats } ?: "UNKNOWN"
    }

    fun getDetectedResourceFormat(url: String, headers: Map<String, String> = emptyMap()): String {
        if (url.isBlank()) {
            return "UNKNOWN"
        }

        val lowerUrl = getNeedCheckUrl(url).lowercase()
        if (lowerUrl.startsWith("rtmp://") || lowerUrl.startsWith("rtmps://")) {
            return "RTMP"
        }
        if (lowerUrl.startsWith("rtsp://")) {
            return "RTSP"
        }

        val contentType = getHeader(headers, "Content-Type").orEmpty().substringBefore(";").trim().lowercase()
        detectFormatFromMime(contentType)?.let { return it }

        buildInspectionTargets(url, headers).forEach { target ->
            detectSplitArchiveFormat(target)?.let { return it }
            detectFormatFromExtensions(target)?.let { return it }
        }

        return when {
            contentType.startsWith("video/") -> "VIDEO"
            contentType.startsWith("audio/") -> "AUDIO"
            else -> "UNKNOWN"
        }
    }

    fun getMimeTypeForFormat(format: String, headers: Map<String, String> = emptyMap()): String {
        val headerMimeType = getHeader(headers, "Content-Type")
            ?.substringBefore(";")
            ?.trim()
            .orEmpty()
        if (headerMimeType.isNotBlank()) {
            return headerMimeType
        }

        return formatMimeTypes[format.uppercase()]?.firstOrNull().orEmpty()
    }

    private fun buildInspectionTargets(url: String, headers: Map<String, String>): List<String> {
        val targets = linkedSetOf<String>()
        val safeUrl = getNeedCheckUrl(url)
        val parsedUri = runCatching { Uri.parse(safeUrl) }.getOrNull()
        val fullUrl = safeUrl.lowercase()

        targets += fullUrl
        targets += safeUrl.substringBefore("?").lowercase()
        parsedUri?.lastPathSegment
            ?.substringBefore("?")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?.let(targets::add)

        extractFileNameFromContentDisposition(getHeader(headers, "Content-Disposition"))
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?.let(targets::add)

        parsedUri?.queryParameterNames.orEmpty().forEach { name ->
            parsedUri?.getQueryParameter(name)
                ?.lowercase()
                ?.takeIf { it.contains('.') }
                ?.let(targets::add)
        }

        return targets.toList()
    }

    private fun detectFormatFromExtensions(target: String): String? {
        val normalizedTarget = target.lowercase()
        for ((format, extensions) in formatExtensions) {
            if (extensions.any { extension -> normalizedTarget.contains(extension) }) {
                logDebug("Detected downloadable format by extension: $format in $target")
                return format
            }
        }
        return null
    }

    private fun detectSplitArchiveFormat(target: String): String? {
        val lowerTarget = target.lowercase()
        return when {
            Regex("\\.r0\\d*$").containsMatchIn(lowerTarget) -> "R0"
            Regex("\\.r1\\d*$").containsMatchIn(lowerTarget) -> "R1"
            else -> null
        }
    }

    private fun detectFormatFromMime(contentType: String): String? {
        if (contentType.isBlank()) {
            return null
        }

        for ((format, mimeTypes) in formatMimeTypes) {
            if (mimeTypes.any { mime -> contentType.equals(mime, ignoreCase = true) }) {
                logDebug("Detected downloadable format by Content-Type: $format / $contentType")
                return format
            }
        }

        return null
    }

    private fun extractFileNameFromContentDisposition(contentDisposition: String?): String? {
        val value = contentDisposition?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }

        val candidates = listOf("filename*=", "filename=")
        for (marker in candidates) {
            val startIndex = value.indexOf(marker, ignoreCase = true)
            if (startIndex < 0) {
                continue
            }

            return value
                .substring(startIndex + marker.length)
                .substringBefore(";")
                .substringAfter("''", "")
                .trim()
                .trim('"')
                .trim('\'')
                .takeIf { it.isNotBlank() }
        }

        return null
    }

    private fun hasAttachmentDisposition(headers: Map<String, String>): Boolean {
        return getHeader(headers, "Content-Disposition")?.contains("attachment", ignoreCase = true) == true
    }

    private fun hasFailedStatus(headers: Map<String, String>): Boolean {
        val statusCode = getHeader(headers, "X-Kiyori-Status-Code")
            ?.toIntOrNull()
            ?: return false
        return statusCode >= 400
    }

    private fun getHeader(headers: Map<String, String>, name: String): String? {
        return headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private fun logDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: RuntimeException) {
        }
    }

    private val preferredFormats = listOf(
        "M3U8", "DASH", "MP4", "FLV", "WEBM", "MKV", "MOV", "AVI", "3GP",
        "ASF", "WMV", "RMVB", "RM", "M4V", "MPEG", "MPG", "MPE", "OGV", "QT",
        "RTMP", "RTSP", "AAC", "AIF", "M4A", "MP3", "MPA", "OGG", "RA", "WAV",
        "WMA", "APK", "EXE", "MSI", "MSU", "PDF", "PPT", "PPS", "PLJ", "TIF",
        "TIFF", "ZIP", "RAR", "7Z", "TAR", "GZIP", "GZ", "BZ2", "ACE", "ARJ",
        "LZH", "SIT", "SITX", "SEA", "ISO", "IMG", "BIN", "Z", "R0", "R1",
        "VIDEO", "AUDIO", "UNKNOWN"
    )
}
