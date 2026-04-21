package com.android.kiyori.history.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.android.kiyori.R
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.ThemeManager

class PlaybackHistoryComposeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val historyManager = PlaybackHistoryManager(this)

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
                PlaybackHistoryScreen(
                    historyManager = historyManager,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onPlayVideo = { uri, startPosition ->
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            data = uri
                            putExtra("lastPosition", startPosition)
                        }
                        startActivity(intent)
                        startActivityWithDefaultTransition()
                    }
                )
            }
        }
    }
}

