package com.android.kiyori.browser.domain

enum class BrowserUserAgentMode(
    val id: String,
    val displayName: String,
    val isImplemented: Boolean = true
) {
    ANDROID("android", "Android"),
    PC_DESKTOP("pc_desktop", "PC桌面"),
    IPHONE("iphone", "iPhone"),
    SYMBIAN_WAP("symbian_wap", "塞班Wap"),
    CUSTOM_GLOBAL("custom_global", "自定义全局"),
    CUSTOM_SITE("custom_site", "自定义当前网站");

    companion object {
        val selectableEntries: List<BrowserUserAgentMode> = listOf(
            ANDROID,
            PC_DESKTOP,
            IPHONE,
            SYMBIAN_WAP,
            CUSTOM_GLOBAL,
            CUSTOM_SITE
        )

        fun fromId(id: String): BrowserUserAgentMode {
            return entries.firstOrNull { it.id == id } ?: ANDROID
        }
    }
}
