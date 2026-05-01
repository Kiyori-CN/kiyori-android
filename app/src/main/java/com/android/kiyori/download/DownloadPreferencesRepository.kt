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
        val normalThreadCount = preferencesManager.getDownloadNormalThreadCount()
            .coerceIn(1, 64)
        val m3u8ThreadCount = preferencesManager.getDownloadM3u8ThreadCount()
            .coerceIn(1, 64)
        return DownloadSettingsState(
            defaultEngine = DownloadEngineMode.fromId(preferencesManager.getDownloadDefaultEngineId()),
            customDirectoryUri = preferencesManager.getDownloadCustomDirectoryUri().trim(),
            customDirectoryName = preferencesManager.getDownloadCustomDirectoryName().trim(),
            maxConcurrentTasks = preferencesManager.getDownloadMaxConcurrentTasks()
                .coerceIn(1, maxConcurrentTasksFor(normalThreadCount, m3u8ThreadCount)),
            normalThreadCount = normalThreadCount,
            m3u8ThreadCount = m3u8ThreadCount,
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
        preferencesManager.setDownloadAutoTransferPublicEnabled(false)
    }

    fun clearCustomDirectory() {
        preferencesManager.setDownloadCustomDirectoryUri("")
        preferencesManager.setDownloadCustomDirectoryName("")
    }

    fun setMaxConcurrentTasks(value: Int) {
        val settings = getSettings()
        preferencesManager.setDownloadMaxConcurrentTasks(
            value.coerceIn(1, maxConcurrentTasksLimit(settings.normalThreadCount, settings.m3u8ThreadCount))
        )
    }

    fun maxConcurrentTasksLimit(
        normalThreadCount: Int = getSettings().normalThreadCount,
        m3u8ThreadCount: Int = getSettings().m3u8ThreadCount
    ): Int {
        val heaviestThreadCount = maxOf(normalThreadCount, m3u8ThreadCount).coerceAtLeast(1)
        return minOf(8, 128 / heaviestThreadCount).coerceAtLeast(1)
    }

    fun setNormalThreadCount(value: Int) {
        preferencesManager.setDownloadNormalThreadCount(value.coerceIn(1, 64))
        normalizeMaxConcurrentTasks()
    }

    fun setM3u8ThreadCount(value: Int) {
        preferencesManager.setDownloadM3u8ThreadCount(value.coerceIn(1, 64))
        normalizeMaxConcurrentTasks()
    }

    fun setAutoMergeM3u8(enabled: Boolean) {
        preferencesManager.setDownloadM3u8AutoMergeEnabled(enabled)
    }

    fun setAutoTransferToPublicDir(enabled: Boolean) {
        preferencesManager.setDownloadAutoTransferPublicEnabled(enabled)
        if (enabled) {
            clearCustomDirectory()
        }
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

    private fun normalizeMaxConcurrentTasks() {
        val settings = getSettings()
        setMaxConcurrentTasks(settings.maxConcurrentTasks)
    }

    private fun maxConcurrentTasksFor(normalThreadCount: Int, m3u8ThreadCount: Int): Int {
        return maxConcurrentTasksLimit(normalThreadCount, m3u8ThreadCount)
    }
}
