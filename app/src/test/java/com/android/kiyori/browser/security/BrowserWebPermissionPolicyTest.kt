package com.android.kiyori.browser.security

import android.Manifest
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserWebPermissionPolicyTest {

    @Test
    fun missingAndroidPermissions_mapsSupportedWebResourcesOnly() {
        val missing = BrowserWebPermissionPolicy.missingAndroidPermissions(
            requestedResources = arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                "unsupported.resource",
                PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
            ),
            hasAndroidPermission = { false }
        )

        assertEquals(
            listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            missing
        )
    }

    @Test
    fun missingAndroidPermissions_excludesAlreadyGrantedPermissions() {
        val missing = BrowserWebPermissionPolicy.missingAndroidPermissions(
            requestedResources = arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
            ),
            hasAndroidPermission = { permission ->
                permission == Manifest.permission.CAMERA
            }
        )

        assertEquals(listOf(Manifest.permission.RECORD_AUDIO), missing)
    }

    @Test
    fun grantedResources_returnsOnlyResourcesWithGrantedAndroidPermission() {
        val grantedResources = BrowserWebPermissionPolicy.grantedResources(
            requestedResources = arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                "unsupported.resource",
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
            ),
            hasAndroidPermission = { permission ->
                permission == Manifest.permission.CAMERA
            }
        )

        assertEquals(listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE), grantedResources)
    }

    @Test
    fun grantedResources_returnsEmptyForUnsupportedResources() {
        val grantedResources = BrowserWebPermissionPolicy.grantedResources(
            requestedResources = arrayOf("unsupported.resource"),
            hasAndroidPermission = { true }
        )

        assertTrue(grantedResources.isEmpty())
    }
}
