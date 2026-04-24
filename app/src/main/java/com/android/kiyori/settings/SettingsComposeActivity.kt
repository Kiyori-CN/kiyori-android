package com.android.kiyori.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.kiyori.R
import com.android.kiyori.settings.ui.SettingsScreen
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.enableTransparentSystemBars
import com.android.kiyori.utils.ThemeManager

/**
 * Compose 版本的设置 Activity
 */
class SettingsComposeActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_INITIAL_PAGE = "extra_initial_page"
        const val INITIAL_PAGE_BROWSER = "browser"

        fun start(
            context: Context,
            initialPage: String? = null
        ) {
            val intent = Intent(context, SettingsComposeActivity::class.java).apply {
                if (!initialPage.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_PAGE, initialPage)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableTransparentSystemBars()
        
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
                    surface = themeColors.background,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = Color(0xFF212121)
                )
            ) {
                SettingsScreen(
                    onNavigateBack = {
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    initialPage = intent.getStringExtra(EXTRA_INITIAL_PAGE)
                )
            }
        }
    }
}

