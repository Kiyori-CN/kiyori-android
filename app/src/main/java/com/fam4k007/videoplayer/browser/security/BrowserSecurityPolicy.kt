package com.fam4k007.videoplayer.browser.security

import android.net.Uri
import com.fam4k007.videoplayer.browser.domain.BrowserSearchEngine
import com.fam4k007.videoplayer.remote.RemotePlaybackHeaders

object BrowserSecurityPolicy {
    const val BLANK_HOME_URL: String = "about:blank"
    const val MOBILE_USER_AGENT: String =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
    val DESKTOP_USER_AGENT: String = RemotePlaybackHeaders.DEFAULT_USER_AGENT

    private val allowedWebSchemes = setOf("http", "https", "about")
    private val domainLikePattern =
        Regex("""^(localhost|(\d{1,3}\.){3}\d{1,3}|([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,})(:\d+)?([/?#].*)?$""")

    fun normalizeUserInput(
        input: String,
        searchEngine: BrowserSearchEngine = BrowserSearchEngine.DEFAULT
    ): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            return null
        }

        if (trimmed.equals(BLANK_HOME_URL, ignoreCase = true)) {
            return BLANK_HOME_URL
        }

        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase().orEmpty()
        if (scheme.isNotBlank()) {
            return trimmed.takeIf { scheme in allowedWebSchemes }
        }

        return if (looksLikeUrl(trimmed)) {
            "https://$trimmed"
        } else {
            buildSearchUrl(trimmed, searchEngine)
        }
    }

    fun shouldLoadInsideWebView(url: String): Boolean {
        val scheme = runCatching { Uri.parse(url).scheme.orEmpty().lowercase() }
            .getOrDefault("")
        return scheme in allowedWebSchemes
    }

    fun resolveUserAgent(url: String, isDesktopMode: Boolean): String {
        if (isDesktopMode) {
            return DESKTOP_USER_AGENT
        }

        val host = runCatching { Uri.parse(url).host.orEmpty().lowercase() }.getOrDefault("")
        return when {
            host.endsWith("bilibili.com") || host.endsWith("b23.tv") -> DESKTOP_USER_AGENT
            else -> MOBILE_USER_AGENT
        }
    }

    private fun looksLikeUrl(input: String): Boolean {
        if (input.contains(' ')) {
            return false
        }

        return domainLikePattern.matches(input) || input.contains('/')
    }

    private fun buildSearchUrl(query: String, searchEngine: BrowserSearchEngine): String {
        return searchEngine.buildSearchUrl(query)
    }
}
