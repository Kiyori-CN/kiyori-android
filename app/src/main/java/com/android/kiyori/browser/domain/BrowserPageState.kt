package com.android.kiyori.browser.domain

import com.android.kiyori.browser.security.BrowserSecurityPolicy

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
    val title: String = "Kiyori",
    val searchEngine: BrowserSearchEngine = BrowserSearchEngine.DEFAULT,
    val userAgentMode: BrowserUserAgentMode = BrowserUserAgentMode.ANDROID,
    val isIncognitoMode: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val showUrlBar: Boolean = true,
    val lastSearchQuery: String = "",
    val showSearchEngineQuickSwitchBar: Boolean = false,
    val blockedExternalUrl: String? = null,
    val historyEntries: List<BrowserHistoryEntry> = emptyList(),
    val searchRecords: List<BrowserSearchRecord> = emptyList()
) {
    val isBlankPage: Boolean
        get() = currentUrl == BrowserSecurityPolicy.BLANK_HOME_URL
}
