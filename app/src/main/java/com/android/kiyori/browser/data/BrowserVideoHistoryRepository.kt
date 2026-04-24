package com.android.kiyori.browser.data

import android.content.Context
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackRequest
import org.json.JSONArray
import org.json.JSONObject

class BrowserVideoHistoryRepository(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    data class HistoryItem(
        val id: Long,
        val url: String,
        val title: String,
        val sourcePageUrl: String,
        val headers: LinkedHashMap<String, String>,
        val createdAt: Long
    ) {
        fun toRemotePlaybackRequest(): RemotePlaybackRequest {
            return RemotePlaybackRequest(
                url = url,
                title = title,
                sourcePageUrl = sourcePageUrl,
                headers = LinkedHashMap(headers),
                source = RemotePlaybackRequest.Source.WEB_SNIFFER
            )
        }
    }

    fun getHistory(): List<HistoryItem> {
        val raw = sharedPreferences.getString(KEY_HISTORY_LIST, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        HistoryItem(
                            id = item.optLong("id"),
                            url = item.optString("url"),
                            title = item.optString("title"),
                            sourcePageUrl = item.optString("sourcePageUrl"),
                            headers = item.optJSONObject("headers").toLinkedHashMap(),
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun upsertHistory(
        url: String,
        title: String,
        sourcePageUrl: String,
        headers: Map<String, String>
    ) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            return
        }

        val currentItems = getHistory().toMutableList()
        currentItems.removeAll { it.url == normalizedUrl }
        currentItems.add(
            0,
            HistoryItem(
                id = System.currentTimeMillis(),
                url = normalizedUrl,
                title = title.trim().ifBlank { normalizedUrl.substringAfter("://").substringBefore("/") },
                sourcePageUrl = sourcePageUrl.trim(),
                headers = LinkedHashMap(RemotePlaybackHeaders.normalize(headers)),
                createdAt = System.currentTimeMillis()
            )
        )
        persistHistory(currentItems.take(MAX_HISTORY_ITEMS))
    }

    fun deleteHistory(id: Long) {
        persistHistory(getHistory().filterNot { it.id == id })
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY_LIST).apply()
    }

    private fun persistHistory(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("url", item.url)
                    put("title", item.title)
                    put("sourcePageUrl", item.sourcePageUrl)
                    put("createdAt", item.createdAt)
                    put("headers", JSONObject(item.headers as Map<*, *>))
                }
            )
        }
        sharedPreferences.edit().putString(KEY_HISTORY_LIST, array.toString()).apply()
    }

    private fun JSONObject?.toLinkedHashMap(): LinkedHashMap<String, String> {
        val result = linkedMapOf<String, String>()
        this ?: return result
        keys().forEach { key ->
            result[key] = optString(key)
        }
        return result
    }

    companion object {
        private const val PREF_NAME = "browser_network_video_history"
        private const val KEY_HISTORY_LIST = "browser_network_video_history_list"
        private const val MAX_HISTORY_ITEMS = 50
    }
}
