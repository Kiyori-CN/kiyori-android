package com.android.kiyori.browser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BrowserSearchHistoryRepository(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    data class SearchRecordItem(
        val id: Long,
        val query: String,
        val targetUrl: String,
        val createdAt: Long
    )

    fun getSearchHistory(): List<SearchRecordItem> {
        val raw = sharedPreferences.getString(KEY_SEARCH_HISTORY, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        SearchRecordItem(
                            id = item.optLong("id"),
                            query = item.optString("query"),
                            targetUrl = item.optString("targetUrl"),
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addSearchHistory(query: String, targetUrl: String) {
        val normalizedQuery = query.trim()
        val normalizedTargetUrl = targetUrl.trim()
        if (normalizedQuery.isBlank() || normalizedTargetUrl.isBlank()) {
            return
        }

        val items = getSearchHistory().toMutableList()
        items.removeAll {
            it.query == normalizedQuery || it.targetUrl == normalizedTargetUrl
        }
        items.add(
            0,
            SearchRecordItem(
                id = System.currentTimeMillis(),
                query = normalizedQuery,
                targetUrl = normalizedTargetUrl,
                createdAt = System.currentTimeMillis()
            )
        )
        persistSearchHistory(items.take(MAX_HISTORY_ITEMS))
    }

    fun clearSearchHistory() {
        sharedPreferences.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun deleteSearchHistory(id: Long) {
        persistSearchHistory(getSearchHistory().filterNot { it.id == id })
    }

    private fun persistSearchHistory(items: List<SearchRecordItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("query", item.query)
                    put("targetUrl", item.targetUrl)
                    put("createdAt", item.createdAt)
                }
            )
        }
        sharedPreferences.edit().putString(KEY_SEARCH_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "browser_search_history"
        private const val KEY_SEARCH_HISTORY = "browser_search_history_list"
        private const val MAX_HISTORY_ITEMS = 12
    }
}

