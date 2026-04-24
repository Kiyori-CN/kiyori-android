package com.android.kiyori.webdav

import android.content.Intent
import android.net.Uri
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
import com.android.kiyori.player.ui.VideoPlayerActivity
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
            // 构建包含认证信息的 URL
            val fileUrl = if (client.config.isAnonymous || client.config.account.isEmpty()) {
                // 匿名访问
                client.getFileUrl(file.path)
            } else {
                // 账号认证：在 URL 中嵌入用户名和密码
                val uri = Uri.parse(client.config.serverUrl)
                val scheme = uri.scheme // http 或 https
                val host = uri.host
                val port = if (uri.port != -1) ":${uri.port}" else ""
                val username = Uri.encode(client.config.account)
                val password = Uri.encode(client.config.password)
                
                // 获取基础路径（如 /dav/）
                val basePath = uri.path ?: "/"
                // 对文件路径的每个部分进行编码
                val encodedPath = file.path.split("/").joinToString("/") { Uri.encode(it) }
                val fullPath = "$basePath${if (encodedPath.startsWith("/")) encodedPath.substring(1) else encodedPath}"
                
                "$scheme://$username:$password@$host$port$fullPath"
            }
            
            android.util.Log.d("WebDavBrowser", "播放 URL: $fileUrl")
            
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                data = Uri.parse(fileUrl)
                action = Intent.ACTION_VIEW
                putExtra("is_webdav", true)
                putExtra("file_name", file.name)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

