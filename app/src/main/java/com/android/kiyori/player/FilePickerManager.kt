package com.android.kiyori.player

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.kiyori.danmaku.DanmakuManager
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.manager.PreferencesManager
import com.android.kiyori.manager.SubtitleManager
import com.android.kiyori.utils.DialogUtils
import com.android.kiyori.manager.compose.ComposeOverlayManager
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/**
 * 文件选择器管理器
 * 负责管理字幕和弹幕文件的导入
 */
class FilePickerManager(
    private val activityRef: WeakReference<AppCompatActivity>,
    private val subtitleManager: SubtitleManager,
    private val danmakuManager: DanmakuManager,
    private val historyManager: PlaybackHistoryManager,
    private val playbackEngineRef: WeakReference<PlaybackEngine>,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "FilePickerManager"
    }

    // 移除系统字幕和弹幕文件选择器，全面改用Compose UI选择器
    // private var subtitlePickerLauncher: ActivityResultLauncher<Array<String>>? = null
    // private var danmakuPickerLauncher: ActivityResultLauncher<Array<String>>? = null
    
    private var currentVideoUri: Uri? = null
    
    // Compose overlay管理器（用于显示自定义文件选择器）
    private var composeOverlayManager: ComposeOverlayManager? = null

    /**
     * 初始化文件选择器
     */
    fun initialize() {
        // 字幕和弹幕文件选择器全部改用Compose UI，不再注册系统选择器
        Log.d(TAG, "File pickers initialized (using Compose UI)")
    }
    
    /**
     * 设置Compose overlay管理器
     */
    fun setComposeOverlayManager(manager: ComposeOverlayManager) {
        composeOverlayManager = manager
    }

    /**
     * 设置当前播放的视频 URI（用于历史记录更新）
     */
    fun setCurrentVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }

    /**
     * 导入字幕文件（使用Compose UI选择器）
     */
    fun importSubtitle() {
        // 不再暂停视频，让选择器不影响播放状态
        
        // 获取上次选择的路径
        val lastPath = preferencesManager.getLastSubtitlePickerPath()
        
        // 显示Compose文件选择器
        composeOverlayManager?.showSubtitleFilePicker(
            initialPath = lastPath,
            onFileSelected = { filePath ->
                // 保存选择的路径
                val parentPath = File(filePath).parent
                if (parentPath != null) {
                    preferencesManager.setLastSubtitlePickerPath(parentPath)
                }
                
                // 处理选择的文件
                handleSubtitleFileSelected(filePath)
            }
        )
    }

    /**
     * 导入弹幕文件（使用Compose UI选择器）
     */
    fun importDanmaku() {
        // 不再暂停视频，让选择器不影响播放状态
        
        // 获取上次选择的路径
        val lastPath = preferencesManager.getLastDanmakuPickerPath()
        
        // 显示Compose文件选择器
        composeOverlayManager?.showDanmakuFilePicker(
            initialPath = lastPath,
            onFileSelected = { filePath ->
                // 保存选择的路径
                val parentPath = File(filePath).parent
                if (parentPath != null) {
                    preferencesManager.setLastDanmakuPickerPath(parentPath)
                }
                
                // 处理选择的文件
                handleDanmakuFileSelected(filePath)
            }
        )
    }

    /**
     * 处理选中的字幕文件（新方法，接收文件路径）
     */
    private fun handleSubtitleFileSelected(filePath: String) {
        val activity = activityRef.get() ?: return
        
        Log.d(TAG, "Subtitle file selected: $filePath")
        
        activity.lifecycleScope.launch {
            try {
                val success = subtitleManager.addExternalSubtitleFromPath(filePath)
                
                if (success) {
                    DialogUtils.showToastShort(activity, "字幕导入成功")
                    
                    // 保存外挂字幕路径到历史记录
                    currentVideoUri?.let { videoUri ->
                        preferencesManager.setExternalSubtitle(videoUri.toString(), filePath)
                        Log.d(TAG, "Saved external subtitle path: $filePath")
                    }
                } else {
                    DialogUtils.showToastLong(activity, "字幕导入失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import subtitle", e)
                DialogUtils.showToastLong(activity, "字幕导入失败: ${e.message}")
            }
        }
    }
    
    /**
     * 处理选中的弹幕文件（新方法，接收文件路径）
     */
    private fun handleDanmakuFileSelected(filePath: String) {
        val activity = activityRef.get() ?: return
        val playbackEngine = playbackEngineRef.get() ?: return
        
        Log.d(TAG, "Danmaku file selected: $filePath")
        
        activity.lifecycleScope.launch {
            try {
                val loaded = danmakuManager.loadDanmakuFile(filePath, autoShow = true)
                if (!loaded) {
                    DialogUtils.showToastLong(activity, "弹幕导入失败")
                    Log.w(TAG, "Danmaku file failed to load: $filePath")
                    return@launch
                }

                val currentPosition = (playbackEngine.currentPosition * 1000).toLong()
                danmakuManager.seekTo(currentPosition)
                DialogUtils.showToastShort(activity, "弹幕导入成功")
                Log.d(TAG, "Danmaku loaded and synced to position: $currentPosition")

                // 更新历史记录
                currentVideoUri?.let { videoUri ->
                    historyManager.updateDanmu(
                        uri = videoUri,
                        danmuPath = filePath,
                        danmuVisible = danmakuManager.isVisible(),
                        danmuOffsetTime = 0L
                    )
                    Log.d(TAG, "Danmu info updated in history")
                }
                
                // 不再恢复播放状态，保持用户原有的播放状态
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import danmaku", e)
                DialogUtils.showToastLong(activity, "弹幕导入失败: ${e.message}")
            }
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // 已移除系统文件选择器，不再需要清理 subtitlePickerLauncher 和 danmakuPickerLauncher
        currentVideoUri = null
        composeOverlayManager = null
        Log.d(TAG, "FilePickerManager cleaned up")
    }
}

