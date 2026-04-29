package com.android.kiyori.browser.domain

import android.net.Uri
import androidx.annotation.DrawableRes
import com.android.kiyori.R

enum class BrowserSearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlPrefix: String,
    @DrawableRes val iconResId: Int
) {
    BAIDU(
        id = "baidu",
        displayName = "百度",
        searchUrlPrefix = "https://www.baidu.com/s?wd=",
        iconResId = R.drawable.ic_kiyori_search_engine_baidu
    ),
    SOGOU(
        id = "sogou",
        displayName = "搜狗",
        searchUrlPrefix = "https://www.sogou.com/web?query=",
        iconResId = R.drawable.ic_kiyori_search_engine_sogou
    ),
    BING(
        id = "bing",
        displayName = "必应",
        searchUrlPrefix = "https://www.bing.com/search?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_bing
    ),
    SO360(
        id = "so360",
        displayName = "360搜索",
        searchUrlPrefix = "https://www.so.com/s?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_so360
    ),
    QUARK(
        id = "quark",
        displayName = "夸克",
        searchUrlPrefix = "https://quark.sm.cn/s?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_quark
    ),
    GOOGLE(
        id = "google",
        displayName = "谷歌",
        searchUrlPrefix = "https://www.google.com/search?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_google
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave",
        searchUrlPrefix = "https://search.brave.com/search?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_brave
    ),
    YANDEX(
        id = "yandex",
        displayName = "Yandex",
        searchUrlPrefix = "https://yandex.com/search/?text=",
        iconResId = R.drawable.ic_kiyori_search_engine_yandex
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        searchUrlPrefix = "https://duckduckgo.com/?q=",
        iconResId = R.drawable.ic_kiyori_search_engine_duckduckgo
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

