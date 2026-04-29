package com.android.kiyori.manager

import android.content.Context
import android.content.SharedPreferences
import com.android.kiyori.app.AppConstants

enum class LongPressSpeedOption(
    val storageValue: String,
    val label: String
) {
    Relative2x("relative_2x", "2倍(原速度上加倍)"),
    Relative3x("relative_3x", "3倍(原速度上加倍)"),
    Relative4x("relative_4x", "4倍(原速度上加倍)"),
    Fixed2x("fixed_2x", "固定2倍"),
    Fixed3x("fixed_3x", "固定3倍"),
    Fixed4x("fixed_4x", "固定4倍");

    companion object {
        fun fromStorageValue(value: String?): LongPressSpeedOption? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}

/**
 * 统一的设置管理器（单例模式）
 * 集中所有 SharedPreferences 操作，避免重复创建和散落在各处
 */
class PreferencesManager private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        AppConstants.Preferences.PLAYER_PREFS,
        Context.MODE_PRIVATE
    )
    
    companion object {
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // ==================== 快进时长 ====================
    
    /**
     * 获取快进/快退时长（秒）
     */
    fun getSeekTime(): Int {
        return sharedPreferences.getInt(
            AppConstants.Preferences.SEEK_TIME,
            AppConstants.Defaults.DEFAULT_SEEK_TIME
        )
    }
    
    /**
     * 保存快进/快退时长
     */
    fun setSeekTime(seconds: Int) {
        sharedPreferences.edit().putInt(AppConstants.Preferences.SEEK_TIME, seconds).apply()
    }
    
    // ==================== 长按倍速 ====================
    
    /**
     * 获取长按倍速
     */
    fun getLongPressSpeed(): Float {
        return sharedPreferences.getFloat(
            AppConstants.Preferences.LONG_PRESS_SPEED,
            AppConstants.Defaults.DEFAULT_LONG_PRESS_SPEED
        )
    }
    
    /**
     * 保存长按倍速
     */
    fun setLongPressSpeed(speed: Float) {
        sharedPreferences.edit().putFloat(AppConstants.Preferences.LONG_PRESS_SPEED, speed).apply()
    }

    fun getLongPressSpeedOption(): LongPressSpeedOption {
        val storedValue = sharedPreferences.getString(
            AppConstants.Preferences.LONG_PRESS_SPEED_OPTION,
            null
        )
        LongPressSpeedOption.fromStorageValue(storedValue)?.let { return it }

        val legacySpeed = getLongPressSpeed()
        return when {
            legacySpeed < 2.5f -> LongPressSpeedOption.Fixed2x
            legacySpeed < 3.5f -> LongPressSpeedOption.Fixed3x
            else -> LongPressSpeedOption.Fixed4x
        }
    }

    fun setLongPressSpeedOption(option: LongPressSpeedOption) {
        sharedPreferences.edit()
            .putString(AppConstants.Preferences.LONG_PRESS_SPEED_OPTION, option.storageValue)
            .putFloat(
                AppConstants.Preferences.LONG_PRESS_SPEED,
                when (option) {
                    LongPressSpeedOption.Relative2x, LongPressSpeedOption.Fixed2x -> 2.0f
                    LongPressSpeedOption.Relative3x, LongPressSpeedOption.Fixed3x -> 3.0f
                    LongPressSpeedOption.Relative4x, LongPressSpeedOption.Fixed4x -> 4.0f
                }
            )
            .apply()
    }

    fun resolveLongPressSpeed(baseSpeed: Float): Float {
        return when (getLongPressSpeedOption()) {
            LongPressSpeedOption.Relative2x -> baseSpeed * 2.0f
            LongPressSpeedOption.Relative3x -> baseSpeed * 3.0f
            LongPressSpeedOption.Relative4x -> baseSpeed * 4.0f
            LongPressSpeedOption.Fixed2x -> 2.0f
            LongPressSpeedOption.Fixed3x -> 3.0f
            LongPressSpeedOption.Fixed4x -> 4.0f
        }
    }
    
    // ==================== 精确进度定位 ====================
    
    /**
     * 获取是否启用精确进度定位
     */
    fun isPreciseSeekingEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            AppConstants.Preferences.PRECISE_SEEKING,
            false
        )
    }
    
    /**
     * 保存精确进度定位设置
     */
    fun setPreciseSeekingEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(AppConstants.Preferences.PRECISE_SEEKING, enabled).apply()
    }
    
    // ==================== 音量增强 ====================
    
    /**
     * 获取是否启用音量增强(允许音量超过100%)
     */
    fun isVolumeBoostEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            AppConstants.Preferences.VOLUME_BOOST_ENABLED,
            false  // 默认关闭
        )
    }
    
    /**
     * 保存音量增强设置
     */
    fun setVolumeBoostEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(AppConstants.Preferences.VOLUME_BOOST_ENABLED, enabled).apply()
    }
    
    // ==================== Anime4K 超分模式记忆 ====================
    
    /**
     * 获取是否启用Anime4K模式记忆
     */
    fun isAnime4KMemoryEnabled(): Boolean {
        return sharedPreferences.getBoolean(
            AppConstants.Preferences.ANIME4K_MEMORY_ENABLED,
            false  // 默认关闭
        )
    }
    
    /**
     * 保存Anime4K模式记忆设置
     */
    fun setAnime4KMemoryEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(AppConstants.Preferences.ANIME4K_MEMORY_ENABLED, enabled).apply()
    }
    
    /**
     * 获取上次使用的Anime4K模式
     * @return 模式名称（OFF, A, B, C, A_PLUS, B_PLUS, C_PLUS）
     */
    fun getLastAnime4KMode(): String {
        return sharedPreferences.getString(
            AppConstants.Preferences.ANIME4K_LAST_MODE,
            "OFF"  // 默认为OFF
        ) ?: "OFF"
    }
    
    /**
     * 保存当前使用的Anime4K模式
     * @param mode 模式名称
     */
    fun setLastAnime4KMode(mode: String) {
        sharedPreferences.edit().putString(AppConstants.Preferences.ANIME4K_LAST_MODE, mode).apply()
    }
    
    // ==================== 双击手势设置 ====================
    
    /**
     * 获取双击手势模式
     * @return 0=暂停/播放, 1=快进/快退
     */
    fun getDoubleTapMode(): Int {
        return sharedPreferences.getInt(
            AppConstants.Preferences.DOUBLE_TAP_MODE,
            AppConstants.Defaults.DEFAULT_DOUBLE_TAP_MODE
        )
    }
    
    /**
     * 设置双击手势模式
     * @param mode 0=暂停/播放, 1=快进/快退
     */
    fun setDoubleTapMode(mode: Int) {
        sharedPreferences.edit().putInt(AppConstants.Preferences.DOUBLE_TAP_MODE, mode).apply()
    }
    
    /**
     * 获取双击跳转秒数（仅在快进/快退模式下有效）
     */
    fun getDoubleTapSeekSeconds(): Int {
        return sharedPreferences.getInt(
            AppConstants.Preferences.DOUBLE_TAP_SEEK_SECONDS,
            AppConstants.Defaults.DEFAULT_DOUBLE_TAP_SEEK_SECONDS
        )
    }
    
    /**
     * 设置双击跳转秒数
     */
    fun setDoubleTapSeekSeconds(seconds: Int) {
        sharedPreferences.edit().putInt(AppConstants.Preferences.DOUBLE_TAP_SEEK_SECONDS, seconds).apply()
    }
    
    // ==================== 播放位置（用于记忆播放进度）====================
    
    /**
     * 获取视频的保存播放位置
     * @param videoUri 视频URI（使用 uri.toString() 作为键）
     */
    fun getPlaybackPosition(videoUri: String): Double {
        return sharedPreferences.getFloat(videoUri, 0f).toDouble()
    }
    
    /**
     * 保存视频播放位置
     */
    fun setPlaybackPosition(videoUri: String, position: Double) {
        sharedPreferences.edit().putFloat(videoUri, position.toFloat()).apply()
    }
    
    /**
     * 清除视频播放位置记录
     */
    fun clearPlaybackPosition(videoUri: String) {
        sharedPreferences.edit().remove(videoUri).apply()
    }
    
    // ==================== 字幕偏好设置（针对每个视频）====================
    
    /**
     * 获取视频的外部字幕路径
     */
    fun getExternalSubtitle(videoUri: String): String? {
        return sharedPreferences.getString("${videoUri}_subtitle", null)
    }
    
    /**
     * 保存视频的外部字幕路径
     */
    fun setExternalSubtitle(videoUri: String, subtitlePath: String) {
        sharedPreferences.edit().putString("${videoUri}_subtitle", subtitlePath).apply()
    }
    
    /**
     * 获取视频的字幕大小
     */
    fun getSubtitleScale(videoUri: String): Double {
        return sharedPreferences.getFloat("${videoUri}_sub_scale", 1.0f).toDouble()
    }
    
    /**
     * 保存视频的字幕大小
     */
    fun setSubtitleScale(videoUri: String, scale: Double) {
        sharedPreferences.edit().putFloat("${videoUri}_sub_scale", scale.toFloat()).apply()
    }
    
    /**
     * 获取视频的字幕位置
     */
    fun getSubtitlePosition(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_pos", 100)
    }
    
    /**
     * 保存视频的字幕位置
     */
    fun setSubtitlePosition(videoUri: String, position: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_pos", position).apply()
    }
    
    /**
     * 获取视频的字幕延迟
     */
    fun getSubtitleDelay(videoUri: String): Double {
        return sharedPreferences.getFloat("${videoUri}_sub_delay", 0f).toDouble()
    }
    
    /**
     * 保存视频的字幕延迟
     */
    fun setSubtitleDelay(videoUri: String, delay: Double) {
        sharedPreferences.edit().putFloat("${videoUri}_sub_delay", delay.toFloat()).apply()
    }
    
    /**
     * 获取是否启用ASS字幕样式覆盖
     */
    fun isAssOverrideEnabled(videoUri: String): Boolean {
        return sharedPreferences.getBoolean("${videoUri}_ass_override", false)
    }
    
    /**
     * 保存ASS字幕样式覆盖设置
     */
    fun setAssOverrideEnabled(videoUri: String, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("${videoUri}_ass_override", enabled).apply()
    }
    
    /**
     * 获取视频的字幕轨道ID
     */
    fun getSubtitleTrackId(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_track_id", -1)
    }
    
    /**
     * 保存视频的字幕轨道ID
     */
    fun setSubtitleTrackId(videoUri: String, trackId: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_track_id", trackId).apply()
    }
    
    // ==================== 字幕样式设置 ====================
    
    /**
     * 获取字幕文本颜色（ARGB格式）
     */
    fun getSubtitleTextColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_text_color", "#FFFFFF") ?: "#FFFFFF"
    }
    
    /**
     * 保存字幕文本颜色
     */
    fun setSubtitleTextColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_text_color", color).apply()
    }
    
    /**
     * 获取字幕描边粗细
     */
    fun getSubtitleBorderSize(videoUri: String): Int {
        return sharedPreferences.getInt("${videoUri}_sub_border_size", 3)
    }
    
    /**
     * 保存字幕描边粗细
     */
    fun setSubtitleBorderSize(videoUri: String, size: Int) {
        sharedPreferences.edit().putInt("${videoUri}_sub_border_size", size).apply()
    }
    
    /**
     * 获取字幕描边颜色（ARGB格式）
     */
    fun getSubtitleBorderColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_border_color", "#000000") ?: "#000000"
    }
    
    /**
     * 保存字幕描边颜色
     */
    fun setSubtitleBorderColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_border_color", color).apply()
    }
    
    /**
     * 获取字幕背景颜色（ARGB格式）
     */
    fun getSubtitleBackColor(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_back_color", "#00000000") ?: "#00000000"
    }
    
    /**
     * 保存字幕背景颜色
     */
    fun setSubtitleBackColor(videoUri: String, color: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_back_color", color).apply()
    }
    
    /**
     * 获取字幕描边样式
     */
    fun getSubtitleBorderStyle(videoUri: String): String {
        return sharedPreferences.getString("${videoUri}_sub_border_style", "outline-and-shadow") ?: "outline-and-shadow"
    }
    
    /**
     * 保存字幕描边样式
     */
    fun setSubtitleBorderStyle(videoUri: String, style: String) {
        sharedPreferences.edit().putString("${videoUri}_sub_border_style", style).apply()
    }
    
    // ==================== 主题设置 ====================
    
    /**
     * 获取主题模式
     * @return "light" 亮色 | "dark" 深色 | "system" 跟随系统
     */
    fun getThemeMode(): String {
        return sharedPreferences.getString(
            "theme_mode",
            "light"  // 默认亮色
        ) ?: "light"
    }
    
    /**
     * 保存主题模式
     */
    fun setThemeMode(mode: String) {
        sharedPreferences.edit().putString("theme_mode", mode).apply()
    }
    
    // ==================== 弹幕设置 ====================
    
    fun getDanmakuEnabled(): Boolean {
        return sharedPreferences.getBoolean("danmaku_enabled", true)
    }
    
    fun setDanmakuEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_enabled", enabled).apply()
    }
    
    fun getDanmakuSize(): Int {
        return sharedPreferences.getInt("danmaku_size", 50)
    }
    
    fun setDanmakuSize(size: Int) {
        sharedPreferences.edit().putInt("danmaku_size", size).apply()
    }
    
    fun getDanmakuSpeed(): Int {
        return sharedPreferences.getInt("danmaku_speed", 50)
    }
    
    fun setDanmakuSpeed(speed: Int) {
        sharedPreferences.edit().putInt("danmaku_speed", speed).apply()
    }
    
    fun getDanmakuAlpha(): Int {
        return sharedPreferences.getInt("danmaku_alpha", 100)
    }
    
    fun setDanmakuAlpha(alpha: Int) {
        sharedPreferences.edit().putInt("danmaku_alpha", alpha).apply()
    }
    
    fun getDanmakuStroke(): Int {
        return sharedPreferences.getInt("danmaku_stroke", 50)
    }
    
    fun setDanmakuStroke(stroke: Int) {
        sharedPreferences.edit().putInt("danmaku_stroke", stroke).apply()
    }
    
    fun getDanmakuOffsetTime(): Long {
        return sharedPreferences.getLong("danmaku_offset_time", 0L)
    }
    
    fun setDanmakuOffsetTime(time: Long) {
        sharedPreferences.edit().putLong("danmaku_offset_time", time).apply()
    }
    
    fun getDanmakuShowScroll(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_scroll", true)
    }
    
    fun setDanmakuShowScroll(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_scroll", show).apply()
    }
    
    fun getDanmakuShowTop(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_top", true)
    }
    
    fun setDanmakuShowTop(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_top", show).apply()
    }
    
    fun getDanmakuShowBottom(): Boolean {
        return sharedPreferences.getBoolean("danmaku_show_bottom", true)
    }
    
    fun setDanmakuShowBottom(show: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_show_bottom", show).apply()
    }
    
    fun getDanmakuMaxScrollLine(): Int {
        return sharedPreferences.getInt("danmaku_max_scroll_line", 0)
    }
    
    fun setDanmakuMaxScrollLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_scroll_line", line).apply()
    }
    
    fun getDanmakuMaxTopLine(): Int {
        return sharedPreferences.getInt("danmaku_max_top_line", 0)
    }
    
    fun setDanmakuMaxTopLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_top_line", line).apply()
    }
    
    fun getDanmakuMaxBottomLine(): Int {
        return sharedPreferences.getInt("danmaku_max_bottom_line", 0)
    }
    
    fun setDanmakuMaxBottomLine(line: Int) {
        sharedPreferences.edit().putInt("danmaku_max_bottom_line", line).apply()
    }
    
    fun getDanmakuMaxScreenNum(): Int {
        return sharedPreferences.getInt("danmaku_max_screen_num", 0)
    }
    
    fun setDanmakuMaxScreenNum(num: Int) {
        sharedPreferences.edit().putInt("danmaku_max_screen_num", num).apply()
    }
    
    fun getDanmakuUseChoreographer(): Boolean {
        return sharedPreferences.getBoolean("danmaku_use_choreographer", true)
    }
    
    fun setDanmakuUseChoreographer(use: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_use_choreographer", use).apply()
    }
    
    fun getDanmakuDebug(): Boolean {
        return sharedPreferences.getBoolean("danmaku_debug", false)
    }
    
    fun setDanmakuDebug(debug: Boolean) {
        sharedPreferences.edit().putBoolean("danmaku_debug", debug).apply()
    }
    
    // ==================== 视频列表排序设置 ====================
    
    /**
     * 获取视频列表排序类型 (NAME, DATE)
     */
    fun getVideoSortType(): String {
        return sharedPreferences.getString("video_sort_type", "NAME") ?: "NAME"
    }
    
    /**
     * 保存视频列表排序类型
     */
    fun setVideoSortType(type: String) {
        sharedPreferences.edit().putString("video_sort_type", type).apply()
    }
    
    /**
     * 获取视频列表排序顺序 (ASCENDING, DESCENDING)
     */
    fun getVideoSortOrder(): String {
        return sharedPreferences.getString("video_sort_order", "ASCENDING") ?: "ASCENDING"
    }
    
    /**
     * 保存视频列表排序顺序
     */
    fun setVideoSortOrder(order: String) {
        sharedPreferences.edit().putString("video_sort_order", order).apply()
    }
    
    /**
     * 获取文件夹列表排序类型 (NAME, VIDEO_COUNT)
     */
    fun getFolderSortType(): String {
        return sharedPreferences.getString("folder_sort_type", "VIDEO_COUNT") ?: "VIDEO_COUNT"
    }
    
    /**
     * 保存文件夹列表排序类型
     */
    fun setFolderSortType(type: String) {
        sharedPreferences.edit().putString("folder_sort_type", type).apply()
    }
    
    /**
     * 获取文件夹列表排序顺序 (ASCENDING, DESCENDING)
     */
    fun getFolderSortOrder(): String {
        return sharedPreferences.getString("folder_sort_order", "DESCENDING") ?: "DESCENDING"
    }
    
    /**
     * 保存文件夹列表排序顺序
     */
    fun setFolderSortOrder(order: String) {
        sharedPreferences.edit().putString("folder_sort_order", order).apply()
    }
    
    // ==================== 硬件解码 ====================
    
    /**
     * 获取硬件解码设置
     */
    fun getHardwareDecoder(): Boolean {
        return sharedPreferences.getBoolean("hardware_decoder", true)
    }
    
    /**
     * 保存硬件解码设置
     */
    fun setHardwareDecoder(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("hardware_decoder", enabled).apply()
    }
    
    // ==================== 片头片尾跳过 ====================
    
    /**
     * 获取片头跳过秒数（按文件夹存储）
     */
    fun getSkipIntroSeconds(folderPath: String): Int {
        val key = "skip_intro_folder_${folderPath.hashCode()}"
        return sharedPreferences.getInt(key, 0)
    }
    
    /**
     * 保存片头跳过秒数（按文件夹存储）
     */
    fun setSkipIntroSeconds(folderPath: String, seconds: Int) {
        val key = "skip_intro_folder_${folderPath.hashCode()}"
        sharedPreferences.edit().putInt(key, seconds).apply()
    }
    
    /**
     * 获取片尾跳过秒数（按文件夹存储）
     */
    fun getSkipOutroSeconds(folderPath: String): Int {
        val key = "skip_outro_folder_${folderPath.hashCode()}"
        return sharedPreferences.getInt(key, 0)
    }
    
    /**
     * 保存片尾跳过秒数（按文件夹存储）
     */
    fun setSkipOutroSeconds(folderPath: String, seconds: Int) {
        val key = "skip_outro_folder_${folderPath.hashCode()}"
        sharedPreferences.edit().putInt(key, seconds).apply()
    }
    
    /**
     * 获取是否自动跳过章节（按文件夹存储）
     */
    fun getAutoSkipChapter(folderPath: String): Boolean {
        val key = "auto_skip_chapter_folder_${folderPath.hashCode()}"
        return sharedPreferences.getBoolean(key, false)
    }
    
    /**
     * 保存是否自动跳过章节（按文件夹存储）
     */
    fun setAutoSkipChapter(folderPath: String, enabled: Boolean) {
        val key = "auto_skip_chapter_folder_${folderPath.hashCode()}"
        sharedPreferences.edit().putBoolean(key, enabled).apply()
    }
    
    /**
     * 获取章节跳过的目标章节索引（按文件夹存储）
     * 默认为1（第二个章节，通常OP结束后的正片）
     */
    fun getSkipToChapterIndex(folderPath: String): Int {
        val key = "skip_to_chapter_index_folder_${folderPath.hashCode()}"
        return sharedPreferences.getInt(key, 1)
    }
    
    /**
     * 保存章节跳过的目标章节索引（按文件夹存储）
     */
    fun setSkipToChapterIndex(folderPath: String, chapterIndex: Int) {
        val key = "skip_to_chapter_index_folder_${folderPath.hashCode()}"
        sharedPreferences.edit().putInt(key, chapterIndex).apply()
    }
    
    // ==================== 弹幕文件选择器路径记忆 ====================
    
    /**
     * 获取上次选择弹幕文件的路径
     */
    fun getLastDanmakuPickerPath(): String? {
        return sharedPreferences.getString("last_danmaku_picker_path", null)
    }
    
    /**
     * 保存弹幕文件选择器路径
     */
    fun setLastDanmakuPickerPath(path: String) {
        sharedPreferences.edit().putString("last_danmaku_picker_path", path).apply()
    }
    
    // ==================== 字幕文件选择器路径记忆 ====================
    
    /**
     * 获取上次选择字幕文件的路径
     */
    fun getLastSubtitlePickerPath(): String? {
        return sharedPreferences.getString("last_subtitle_picker_path", null)
    }
    
    /**
     * 保存字幕文件选择器路径
     */
    fun setLastSubtitlePickerPath(path: String) {
        sharedPreferences.edit().putString("last_subtitle_picker_path", path).apply()
    }
    
    // ==================== 视频列表排序设置 ====================
    
    /**
     * 获取视频列表排序方式
     * @return "NAME", "SIZE", "DURATION", "DATE"
     */
    fun getVideoListSortBy(): String {
        return sharedPreferences.getString("video_list_sort_by", "NAME") ?: "NAME"
    }
    
    /**
     * 保存视频列表排序方式
     */
    fun setVideoListSortBy(sortBy: String) {
        sharedPreferences.edit().putString("video_list_sort_by", sortBy).apply()
    }
    
    /**
     * 获取视频列表排序顺序
     * @return "ASCENDING" 或 "DESCENDING"
     */
    fun getVideoListSortOrder(): String {
        return sharedPreferences.getString("video_list_sort_order", "ASCENDING") ?: "ASCENDING"
    }
    
    /**
     * 保存视频列表排序顺序
     */
    fun setVideoListSortOrder(sortOrder: String) {
        sharedPreferences.edit().putString("video_list_sort_order", sortOrder).apply()
    }
    
    // ==================== 字幕字体设置（全局） ====================
    
    /**
     * 获取系统字体名称
     */
    fun getSystemFontName(): String {
        return sharedPreferences.getString("system_font_name", "Noto Sans CJK SC") ?: "Noto Sans CJK SC"
    }
    
    /**
     * 保存系统字体名称
     */
    fun setSystemFontName(fontName: String) {
        sharedPreferences.edit().putString("system_font_name", fontName).apply()
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 清除所有设置（谨慎使用）
     */
    fun getLastRemoteInputUrl(): String {
        return sharedPreferences.getString("remote_input_url", "") ?: ""
    }

    fun setLastRemoteInputUrl(value: String) {
        sharedPreferences.edit().putString("remote_input_url", value).apply()
    }

    fun getLastRemoteInputTitle(): String {
        return sharedPreferences.getString("remote_input_title", "") ?: ""
    }

    fun setLastRemoteInputTitle(value: String) {
        sharedPreferences.edit().putString("remote_input_title", value).apply()
    }

    fun getLastRemoteInputSourcePageUrl(): String {
        return sharedPreferences.getString("remote_input_source_page_url", "") ?: ""
    }

    fun setLastRemoteInputSourcePageUrl(value: String) {
        sharedPreferences.edit().putString("remote_input_source_page_url", value).apply()
    }

    fun getLastRemoteInputReferer(): String {
        return sharedPreferences.getString("remote_input_referer", "") ?: ""
    }

    fun setLastRemoteInputReferer(value: String) {
        sharedPreferences.edit().putString("remote_input_referer", value).apply()
    }

    fun getLastRemoteInputOrigin(): String {
        return sharedPreferences.getString("remote_input_origin", "") ?: ""
    }

    fun setLastRemoteInputOrigin(value: String) {
        sharedPreferences.edit().putString("remote_input_origin", value).apply()
    }

    fun getLastRemoteInputCookie(): String {
        return sharedPreferences.getString("remote_input_cookie", "") ?: ""
    }

    fun setLastRemoteInputCookie(value: String) {
        sharedPreferences.edit().putString("remote_input_cookie", value).apply()
    }

    fun getLastRemoteInputAuthorization(): String {
        return sharedPreferences.getString("remote_input_authorization", "") ?: ""
    }

    fun setLastRemoteInputAuthorization(value: String) {
        sharedPreferences.edit().putString("remote_input_authorization", value).apply()
    }

    fun getLastRemoteInputUserAgent(): String {
        return sharedPreferences.getString("remote_input_user_agent", "") ?: ""
    }

    fun setLastRemoteInputUserAgent(value: String) {
        sharedPreferences.edit().putString("remote_input_user_agent", value).apply()
    }

    fun clearLastRemoteInputDraft() {
        sharedPreferences.edit()
            .remove("remote_input_url")
            .remove("remote_input_title")
            .remove("remote_input_source_page_url")
            .remove("remote_input_referer")
            .remove("remote_input_origin")
            .remove("remote_input_cookie")
            .remove("remote_input_authorization")
            .remove("remote_input_user_agent")
            .apply()
    }

    fun getLastRemoteDebugSummary(): String {
        return sharedPreferences.getString("remote_debug_summary", "") ?: ""
    }

    fun setLastRemoteDebugSummary(value: String) {
        sharedPreferences.edit().putString("remote_debug_summary", value).apply()
    }

    fun clearLastRemoteDebugSummary() {
        sharedPreferences.edit().remove("remote_debug_summary").apply()
    }

    fun getBrowserLastInputUrl(): String {
        return sharedPreferences.getString("browser_last_input_url", "") ?: ""
    }

    fun setBrowserLastInputUrl(value: String) {
        sharedPreferences.edit().putString("browser_last_input_url", value).apply()
    }

    fun getBrowserHomeUrl(): String {
        return sharedPreferences.getString("browser_home_url", "about:blank") ?: "about:blank"
    }

    fun setBrowserHomeUrl(value: String) {
        sharedPreferences.edit().putString("browser_home_url", value).apply()
    }

    fun getBrowserSearchEngineId(): String {
        return sharedPreferences.getString("browser_search_engine", "baidu") ?: "baidu"
    }

    fun setBrowserSearchEngineId(value: String) {
        sharedPreferences.edit().putString("browser_search_engine", value).apply()
    }

    fun isBrowserDesktopModeEnabled(): Boolean {
        return sharedPreferences.getBoolean("browser_desktop_mode", false)
    }

    fun setBrowserDesktopModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("browser_desktop_mode", enabled).apply()
    }

    fun getBrowserUserAgentModeId(): String {
        return sharedPreferences.getString("browser_user_agent_mode", "") ?: ""
    }

    fun setBrowserUserAgentModeId(value: String) {
        sharedPreferences.edit().putString("browser_user_agent_mode", value).apply()
    }

    fun getBrowserCustomGlobalUserAgent(): String {
        return sharedPreferences.getString("browser_custom_global_user_agent", "") ?: ""
    }

    fun setBrowserCustomGlobalUserAgent(value: String) {
        sharedPreferences.edit().putString("browser_custom_global_user_agent", value).apply()
    }

    fun getBrowserCustomSiteUserAgents(): String {
        return sharedPreferences.getString("browser_custom_site_user_agents", "") ?: ""
    }

    fun setBrowserCustomSiteUserAgents(value: String) {
        sharedPreferences.edit().putString("browser_custom_site_user_agents", value).apply()
    }

    fun isBrowserIncognitoModeEnabled(): Boolean {
        return sharedPreferences.getBoolean("browser_incognito_mode", false)
    }

    fun setBrowserIncognitoModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("browser_incognito_mode", enabled).apply()
    }

    fun isBrowserAllowOpenAppEnabled(): Boolean {
        return sharedPreferences.getBoolean("browser_allow_open_app", true)
    }

    fun setBrowserAllowOpenAppEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("browser_allow_open_app", enabled).apply()
    }

    fun isBrowserAllowGeolocationEnabled(): Boolean {
        return sharedPreferences.getBoolean("browser_allow_geolocation", true)
    }

    fun setBrowserAllowGeolocationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("browser_allow_geolocation", enabled).apply()
    }

    fun isBrowserX5Enabled(): Boolean {
        return sharedPreferences.getBoolean("browser_x5_enabled", true)
    }

    fun setBrowserX5Enabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("browser_x5_enabled", enabled).apply()
    }

    fun getDownloadDefaultEngineId(): String {
        return sharedPreferences.getString("download_default_engine", "internal") ?: "internal"
    }

    fun setDownloadDefaultEngineId(value: String) {
        sharedPreferences.edit().putString("download_default_engine", value).apply()
    }

    fun getDownloadMaxConcurrentTasks(): Int {
        return sharedPreferences.getInt("download_max_concurrent_tasks", 3)
    }

    fun setDownloadMaxConcurrentTasks(value: Int) {
        sharedPreferences.edit().putInt("download_max_concurrent_tasks", value).apply()
    }

    fun getDownloadNormalThreadCount(): Int {
        return sharedPreferences.getInt("download_normal_thread_count", 6)
    }

    fun setDownloadNormalThreadCount(value: Int) {
        sharedPreferences.edit().putInt("download_normal_thread_count", value).apply()
    }

    fun getDownloadM3u8ThreadCount(): Int {
        return sharedPreferences.getInt("download_m3u8_thread_count", 16)
    }

    fun setDownloadM3u8ThreadCount(value: Int) {
        sharedPreferences.edit().putInt("download_m3u8_thread_count", value).apply()
    }

    fun isDownloadM3u8AutoMergeEnabled(): Boolean {
        return sharedPreferences.getBoolean("download_m3u8_auto_merge", true)
    }

    fun setDownloadM3u8AutoMergeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_m3u8_auto_merge", enabled).apply()
    }

    fun isDownloadAutoTransferPublicEnabled(): Boolean {
        return sharedPreferences.getBoolean("download_auto_transfer_public", false)
    }

    fun setDownloadAutoTransferPublicEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_auto_transfer_public", enabled).apply()
    }

    fun getDownloadChunkSizeKb(): Int {
        return sharedPreferences.getInt("download_chunk_size_kb", 2048)
    }

    fun setDownloadChunkSizeKb(value: Int) {
        sharedPreferences.edit().putInt("download_chunk_size_kb", value).apply()
    }

    fun isDownloadApkAutoCleanEnabled(): Boolean {
        return sharedPreferences.getBoolean("download_apk_auto_clean", false)
    }

    fun setDownloadApkAutoCleanEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_apk_auto_clean", enabled).apply()
    }

    fun isDownloadSkipConfirmEnabled(): Boolean {
        return sharedPreferences.getBoolean("download_skip_confirm", false)
    }

    fun setDownloadSkipConfirmEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_skip_confirm", enabled).apply()
    }

    fun isDownloadCompletionTipEnabled(): Boolean {
        return sharedPreferences.getBoolean("download_completion_tip", true)
    }

    fun setDownloadCompletionTipEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_completion_tip", enabled).apply()
    }

    fun isDownloadHttp2Enabled(): Boolean {
        return sharedPreferences.getBoolean("download_http2_enabled", true)
    }

    fun setDownloadHttp2Enabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("download_http2_enabled", enabled).apply()
    }

    fun getDownloadCustomDirectoryUri(): String {
        return sharedPreferences.getString("download_custom_directory_uri", "") ?: ""
    }

    fun setDownloadCustomDirectoryUri(value: String) {
        sharedPreferences.edit().putString("download_custom_directory_uri", value).apply()
    }

    fun getDownloadCustomDirectoryName(): String {
        return sharedPreferences.getString("download_custom_directory_name", "") ?: ""
    }

    fun setDownloadCustomDirectoryName(value: String) {
        sharedPreferences.edit().putString("download_custom_directory_name", value).apply()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * 获取所有设置
     */
    fun getAll(): Map<String, *> {
        return sharedPreferences.all ?: emptyMap<String, Any>()
    }
}

