package com.android.kiyori.browser.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.android.kiyori.app.BaseActivity
import com.android.kiyori.browser.security.BrowserWebPermissionPolicy
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.PermissionRequest

internal class BrowserPermissionCoordinator(
    private val activity: BaseActivity,
    private val isWebPageGeolocationEnabled: () -> Boolean
) {
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissionsCallback? = null
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var pendingWebPermissionResources: Array<String> = emptyArray()

    private val geolocationPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            completePendingGeolocationRequest(granted = hasLocationPermission())
        }

    private val webPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            completePendingWebPermissionRequest(grantAvailableResources = true)
        }

    fun handleGeolocationPermissionRequest(
        origin: String?,
        callback: GeolocationPermissionsCallback?
    ) {
        callback ?: return
        if (!isWebPageGeolocationEnabled()) {
            callback.invoke(origin, false, false)
            return
        }
        if (hasLocationPermission()) {
            callback.invoke(origin, true, false)
            return
        }

        completePendingGeolocationRequest(granted = false)
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        geolocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun handleWebPermissionRequest(
        request: PermissionRequest,
        resources: Array<String>
    ) {
        val missingPermissions = BrowserWebPermissionPolicy.missingAndroidPermissions(
            requestedResources = resources,
            hasAndroidPermission = ::hasRuntimePermission
        )
        if (missingPermissions.isEmpty()) {
            grantOrDenyWebPermissionRequest(request, collectGrantedWebPermissionResources(resources))
            return
        }

        completePendingWebPermissionRequest(grantAvailableResources = false)
        pendingWebPermissionRequest = request
        pendingWebPermissionResources = resources
        webPermissionLauncher.launch(missingPermissions.toTypedArray())
    }

    fun cancelPendingRequests() {
        completePendingGeolocationRequest(granted = false)
        completePendingWebPermissionRequest(grantAvailableResources = false)
    }

    private fun completePendingGeolocationRequest(granted: Boolean) {
        val callback = pendingGeolocationCallback ?: run {
            pendingGeolocationOrigin = null
            return
        }
        callback.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    private fun completePendingWebPermissionRequest(grantAvailableResources: Boolean) {
        val request = pendingWebPermissionRequest ?: run {
            pendingWebPermissionResources = emptyArray()
            return
        }
        val grantedResources = if (grantAvailableResources) {
            collectGrantedWebPermissionResources(pendingWebPermissionResources)
        } else {
            emptyList()
        }
        grantOrDenyWebPermissionRequest(request, grantedResources)
        pendingWebPermissionRequest = null
        pendingWebPermissionResources = emptyArray()
    }

    private fun grantOrDenyWebPermissionRequest(
        request: PermissionRequest,
        grantedResources: List<String>
    ) {
        if (grantedResources.isEmpty()) {
            request.deny()
        } else {
            request.grant(grantedResources.toTypedArray())
        }
    }

    private fun collectGrantedWebPermissionResources(resources: Array<String>): List<String> {
        return BrowserWebPermissionPolicy.grantedResources(
            requestedResources = resources,
            hasAndroidPermission = ::hasRuntimePermission
        )
    }

    private fun hasLocationPermission(): Boolean {
        return hasRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasRuntimePermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}
