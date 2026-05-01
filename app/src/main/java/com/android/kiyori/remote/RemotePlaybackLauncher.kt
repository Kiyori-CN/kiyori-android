package com.android.kiyori.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.kiyori.browser.ui.BrowserActivity
import com.android.kiyori.bilibili.ui.BiliBiliPlayActivity
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.remote.ui.RemotePlaybackHistoryActivity
import com.android.kiyori.remote.ui.RemotePlaybackInputActivity

object RemotePlaybackLauncher {

    const val EXTRA_REMOTE_REQUEST = "remote_request"
    const val EXTRA_RETURN_DESTINATION = "remote_return_destination"
    const val EXTRA_RETURN_URL = "remote_return_url"

    const val RETURN_DESTINATION_BROWSER = "browser"
    const val RETURN_DESTINATION_REMOTE_INPUT = "remote_input"
    const val RETURN_DESTINATION_REMOTE_SEARCH_HISTORY = "remote_search_history"
    const val RETURN_DESTINATION_REMOTE_DEBUG_HISTORY = "remote_debug_history"
    const val RETURN_DESTINATION_BILIBILI = "bilibili"

    fun start(context: Context, request: RemotePlaybackRequest) {
        val parsedInput = RemoteUrlParser.parsePlaybackInput(request.url)
        val normalizedUrl = parsedInput?.url ?: RemoteUrlParser.normalizeForPlayback(request.url) ?: request.url.trim()
        val mergedHeaders = linkedMapOf<String, String>().apply {
            putAll(parsedInput?.headers.orEmpty())
            putAll(request.headers)
        }
        val normalizedRequest = request.copy(
            url = normalizedUrl,
            sourcePageUrl = RemotePlaybackHeaders.deriveSourcePageUrl(
                headers = mergedHeaders,
                sourcePageUrl = request.sourcePageUrl
            ),
            headers = RemotePlaybackHeaders.normalize(mergedHeaders)
        )
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = Uri.parse(normalizedUrl)
            putExtra(EXTRA_REMOTE_REQUEST, normalizedRequest)
            putExtra("is_online", true)
            if (normalizedRequest.source == RemotePlaybackRequest.Source.WEBDAV) {
                putExtra("is_webdav", true)
            }
            resolveReturnDestination(context)?.let { destination ->
                putExtra(EXTRA_RETURN_DESTINATION, destination)
            }
            if (normalizedRequest.sourcePageUrl.isNotBlank()) {
                putExtra(EXTRA_RETURN_URL, normalizedRequest.sourcePageUrl)
            }
            if (normalizedRequest.title.isNotBlank()) {
                putExtra("video_title", normalizedRequest.title)
            }
        }
        context.startActivity(intent)
    }

    private fun resolveReturnDestination(context: Context): String? {
        return when (context) {
            is BrowserActivity -> RETURN_DESTINATION_BROWSER
            is RemotePlaybackInputActivity -> RETURN_DESTINATION_REMOTE_INPUT
            is RemotePlaybackHistoryActivity -> {
                val historyType = context.intent.getStringExtra("history_type")
                if (historyType == RemotePlaybackHistoryActivity.HISTORY_TYPE_DEBUG) {
                    RETURN_DESTINATION_REMOTE_DEBUG_HISTORY
                } else {
                    RETURN_DESTINATION_REMOTE_SEARCH_HISTORY
                }
            }
            is BiliBiliPlayActivity -> RETURN_DESTINATION_BILIBILI
            else -> null
        }
    }
}

