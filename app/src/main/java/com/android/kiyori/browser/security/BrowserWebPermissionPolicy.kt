package com.android.kiyori.browser.security

import android.Manifest
import com.tencent.smtt.export.external.interfaces.PermissionRequest

internal object BrowserWebPermissionPolicy {

    fun grantedResources(
        requestedResources: Array<String>,
        hasAndroidPermission: (String) -> Boolean
    ): List<String> {
        return requestedResources.distinct().mapNotNull { resource ->
            resource.takeIf {
                requiredAndroidPermissionFor(resource)?.let(hasAndroidPermission) == true
            }
        }
    }

    fun missingAndroidPermissions(
        requestedResources: Array<String>,
        hasAndroidPermission: (String) -> Boolean
    ): List<String> {
        return requestedResources.mapNotNull(::requiredAndroidPermissionFor)
            .distinct()
            .filterNot(hasAndroidPermission)
    }

    private fun requiredAndroidPermissionFor(webResource: String): String? {
        return when (webResource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
            else -> null
        }
    }
}
