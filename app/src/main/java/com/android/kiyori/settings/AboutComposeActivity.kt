package com.android.kiyori.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.android.kiyori.R
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.settings.ui.AboutScreen
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.ThemeManager

class AboutComposeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 获取版本号
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
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
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                AboutScreen(
                    versionName = versionName,
                    onBack = {
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onNavigateToLicense = {
                        startActivity(Intent(this, LicenseActivity::class.java))
                        startActivityWithDefaultTransition()
                    },
                    onNavigateToFeedback = {
                        startActivity(Intent(this, FeedbackActivity::class.java))
                        startActivityWithDefaultTransition()
                    }
                )
            }
        }
    }
}

