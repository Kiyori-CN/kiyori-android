package com.android.kiyori.download.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.utils.enableTransparentSystemBars

class BilibiliDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentSystemBars()

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = androidx.compose.ui.graphics.Color.White,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.primary,
                    surfaceVariant = themeColors.primary,
                    onSurface = themeColors.onSurface
                )
            ) {
                DownloadScreen()
            }
        }
    }
}
