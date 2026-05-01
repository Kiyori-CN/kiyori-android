package com.android.kiyori.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.settings.UserAgreementActivity
import com.android.kiyori.app.ui.HomeScreen
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.enableTransparentSystemBars
import com.android.kiyori.utils.ThemeManager

class MainActivity : BaseActivity() {

    companion object {
        private const val EXTRA_INITIAL_TAB = "extra_initial_tab"
        private const val EXTRA_INITIAL_SETTINGS_PAGE = "extra_initial_settings_page"
        private const val EXTRA_NAV_REQUEST_ID = "extra_nav_request_id"
        const val TAB_HOME = "home"
        const val TAB_APPS = "apps"
        const val TAB_FILES = "files"
        const val TAB_AI = "ai"
        const val TAB_SETTINGS = "settings"
        const val SETTINGS_PAGE_BROWSER = "browser"
        const val SETTINGS_PAGE_DOWNLOAD = "download"

        fun start(
            context: android.content.Context,
            initialTab: String? = null,
            initialSettingsPage: String? = null
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                if (!initialTab.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_TAB, initialTab)
                }
                if (!initialSettingsPage.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_SETTINGS_PAGE, initialSettingsPage)
                }
                putExtra(EXTRA_NAV_REQUEST_ID, System.currentTimeMillis())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        }
    }

    private var historyManager: PlaybackHistoryManager? = null
    private var lastThemeName: String? = null
    private var needsRefresh = false
    private var requestedInitialTab: String? = null
    private var requestedInitialSettingsPage: String? = null
    private var requestedNavRequestId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableTransparentSystemBars()

        if (!UserAgreementActivity.isAgreed(this)) {
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        android.os.Looper.myQueue().addIdleHandler {
            historyManager = PlaybackHistoryManager(this)
            com.android.kiyori.utils.Logger.d(
                "MainActivity",
                "PlaybackHistoryManager initialized in idle"
            )
            false
        }

        lastThemeName = ThemeManager.getCurrentTheme(this).themeName
        requestedInitialTab = intent.getStringExtra(EXTRA_INITIAL_TAB)
        requestedInitialSettingsPage = intent.getStringExtra(EXTRA_INITIAL_SETTINGS_PAGE)
        requestedNavRequestId = intent.getLongExtra(EXTRA_NAV_REQUEST_ID, 0L)
        setupContent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedInitialTab = intent.getStringExtra(EXTRA_INITIAL_TAB)
        requestedInitialSettingsPage = intent.getStringExtra(EXTRA_INITIAL_SETTINGS_PAGE)
        requestedNavRequestId = intent.getLongExtra(EXTRA_NAV_REQUEST_ID, 0L)
        needsRefresh = false
        setupContent()
    }

    override fun onResume() {
        super.onResume()
        val currentThemeName = ThemeManager.getCurrentTheme(this).themeName
        if (lastThemeName != null && lastThemeName != currentThemeName) {
            lastThemeName = currentThemeName
            needsRefresh = false
            recreate()
        } else if (needsRefresh) {
            needsRefresh = false
            setupContent()
        }
    }

    override fun onPause() {
        super.onPause()
        needsRefresh = true
    }

    private fun setupContent() {
        val activity = this

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(activity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                HomeScreen(
                    historyManager = historyManager ?: PlaybackHistoryManager(activity),
                    initialTab = requestedInitialTab,
                    initialSettingsPage = requestedInitialSettingsPage,
                    navigationRequestId = requestedNavRequestId
                )
            }
        }
    }
}

