package com.android.kiyori.browser.web

import com.android.kiyori.sniffer.UrlDetector
import java.util.concurrent.ConcurrentHashMap

object BrowserRequestBlocklistManager {
    private val blockedUrls = ConcurrentHashMap.newKeySet<String>()

    fun add(url: String) {
        val normalizedUrl = normalize(url) ?: return
        blockedUrls.add(normalizedUrl)
    }

    fun isBlocked(url: String): Boolean {
        val normalizedUrl = normalize(url) ?: return false
        return normalizedUrl in blockedUrls
    }

    private fun normalize(url: String): String? {
        return url.trim()
            .takeIf { it.isNotBlank() }
            ?.let(UrlDetector::getNeedCheckUrl)
    }
}
