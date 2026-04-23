package com.android.kiyori.browser.data

import android.content.Context
import android.net.Uri
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.domain.BrowserUserAgentMode
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.manager.PreferencesManager
import org.json.JSONObject

class BrowserPreferencesRepository(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context.applicationContext)

    fun getLastInputUrl(): String {
        return preferencesManager.getBrowserLastInputUrl()
    }

    fun setLastInputUrl(value: String) {
        preferencesManager.setBrowserLastInputUrl(value)
    }

    fun getHomeUrl(): String {
        val storedValue = preferencesManager.getBrowserHomeUrl().trim()
        if (storedValue.isBlank()) {
            return BrowserSecurityPolicy.BLANK_HOME_URL
        }
        return BrowserSecurityPolicy.normalizeUserInput(
            input = storedValue,
            searchEngine = getSearchEngine()
        ) ?: BrowserSecurityPolicy.BLANK_HOME_URL
    }

    fun setHomeUrl(value: String) {
        preferencesManager.setBrowserHomeUrl(value)
    }

    fun getSearchEngine(): BrowserSearchEngine {
        return BrowserSearchEngine.fromId(preferencesManager.getBrowserSearchEngineId())
    }

    fun setSearchEngine(engine: BrowserSearchEngine) {
        preferencesManager.setBrowserSearchEngineId(engine.id)
    }

    fun getUserAgentMode(): BrowserUserAgentMode {
        val storedId = preferencesManager.getBrowserUserAgentModeId()
        if (storedId.isNotBlank()) {
            return BrowserUserAgentMode.fromId(storedId)
        }
        return if (preferencesManager.isBrowserDesktopModeEnabled()) {
            BrowserUserAgentMode.PC_DESKTOP
        } else {
            BrowserUserAgentMode.ANDROID
        }
    }

    fun setUserAgentMode(mode: BrowserUserAgentMode) {
        preferencesManager.setBrowserUserAgentModeId(mode.id)
        preferencesManager.setBrowserDesktopModeEnabled(mode == BrowserUserAgentMode.PC_DESKTOP)
    }

    fun getCustomGlobalUserAgent(): String {
        return preferencesManager.getBrowserCustomGlobalUserAgent().trim()
    }

    fun setCustomGlobalUserAgent(value: String) {
        preferencesManager.setBrowserCustomGlobalUserAgent(value.trim())
    }

    fun getCustomSiteUserAgent(url: String): String {
        val siteKey = extractSiteKey(url) ?: return ""
        val allRules = readCustomSiteUserAgents()
        return allRules.optString(siteKey, "").trim()
    }

    fun setCustomSiteUserAgent(url: String, value: String) {
        val siteKey = extractSiteKey(url) ?: return
        val allRules = readCustomSiteUserAgents()
        val trimmedValue = value.trim()
        if (trimmedValue.isBlank()) {
            allRules.remove(siteKey)
        } else {
            allRules.put(siteKey, trimmedValue)
        }
        preferencesManager.setBrowserCustomSiteUserAgents(allRules.toString())
    }

    fun getCurrentSiteHost(url: String): String {
        return extractSiteKey(url).orEmpty()
    }

    fun isDesktopModeEnabled(): Boolean {
        return preferencesManager.isBrowserDesktopModeEnabled()
    }

    fun setDesktopModeEnabled(enabled: Boolean) {
        preferencesManager.setBrowserDesktopModeEnabled(enabled)
    }

    fun isIncognitoModeEnabled(): Boolean {
        return preferencesManager.isBrowserIncognitoModeEnabled()
    }

    fun setIncognitoModeEnabled(enabled: Boolean) {
        preferencesManager.setBrowserIncognitoModeEnabled(enabled)
    }

    fun isX5Enabled(): Boolean {
        return preferencesManager.isBrowserX5Enabled()
    }

    fun setX5Enabled(enabled: Boolean) {
        preferencesManager.setBrowserX5Enabled(enabled)
    }

    private fun readCustomSiteUserAgents(): JSONObject {
        val raw = preferencesManager.getBrowserCustomSiteUserAgents().trim()
        if (raw.isBlank()) {
            return JSONObject()
        }
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun extractSiteKey(url: String): String? {
        return runCatching { Uri.parse(url).host.orEmpty().lowercase() }
            .getOrDefault("")
            .takeIf { it.isNotBlank() }
    }
}
