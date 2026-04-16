package com.fam4k007.videoplayer.browser.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.fam4k007.videoplayer.BaseActivity
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.browser.data.BrowserHistoryRepository
import com.fam4k007.videoplayer.browser.data.BrowserPreferencesRepository
import com.fam4k007.videoplayer.browser.data.BrowserSearchHistoryRepository
import com.fam4k007.videoplayer.browser.domain.BrowserPageState
import com.fam4k007.videoplayer.browser.web.BrowserWebViewCallbacks
import com.fam4k007.videoplayer.browser.web.BrowserWebViewController
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

class BrowserActivity : BaseActivity() {

    companion object {
        private const val EXTRA_INITIAL_URL = "extra_initial_url"

        fun start(context: Context, url: String = "") {
            val intent = Intent(context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_URL, url)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var browserWebViewController: BrowserWebViewController
    private lateinit var preferencesRepository: BrowserPreferencesRepository
    private lateinit var historyRepository: BrowserHistoryRepository
    private lateinit var searchHistoryRepository: BrowserSearchHistoryRepository
    private var pageState by mutableStateOf(BrowserPageState())
    private var shouldLoadInitialUrl = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL).orEmpty()
        shouldLoadInitialUrl = initialUrl.isNotBlank()
        preferencesRepository = BrowserPreferencesRepository(this)
        historyRepository = BrowserHistoryRepository(this)
        searchHistoryRepository = BrowserSearchHistoryRepository(this)
        browserWebViewController = BrowserWebViewController(
            preferencesRepository = preferencesRepository,
            historyRepository = historyRepository,
            searchHistoryRepository = searchHistoryRepository,
            callbacks = BrowserWebViewCallbacks(
                onStateChanged = { state -> pageState = state },
                onExternalUrlBlocked = { }
            )
        )

        if (initialUrl.isNotBlank()) {
            browserWebViewController.updateInputUrl(initialUrl)
        }

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = Color(0xFF212121),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFF4F6F8),
                    onSurface = Color(0xFF212121)
                )
            ) {
                BrowserScreen(
                    state = pageState,
                    onAttachWebView = { webView ->
                        browserWebViewController.attach(webView)
                        if (shouldLoadInitialUrl) {
                            shouldLoadInitialUrl = false
                            browserWebViewController.submitInputUrl()
                        }
                    },
                    onBackPressed = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onToggleUrlBar = {
                        browserWebViewController.setUrlBarVisible(!pageState.showUrlBar)
                    },
                    onInputChanged = browserWebViewController::updateInputUrl,
                    onSubmitInput = browserWebViewController::submitInputUrl,
                    onReload = browserWebViewController::reload,
                    onToggleDesktopMode = browserWebViewController::toggleDesktopMode,
                    onOpenExternalBrowser = {
                        openBrowserUrlExternally(this, pageState.currentUrl)
                    },
                    onCopyCurrentUrl = {
                        copyBrowserUrl(this, pageState.currentUrl)
                    },
                    onClearCookies = browserWebViewController::clearCookiesForCurrentPage,
                    onGoBack = browserWebViewController::goBack,
                    onGoForward = browserWebViewController::goForward,
                    onGoHome = browserWebViewController::loadHome,
                    onSelectSearchEngine = browserWebViewController::selectSearchEngine,
                    onOpenCurrentUrl = { browserWebViewController.loadUrl(pageState.currentUrl) },
                    onEditCurrentUrl = { browserWebViewController.updateInputUrl(pageState.currentUrl) },
                    onOpenSearchRecord = browserWebViewController::loadUrl,
                    onDeleteSearchRecord = browserWebViewController::deleteSearchRecord,
                    onClearSearchRecords = browserWebViewController::clearSearchHistory,
                    onOpenHistoryItem = browserWebViewController::loadUrl,
                    onDeleteHistoryItem = browserWebViewController::deleteHistoryItem,
                    onClearHistory = browserWebViewController::clearHistory
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.onResume()
        }
    }

    override fun onPause() {
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::browserWebViewController.isInitialized) {
            browserWebViewController.destroy()
        }
        super.onDestroy()
    }
}
