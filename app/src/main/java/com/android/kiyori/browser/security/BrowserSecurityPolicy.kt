package com.android.kiyori.browser.security

import android.net.Uri
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.domain.BrowserUserAgentMode
import com.android.kiyori.remote.RemotePlaybackHeaders

object BrowserSecurityPolicy {
    const val BLANK_HOME_URL: String = "about:blank"
    const val MOBILE_USER_AGENT: String =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
    val DESKTOP_USER_AGENT: String = RemotePlaybackHeaders.DEFAULT_USER_AGENT
    const val IPHONE_USER_AGENT: String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
    const val SYMBIAN_WAP_USER_AGENT: String =
        "Mozilla/5.0 (SymbianOS/9.4; Series60/5.0 Nokia5800 XpressMusic/52.0.007; Profile/MIDP-2.1 Configuration/CLDC-1.1) AppleWebKit/413 (KHTML, like Gecko) Safari/413"

    private val allowedWebSchemes = setOf("http", "https", "about", "javascript", "blob", "data")
    private val userVisibleExternalSchemes = setOf("tel", "sms", "smsto", "mailto", "geo", "market")
    private val domainLikePattern =
        Regex("""^(localhost|(\d{1,3}\.){3}\d{1,3}|([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,})(:\d+)?([/?#].*)?$""")

    data class UserAgentBrandVersion(
        val brand: String,
        val majorVersion: String,
        val fullVersion: String
    )

    data class UserAgentProfile(
        val userAgent: String,
        val mobile: Boolean,
        val platform: String? = null,
        val platformVersion: String? = null,
        val model: String? = null,
        val fullVersion: String? = null,
        val brandVersions: List<UserAgentBrandVersion> = emptyList()
    )

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

    fun shouldAttemptExternalNavigation(url: String): Boolean {
        if (url.startsWith("intent:", ignoreCase = true)) {
            return true
        }
        val scheme = runCatching { Uri.parse(url).scheme.orEmpty().lowercase() }
            .getOrDefault("")
        return scheme.isNotBlank() && scheme !in allowedWebSchemes
    }

    fun shouldShowExternalNavigationError(url: String): Boolean {
        val scheme = runCatching { Uri.parse(url).scheme.orEmpty().lowercase() }
            .getOrDefault("")
        return scheme in userVisibleExternalSchemes
    }

    fun resolveSearchQuery(
        input: String,
        searchEngine: BrowserSearchEngine = BrowserSearchEngine.DEFAULT
    ): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank() || trimmed.equals(BLANK_HOME_URL, ignoreCase = true)) {
            return null
        }

        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase().orEmpty()
        if (scheme.isNotBlank()) {
            return null
        }

        return if (looksLikeUrl(trimmed)) {
            null
        } else {
            buildSearchUrl(trimmed, searchEngine)
                .takeIf { normalizeUserInput(trimmed, searchEngine) == it }
                ?.let { trimmed }
        }
    }

    fun resolveUserAgent(
        mode: BrowserUserAgentMode,
        defaultUserAgent: String = MOBILE_USER_AGENT,
        customUserAgent: String = ""
    ): String {
        return resolveUserAgentProfile(mode, defaultUserAgent, customUserAgent).userAgent
    }

    fun resolveUserAgentProfile(
        mode: BrowserUserAgentMode,
        defaultUserAgent: String = MOBILE_USER_AGENT,
        customUserAgent: String = ""
    ): UserAgentProfile {
        val safeDefaultUserAgent = defaultUserAgent.ifBlank { MOBILE_USER_AGENT }
        val trimmedCustomUserAgent = customUserAgent.trim()
        return when (mode) {
            BrowserUserAgentMode.ANDROID -> buildAndroidUserAgentProfile(safeDefaultUserAgent)
            BrowserUserAgentMode.PC_DESKTOP -> buildDesktopUserAgentProfile(safeDefaultUserAgent)
            BrowserUserAgentMode.IPHONE -> buildIPhoneUserAgentProfile(safeDefaultUserAgent)
            BrowserUserAgentMode.SYMBIAN_WAP -> UserAgentProfile(
                userAgent = SYMBIAN_WAP_USER_AGENT,
                mobile = true,
                platform = "SymbianOS"
            )
            BrowserUserAgentMode.CUSTOM_GLOBAL,
            BrowserUserAgentMode.CUSTOM_SITE -> {
                if (trimmedCustomUserAgent.isNotBlank()) {
                    buildCustomUserAgentProfile(trimmedCustomUserAgent)
                } else {
                    buildAndroidUserAgentProfile(safeDefaultUserAgent)
                }
            }
        }
    }

    private fun buildAndroidUserAgentProfile(defaultUserAgent: String): UserAgentProfile {
        val chrome = extractVersion(defaultUserAgent, "Chrome/([\\d.]+)")
            ?: extractVersion(defaultUserAgent, "CriOS/([\\d.]+)")
            ?: "123.0.0.0"
        val appleWebKit = extractVersion(defaultUserAgent, "AppleWebKit/([\\d.]+)") ?: "537.36"
        val safari = extractVersion(defaultUserAgent, "Safari/([\\d.]+)") ?: appleWebKit
        val platformSection = normalizeAndroidPlatformSection(defaultUserAgent)
        val androidVersion = extractVersion(defaultUserAgent, "Android\\s([\\d.]+)") ?: "13"

        return UserAgentProfile(
            userAgent = "Mozilla/5.0 ($platformSection) AppleWebKit/$appleWebKit " +
                "(KHTML, like Gecko) Chrome/$chrome Mobile Safari/$safari",
            mobile = true,
            platform = "Android",
            platformVersion = androidVersion,
            model = extractAndroidModel(platformSection),
            fullVersion = chrome,
            brandVersions = buildChromiumBrandVersions(chrome, includeGoogleChrome = true)
        )
    }

    private fun buildDesktopUserAgentProfile(defaultUserAgent: String): UserAgentProfile {
        val appleWebKit = extractVersion(defaultUserAgent, "AppleWebKit/([\\d.]+)") ?: "537.36"
        val chrome = extractVersion(defaultUserAgent, "Chrome/([\\d.]+)")
            ?: extractVersion(defaultUserAgent, "CriOS/([\\d.]+)")
            ?: "123.0.0.0"
        val safari = extractVersion(defaultUserAgent, "Safari/([\\d.]+)") ?: appleWebKit
        return UserAgentProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/$appleWebKit " +
                "(KHTML, like Gecko) Chrome/$chrome Safari/$safari",
            mobile = false,
            platform = "Windows",
            platformVersion = "10.0",
            fullVersion = chrome,
            brandVersions = buildChromiumBrandVersions(chrome, includeGoogleChrome = true)
        )
    }

    private fun buildIPhoneUserAgentProfile(defaultUserAgent: String): UserAgentProfile {
        val appleWebKit = extractVersion(defaultUserAgent, "AppleWebKit/([\\d.]+)") ?: "605.1.15"
        val safari = extractVersion(defaultUserAgent, "Safari/([\\d.]+)") ?: "604.1"
        return UserAgentProfile(
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) " +
                "AppleWebKit/$appleWebKit (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/$safari",
            mobile = true,
            platform = "iOS",
            platformVersion = "17.4",
            model = "iPhone"
        )
    }

    private fun buildCustomUserAgentProfile(userAgent: String): UserAgentProfile {
        val lowerUserAgent = userAgent.lowercase()
        val mobile = lowerUserAgent.contains("mobile") ||
            lowerUserAgent.contains("android") ||
            lowerUserAgent.contains("iphone") ||
            lowerUserAgent.contains("ipad") ||
            lowerUserAgent.contains("symbian")

        return UserAgentProfile(
            userAgent = userAgent,
            mobile = mobile,
            platform = when {
                lowerUserAgent.contains("windows") -> "Windows"
                lowerUserAgent.contains("android") -> "Android"
                lowerUserAgent.contains("iphone") || lowerUserAgent.contains("ipad") || lowerUserAgent.contains("ios") -> "iOS"
                lowerUserAgent.contains("mac os x") -> "macOS"
                lowerUserAgent.contains("linux") -> "Linux"
                else -> null
            },
            fullVersion = extractVersion(userAgent, "Chrome/([\\d.]+)")
                ?: extractVersion(userAgent, "Version/([\\d.]+)")
        )
    }

    private fun extractVersion(userAgent: String, pattern: String): String? {
        return Regex(pattern).find(userAgent)?.groupValues?.getOrNull(1)
    }

    private fun normalizeAndroidPlatformSection(defaultUserAgent: String): String {
        val rawSection = Regex("""Mozilla/5\.0 \(([^)]*)\)""")
            .find(defaultUserAgent)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        val normalizedTokens = rawSection.split(';')
            .map { it.trim() }
            .filter { token ->
                token.isNotBlank() &&
                    !token.equals("wv", ignoreCase = true) &&
                    !token.equals("Linux U", ignoreCase = true) &&
                    !token.startsWith("Version/", ignoreCase = true)
            }

        val platformSection = normalizedTokens.joinToString("; ")
        return if (platformSection.contains("Android", ignoreCase = true)) {
            platformSection
        } else {
            "Linux; Android 13; Pixel 7 Pro"
        }
    }

    private fun extractAndroidModel(platformSection: String): String? {
        return platformSection.split(';')
            .map { it.trim() }
            .firstOrNull { token ->
                token.isNotBlank() &&
                    !token.equals("Linux", ignoreCase = true) &&
                    !token.startsWith("Android", ignoreCase = true)
            }
            ?.substringBefore(" Build/")
            ?.takeIf { it.isNotBlank() }
    }

    private fun buildChromiumBrandVersions(
        fullVersion: String,
        includeGoogleChrome: Boolean
    ): List<UserAgentBrandVersion> {
        val majorVersion = fullVersion.substringBefore('.').ifBlank { "123" }
        val brands = mutableListOf(
            UserAgentBrandVersion(
                brand = "Chromium",
                majorVersion = majorVersion,
                fullVersion = fullVersion
            )
        )
        if (includeGoogleChrome) {
            brands += UserAgentBrandVersion(
                brand = "Google Chrome",
                majorVersion = majorVersion,
                fullVersion = fullVersion
            )
        }
        return brands
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
