package com.fam4k007.videoplayer.browser.domain

import com.fam4k007.videoplayer.browser.security.BrowserSecurityPolicy

data class BrowserHistoryEntry(
    val id: Long,
    val title: String,
    val url: String,
    val previewPath: String = "",
    val createdAt: Long
)

data class BrowserSearchRecord(
    val id: Long,
    val query: String,
    val targetUrl: String,
    val createdAt: Long
)

data class BrowserPageState(
    val inputUrl: String = "",
    val currentUrl: String = BrowserSecurityPolicy.BLANK_HOME_URL,
    val title: String = "内置浏览器",
    val searchEngine: BrowserSearchEngine = BrowserSearchEngine.DEFAULT,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val showUrlBar: Boolean = true,
    val blockedExternalUrl: String? = null,
    val historyEntries: List<BrowserHistoryEntry> = emptyList(),
    val searchRecords: List<BrowserSearchRecord> = emptyList()
) {
    val isBlankPage: Boolean
        get() = currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL
}
