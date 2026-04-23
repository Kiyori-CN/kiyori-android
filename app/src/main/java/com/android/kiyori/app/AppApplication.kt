package com.android.kiyori.app

import android.app.Application
import com.android.kiyori.browser.x5.BrowserX5KernelManager
import com.android.kiyori.database.VideoDatabase
import com.android.kiyori.download.InternalDownloadManager
import com.android.kiyori.manager.ThemeManager
import com.android.kiyori.utils.CrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用全局Application类
 */
class AppApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化全局异常处理器（优先级最高）
        CrashHandler.init(this)
        
        // 初始化主题
        ThemeManager.initTheme(this)

        // 预初始化 X5 浏览内核，未就绪时自动回退到系统内核
        BrowserX5KernelManager.initialize(this)
        InternalDownloadManager.initialize(this)

        // 后台预热数据库连接（减少首次查询延迟）
        applicationScope.launch {
            try {
                val db = VideoDatabase.getDatabase(this@AppApplication)
                // 执行一个简单查询预热连接
                db.videoCacheDao().getVideoCount()
                com.android.kiyori.utils.Logger.d("AppApplication", "Database warmed up")
            } catch (e: Exception) {
                // 预热失败不影响应用启动
                com.android.kiyori.utils.Logger.e("AppApplication", "Failed to warm up database", e)
            }
        }
    }
}

