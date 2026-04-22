package com.android.kiyori.history.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.core.view.WindowCompat
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.browser.ui.BrowserActivity
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.utils.applyCloseActivityTransitionCompat

class HistoryComposeActivity : BaseActivity() {

    companion object {
        private const val EXTRA_INITIAL_SECTION = "extra_initial_section"

        fun start(
            context: Context,
            initialSection: HistorySection = HistorySection.WEB
        ) {
            context.startActivity(
                Intent(context, HistoryComposeActivity::class.java).apply {
                    putExtra(EXTRA_INITIAL_SECTION, initialSection.value)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val playbackHistoryManager = PlaybackHistoryManager(this)
        val initialSection = HistorySection.fromValue(
            intent.getStringExtra(EXTRA_INITIAL_SECTION)
        )

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)

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
                HistoryScreen(
                    playbackHistoryManager = playbackHistoryManager,
                    initialSection = initialSection,
                    onBack = {
                        finish()
                        applyCloseActivityTransitionCompat(
                            com.android.kiyori.R.anim.slide_in_left,
                            com.android.kiyori.R.anim.slide_out_right
                        )
                    },
                    onOpenBrowserHistory = { url ->
                        BrowserActivity.start(this, url = url)
                        startActivityWithDefaultTransition()
                    },
                    onOpenPlaybackHistory = { uri, startPosition ->
                        startActivity(
                            Intent(this, VideoPlayerActivity::class.java).apply {
                                data = uri
                                putExtra("lastPosition", startPosition)
                            }
                        )
                        startActivityWithDefaultTransition()
                    }
                )
            }
        }
    }
}

enum class HistorySection(
    val value: String,
    val label: String
) {
    MINI_PROGRAM("mini_program", "小程序"),
    WEB("web", "网页浏览"),
    PLAYBACK("playback", "播放记录");

    companion object {
        fun fromValue(value: String?): HistorySection {
            return values().firstOrNull { it.value == value } ?: WEB
        }
    }
}
