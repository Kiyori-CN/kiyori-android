package com.android.kiyori.download

import android.content.Context
import com.android.kiyori.manager.PreferencesManager

enum class DownloadEngineMode(
    val id: String,
    val label: String
) {
    INTERNAL("internal", "内置下载器"),
    SYSTEM("system", "系统下载器");

    companion object {
        fun fromId(id: String): DownloadEngineMode {
            return entries.firstOrNull { it.id == id } ?: INTERNAL
        }
    }
}

data class DownloadSettingsState(
    val defaultEngine: DownloadEngineMode,
    val customDirectoryUri: String,
    val customDirectoryName: String,
    val maxConcurrentTasks: Int,
    val normalThreadCount: Int,
    val m3u8ThreadCount: Int,
    val autoMergeM3u8: Boolean,
    val autoTransferToPublicDir: Boolean,
    val chunkSizeKb: Int,
    val autoCleanApk: Boolean,
    val skipConfirm: Boolean,
    val showCompletionTip: Boolean,
    val enableHttp2: Boolean
)

class DownloadPreferencesRepository(context: Context) {
    private val preferencesManager = PreferencesManager.getInstance(context.applicationContext)

    fun getSettings(): DownloadSettingsState {
        return DownloadSettingsState(
            defaultEngine = DownloadEngineMode.fromId(preferencesManager.getDownloadDefaultEngineId()),
            customDirectoryUri = preferencesManager.getDownloadCustomDirectoryUri().trim(),
            customDirectoryName = preferencesManager.getDownloadCustomDirectoryName().trim(),
            maxConcurrentTasks = preferencesManager.getDownloadMaxConcurrentTasks()
                .coerceIn(1, 8),
            normalThreadCount = preferencesManager.getDownloadNormalThreadCount()
                .coerceIn(1, 64),
            m3u8ThreadCount = preferencesManager.getDownloadM3u8ThreadCount()
                .coerceIn(1, 64),
            autoMergeM3u8 = preferencesManager.isDownloadM3u8AutoMergeEnabled(),
            autoTransferToPublicDir = preferencesManager.isDownloadAutoTransferPublicEnabled(),
            chunkSizeKb = preferencesManager.getDownloadChunkSizeKb()
                .coerceIn(128, 12288),
            autoCleanApk = preferencesManager.isDownloadApkAutoCleanEnabled(),
            skipConfirm = preferencesManager.isDownloadSkipConfirmEnabled(),
            showCompletionTip = preferencesManager.isDownloadCompletionTipEnabled(),
            enableHttp2 = preferencesManager.isDownloadHttp2Enabled()
        )
    }

    fun setDefaultEngine(mode: DownloadEngineMode) {
        preferencesManager.setDownloadDefaultEngineId(mode.id)
    }

    fun setCustomDirectory(uri: String, displayName: String) {
        preferencesManager.setDownloadCustomDirectoryUri(uri.trim())
        preferencesManager.setDownloadCustomDirectoryName(displayName.trim())
    }

    fun clearCustomDirectory() {
        preferencesManager.setDownloadCustomDirectoryUri("")
        preferencesManager.setDownloadCustomDirectoryName("")
    }

    fun setMaxConcurrentTasks(value: Int) {
        preferencesManager.setDownloadMaxConcurrentTasks(value.coerceIn(1, 8))
    }

    fun setNormalThreadCount(value: Int) {
        preferencesManager.setDownloadNormalThreadCount(value.coerceIn(1, 64))
    }

    fun setM3u8ThreadCount(value: Int) {
        preferencesManager.setDownloadM3u8ThreadCount(value.coerceIn(1, 64))
    }

    fun setAutoMergeM3u8(enabled: Boolean) {
        preferencesManager.setDownloadM3u8AutoMergeEnabled(enabled)
    }

    fun setAutoTransferToPublicDir(enabled: Boolean) {
        preferencesManager.setDownloadAutoTransferPublicEnabled(enabled)
    }

    fun setChunkSizeKb(value: Int) {
        preferencesManager.setDownloadChunkSizeKb(value.coerceIn(128, 12288))
    }

    fun setAutoCleanApk(enabled: Boolean) {
        preferencesManager.setDownloadApkAutoCleanEnabled(enabled)
    }

    fun setSkipConfirm(enabled: Boolean) {
        preferencesManager.setDownloadSkipConfirmEnabled(enabled)
    }

    fun setShowCompletionTip(enabled: Boolean) {
        preferencesManager.setDownloadCompletionTipEnabled(enabled)
    }

    fun setEnableHttp2(enabled: Boolean) {
        preferencesManager.setDownloadHttp2Enabled(enabled)
    }
}
