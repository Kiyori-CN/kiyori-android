package com.android.kiyori.browser.domain

import android.net.Uri

enum class BrowserSearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlPrefix: String,
    val iconUrl: String
) {
    BAIDU(
        id = "baidu",
        displayName = "百度",
        searchUrlPrefix = "https://www.baidu.com/s?wd=",
        iconUrl = "https://www.baidu.com/favicon.ico"
    ),
    GOOGLE(
        id = "google",
        displayName = "Google",
        searchUrlPrefix = "https://www.google.com/search?q=",
        iconUrl = "https://www.google.com/favicon.ico"
    ),
    BING(
        id = "bing",
        displayName = "Bing",
        searchUrlPrefix = "https://www.bing.com/search?q=",
        iconUrl = "https://www.bing.com/favicon.ico"
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        searchUrlPrefix = "https://duckduckgo.com/?q=",
        iconUrl = "https://duckduckgo.com/favicon.ico"
    ),
    SOGOU(
        id = "sogou",
        displayName = "搜狗",
        searchUrlPrefix = "https://www.sogou.com/web?query=",
        iconUrl = "https://www.sogou.com/favicon.ico"
    ),
    SO360(
        id = "so360",
        displayName = "360搜索",
        searchUrlPrefix = "https://www.so.com/s?q=",
        iconUrl = "https://www.so.com/favicon.ico"
    ),
    YAHOO(
        id = "yahoo",
        displayName = "Yahoo",
        searchUrlPrefix = "https://search.yahoo.com/search?p=",
        iconUrl = "https://search.yahoo.com/favicon.ico"
    ),
    YANDEX(
        id = "yandex",
        displayName = "Yandex",
        searchUrlPrefix = "https://yandex.com/search/?text=",
        iconUrl = "https://yandex.com/favicon.ico"
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave",
        searchUrlPrefix = "https://search.brave.com/search?q=",
        iconUrl = "https://search.brave.com/favicon.ico"
    );

    fun buildSearchUrl(query: String): String {
        return searchUrlPrefix + Uri.encode(query)
    }

    companion object {
        val DEFAULT = BAIDU

        fun fromId(id: String?): BrowserSearchEngine {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}

