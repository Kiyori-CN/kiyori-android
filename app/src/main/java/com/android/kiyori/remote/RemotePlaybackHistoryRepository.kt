package com.android.kiyori.remote

import android.content.Context
import com.android.kiyori.app.AppConstants
import org.json.JSONArray
import org.json.JSONObject

class RemotePlaybackHistoryRepository(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        AppConstants.Preferences.PLAYER_PREFS,
        Context.MODE_PRIVATE
    )

    data class SearchHistoryItem(
        val id: Long,
        val url: String,
        val title: String,
        val sourcePageUrl: String,
        val createdAt: Long
    )

    data class DebugHistoryItem(
        val id: Long,
        val summary: String,
        val createdAt: Long
    )

    fun isSearchHistoryEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SEARCH_HISTORY_ENABLED, false)
    }

    fun setSearchHistoryEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SEARCH_HISTORY_ENABLED, enabled).apply()
    }

    fun isDebugHistoryEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DEBUG_HISTORY_ENABLED, false)
    }

    fun setDebugHistoryEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DEBUG_HISTORY_ENABLED, enabled).apply()
    }

    fun getSearchHistory(): List<SearchHistoryItem> {
        val raw = sharedPreferences.getString(KEY_SEARCH_HISTORY_LIST, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        SearchHistoryItem(
                            id = obj.optLong("id"),
                            url = obj.optString("url"),
                            title = obj.optString("title"),
                            sourcePageUrl = obj.optString("sourcePageUrl"),
                            createdAt = obj.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addSearchHistory(
        url: String,
        title: String,
        sourcePageUrl: String
    ) {
        if (!isSearchHistoryEnabled() || url.isBlank()) {
            return
        }

        val items = getSearchHistory().toMutableList()
        val normalizedUrl = url.trim()
        items.removeAll { it.url == normalizedUrl }
        items.add(
            0,
            SearchHistoryItem(
                id = System.currentTimeMillis(),
                url = normalizedUrl,
                title = title.trim(),
                sourcePageUrl = sourcePageUrl.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        persistSearchHistory(items.take(MAX_HISTORY_ITEMS))
    }

    fun deleteSearchHistory(id: Long) {
        persistSearchHistory(getSearchHistory().filterNot { it.id == id })
    }

    fun getDebugHistory(): List<DebugHistoryItem> {
        val raw = sharedPreferences.getString(KEY_DEBUG_HISTORY_LIST, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        DebugHistoryItem(
                            id = obj.optLong("id"),
                            summary = obj.optString("summary"),
                            createdAt = obj.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addDebugHistory(summary: String) {
        if (!isDebugHistoryEnabled() || summary.isBlank()) {
            return
        }

        val items = getDebugHistory().toMutableList()
        val trimmedSummary = summary.trim()
        items.removeAll { it.summary == trimmedSummary }
        items.add(
            0,
            DebugHistoryItem(
                id = System.currentTimeMillis(),
                summary = trimmedSummary,
                createdAt = System.currentTimeMillis()
            )
        )
        persistDebugHistory(items.take(MAX_HISTORY_ITEMS))
    }

    fun deleteDebugHistory(id: Long) {
        persistDebugHistory(getDebugHistory().filterNot { it.id == id })
    }

    private fun persistSearchHistory(items: List<SearchHistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("url", item.url)
                    put("title", item.title)
                    put("sourcePageUrl", item.sourcePageUrl)
                    put("createdAt", item.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_SEARCH_HISTORY_LIST, array.toString()).apply()
    }

    private fun persistDebugHistory(items: List<DebugHistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("summary", item.summary)
                    put("createdAt", item.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_DEBUG_HISTORY_LIST, array.toString()).apply()
    }

    companion object {
        private const val KEY_SEARCH_HISTORY_ENABLED = "remote_search_history_enabled"
        private const val KEY_DEBUG_HISTORY_ENABLED = "remote_debug_history_enabled"
        private const val KEY_SEARCH_HISTORY_LIST = "remote_search_history_list"
        private const val KEY_DEBUG_HISTORY_LIST = "remote_debug_history_list"
        private const val MAX_HISTORY_ITEMS = 50
    }
}

