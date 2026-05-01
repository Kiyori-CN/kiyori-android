package com.android.kiyori.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.tencent.smtt.sdk.CookieManager

fun requestDownloadWithPreferences(
    context: Context,
    request: InternalDownloadRequest,
    contentLength: Long = -1L
) {
    val settings = DownloadPreferencesRepository(context).getSettings()
    val dispatch = {
        dispatchDownload(context, request, settings.defaultEngine).onSuccess { mode ->
            Toast.makeText(
                context,
                if (mode == DownloadEngineMode.SYSTEM) "已加入系统下载队列" else "已加入内置下载器",
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            Toast.makeText(context, "下载启动失败，已尝试交给外部应用", Toast.LENGTH_SHORT).show()
            openWithExternalApp(context, request.url)
        }
    }

    if (settings.skipConfirm) {
        dispatch()
        return
    }

    val fileName = request.fileName.ifBlank {
        URLUtil.guessFileName(request.url, null, request.mimeType.ifBlank { null })
    }
    val message = buildString {
        appendLine("文件：$fileName")
        if (request.mimeType.isNotBlank()) {
            appendLine("类型：${request.mimeType}")
        }
        if (contentLength > 0L) {
            appendLine("大小：${Formatter.formatFileSize(context, contentLength)}")
        }
        append("下载器：${settings.defaultEngine.label}")
    }

    AlertDialog.Builder(context)
        .setTitle("确认下载")
        .setMessage(message)
        .setNegativeButton("取消", null)
        .setPositiveButton("下载") { _, _ -> dispatch() }
        .show()
}

fun dispatchDownload(
    context: Context,
    request: InternalDownloadRequest,
    engineMode: DownloadEngineMode = DownloadPreferencesRepository(context).getSettings().defaultEngine
): Result<DownloadEngineMode> {
    return runCatching {
        when (engineMode) {
            DownloadEngineMode.INTERNAL -> {
                InternalDownloadManager.getInstance(context).enqueue(request).getOrThrow()
                DownloadEngineMode.INTERNAL
            }

            DownloadEngineMode.SYSTEM -> {
                enqueueSystemDownload(context, request)
                DownloadEngineMode.SYSTEM
            }
        }
    }
}

private fun enqueueSystemDownload(context: Context, request: InternalDownloadRequest): Long {
    val safeUrl = request.url.trim()
    require(safeUrl.isNotBlank()) { "下载地址不能为空" }

    val preparedHeaders = prepareDownloadHeadersForSystemRequest(request.copy(url = safeUrl))
    val guessedFileName = URLUtil.guessFileName(
        safeUrl,
        RemotePlaybackHeaders.get(preparedHeaders, "Content-Disposition"),
        request.mimeType.ifBlank { null }
    )
    val fileName = sanitizeSystemDownloadFileName(
        request.fileName.trim().ifBlank { guessedFileName }
    )
    val downloadRequest = DownloadManager.Request(Uri.parse(safeUrl)).apply {
        if (request.mimeType.isNotBlank()) {
            setMimeType(request.mimeType)
        }
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setTitle(request.title.ifBlank { fileName })
        setDescription(request.description.ifBlank { "准备下载" })
        preparedHeaders.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                addRequestHeader(key, value)
            }
        }
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Kiyori/$fileName")
    }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return downloadManager.enqueue(downloadRequest)
}

private fun prepareDownloadHeadersForSystemRequest(
    request: InternalDownloadRequest
): Map<String, String> {
    val headers = LinkedHashMap<String, String>()
    request.headers.forEach { (key, value) ->
        if (key.isNotBlank() && value.isNotBlank()) {
            headers[key] = value
        }
    }

    val referer = RemotePlaybackHeaders.get(headers, "Referer").orEmpty()
        .ifBlank { request.sourcePageUrl.trim() }
    if (referer.isNotBlank()) {
        headers.putIfAbsent("Referer", referer)
    }

    RemotePlaybackHeaders.deriveOrigin(referer)?.let {
        headers.putIfAbsent("Origin", it)
    }

    headers.putIfAbsent("User-Agent", RemotePlaybackHeaders.DEFAULT_USER_AGENT)
    headers.putIfAbsent("Accept", "*/*")
    headers.putIfAbsent("Accept-Encoding", "identity")

    if (RemotePlaybackHeaders.get(headers, "Cookie").isNullOrBlank()) {
        val cookie = runCatching {
            CookieManager.getInstance().getCookie(request.url).orEmpty()
                .ifBlank { CookieManager.getInstance().getCookie(referer).orEmpty() }
        }.getOrDefault("")
        if (cookie.isNotBlank()) {
            headers["Cookie"] = cookie
        }
    }

    return headers
}

private fun sanitizeSystemDownloadFileName(value: String): String {
    val safeName = value
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
        .trim()
        .trim('.')
    return safeName.ifBlank { "download_${System.currentTimeMillis()}" }
}

private fun openWithExternalApp(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
