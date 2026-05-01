package com.android.kiyori.webdav

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.android.kiyori.R
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.remote.RemotePlaybackLauncher
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.ui.theme.getThemeColors
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.ThemeManager

/**
 * WebDAV 文件浏览 Compose Activity
 */
class WebDavFileBrowserActivity : ComponentActivity() {
    
    private var accountId: String? = null
    private var onBackCallback: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        accountId = intent.getStringExtra("account_id")
        
        if (accountId == null) {
            Toast.makeText(this, "账户信息错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val accountManager = WebDavAccountManager.getInstance(this)
        val account = accountManager.getAccountById(accountId!!)
        
        if (account == null) {
            Toast.makeText(this, "账户不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        onBackPressedDispatcher.addCallback(this) {
            if (onBackCallback != null) {
                onBackCallback?.invoke()
            } else {
                finish()
                applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
            }
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
                    surface = themeColors.background,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = Color(0xFF212121)
                )
            ) {
                WebDavBrowserScreen(
                    account = account,
                    onNavigateBack = {
                        finish()
                        applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
                    },
                    onPlayVideo = { file, client ->
                        playVideo(file, client)
                    },
                    onBackCallbackChanged = { callback ->
                        onBackCallback = callback
                    }
                )
            }
        }
    }
    
    private fun playVideo(file: WebDavClient.WebDavFile, client: WebDavClient) {
        try {
            val fileUrl = client.getFileUrl(file.path)
            val headers = linkedMapOf<String, String>().apply {
                putAll(client.getAuthHeader().orEmpty())
            }

            android.util.Log.d(
                "WebDavBrowser",
                "播放 WebDAV 文件: url=$fileUrl, headers=${RemotePlaybackHeaders.describeForLog(headers)}"
            )

            RemotePlaybackLauncher.start(
                context = this,
                request = RemotePlaybackRequest(
                    url = fileUrl,
                    title = file.name,
                    headers = RemotePlaybackHeaders.normalize(headers),
                    source = RemotePlaybackRequest.Source.WEBDAV
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

