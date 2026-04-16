package com.fam4k007.videoplayer.browser.data

import android.content.Context
import com.fam4k007.videoplayer.browser.domain.BrowserSearchEngine
import com.fam4k007.videoplayer.browser.security.BrowserSecurityPolicy
import com.fam4k007.videoplayer.manager.PreferencesManager

class BrowserPreferencesRepository(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context.applicationContext)

    fun getLastInputUrl(): String {
        return preferencesManager.getBrowserLastInputUrl()
    }

    fun setLastInputUrl(value: String) {
        preferencesManager.setBrowserLastInputUrl(value)
    }

    fun getHomeUrl(): String {
        return preferencesManager.getBrowserHomeUrl().ifBlank {
            BrowserSecurityPolicy.BLANK_HOME_URL
        }
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

    fun isDesktopModeEnabled(): Boolean {
        return preferencesManager.isBrowserDesktopModeEnabled()
    }

    fun setDesktopModeEnabled(enabled: Boolean) {
        preferencesManager.setBrowserDesktopModeEnabled(enabled)
    }
}
