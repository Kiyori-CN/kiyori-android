package com.android.kiyori.utils

import android.content.Context
import com.android.kiyori.bilibili.auth.BiliBiliAuthManager

object CookieManager {
    fun getBilibiliCookie(context: Context): String {
        return BiliBiliAuthManager.getInstance(context).getCookieString()
    }
}
