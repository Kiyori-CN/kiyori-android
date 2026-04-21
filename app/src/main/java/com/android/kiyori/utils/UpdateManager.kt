package com.android.kiyori.utils

import android.content.Context

/**
 * 共存版不再接受上游更新，保留接口仅用于兼容旧调用。
 */
object UpdateManager {

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val releaseNotes: String,
        val publishedAt: String
    )

    suspend fun checkForUpdate(context: Context): UpdateInfo? = null

    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "未知"
        }
    }

    fun openDownloadPage(context: Context, downloadUrl: String) = Unit
}

