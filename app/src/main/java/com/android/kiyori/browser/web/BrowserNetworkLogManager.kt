package com.android.kiyori.browser.web

import com.android.kiyori.sniffer.UrlDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrowserNetworkLogEntry(
    val url: String,
    val pageUrl: String = "",
    val title: String = "",
    val headers: Map<String, String> = emptyMap(),
    val category: UrlDetector.NetworkCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)

object BrowserNetworkLogManager {
    private const val MAX_ENTRIES = 300

    private val _entries = MutableStateFlow<List<BrowserNetworkLogEntry>>(emptyList())
    val entries: StateFlow<List<BrowserNetworkLogEntry>> = _entries.asStateFlow()

    fun addResource(
        url: String,
        headers: Map<String, String> = emptyMap(),
        pageUrl: String = "",
        pageTitle: String = ""
    ) {
        if (url.isBlank() || shouldIgnore(url)) {
            return
        }

        addEntry(
            BrowserNetworkLogEntry(
                url = url,
                pageUrl = pageUrl,
                title = pageTitle,
                headers = headers,
                category = UrlDetector.classifyNetworkResource(url, headers)
            )
        )
    }

    fun addBlocked(
        url: String,
        pageUrl: String = "",
        pageTitle: String = "",
        headers: Map<String, String> = emptyMap()
    ) {
        if (url.isBlank() || shouldIgnore(url)) {
            return
        }

        addEntry(
            BrowserNetworkLogEntry(
                url = url,
                pageUrl = pageUrl,
                title = pageTitle,
                headers = headers,
                category = UrlDetector.NetworkCategory.BLOCKED,
                isBlocked = true
            )
        )
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun startNewPage() {
        clear()
    }

    private fun addEntry(entry: BrowserNetworkLogEntry) {
        val currentList = _entries.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.url == entry.url &&
                it.pageUrl == entry.pageUrl &&
                it.category == entry.category &&
                it.isBlocked == entry.isBlocked
        }

        if (existingIndex >= 0) {
            val existing = currentList.removeAt(existingIndex)
            val merged = existing.copy(
                pageUrl = entry.pageUrl.ifBlank { existing.pageUrl },
                title = entry.title.ifBlank { existing.title },
                headers = if (entry.headers.isNotEmpty()) entry.headers else existing.headers,
                timestamp = entry.timestamp
            )
            currentList.add(0, merged)
        } else {
            currentList.add(0, entry)
        }

        if (currentList.size > MAX_ENTRIES) {
            currentList.subList(MAX_ENTRIES, currentList.size).clear()
        }

        _entries.value = currentList
    }

    private fun shouldIgnore(url: String): Boolean {
        return url.startsWith("blob:", ignoreCase = true) ||
            url.startsWith("data:", ignoreCase = true) ||
            url.startsWith("javascript:", ignoreCase = true)
    }
}
