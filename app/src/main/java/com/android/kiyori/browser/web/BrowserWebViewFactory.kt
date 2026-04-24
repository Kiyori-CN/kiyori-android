package com.android.kiyori.browser.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.graphics.Color
import android.view.View
import com.android.kiyori.BuildConfig
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.tencent.smtt.sdk.CookieManager
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView

object BrowserWebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    fun configure(webView: WebView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.setBackgroundColor(Color.WHITE)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            setJavaScriptEnabled(true)
            setJavaScriptCanOpenWindowsAutomatically(true)
            setDomStorageEnabled(true)
            setDatabaseEnabled(true)
            setGeolocationEnabled(true)
            setUseWideViewPort(true)
            setLoadWithOverviewMode(true)
            setSupportZoom(true)
            setBuiltInZoomControls(true)
            setDisplayZoomControls(false)
            setTextZoom(100)
            setLoadsImagesAutomatically(true)
            setBlockNetworkImage(false)
            setBlockNetworkLoads(false)
            setAllowFileAccess(false)
            setAllowContentAccess(true)
            setDefaultTextEncodingName("utf-8")
            setCacheMode(WebSettings.LOAD_DEFAULT)
            setMediaPlaybackRequiresUserGesture(false)
            setSupportMultipleWindows(true)
            setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
            setSafeBrowsingEnabled(true)
            setAllowFileAccessFromFileURLs(false)
            setAllowUniversalAccessFromFileURLs(false)
        }

        runCatching {
            val videoGestureBundle = Bundle().apply {
                putBoolean("require", false)
            }
            val videoParamsBundle = Bundle().apply {
                putInt("DefaultVideoScreen", 1)
                putBoolean("supportLiteWnd", true)
            }
            webView.x5WebViewExtension?.invokeMiscMethod(
                "setVideoPlaybackRequiresUserGesture",
                videoGestureBundle
            )
            webView.x5WebViewExtension?.invokeMiscMethod(
                "setVideoParams",
                videoParamsBundle
            )
        }
    }

    fun applyIdentity(
        webView: WebView,
        profile: BrowserSecurityPolicy.UserAgentProfile
    ) {
        val settings = webView.settings
        if (profile.userAgent.isNotBlank() && settings.userAgentString != profile.userAgent) {
            settings.userAgentString = profile.userAgent
        }

        if (!profile.mobile) {
            settings.setUseWideViewPort(true)
            settings.setLoadWithOverviewMode(true)
        }
    }
}
