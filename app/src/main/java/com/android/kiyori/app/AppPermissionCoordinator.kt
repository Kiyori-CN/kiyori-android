package com.android.kiyori.app

import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class AppPermissionCoordinator(
    private val activity: AppCompatActivity
) {

    private val runtimePermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    fun requestAllPermissionsIfNeeded() {
        val missingRuntimePermissions = collectMissingRuntimePermissions()
        if (missingRuntimePermissions.isNotEmpty()) {
            runtimePermissionLauncher.launch(missingRuntimePermissions.toTypedArray())
        }
    }

    private fun collectMissingRuntimePermissions(): List<String> = buildList {
        // Only request permissions needed for core media/file flows at startup.
        // WebView camera, microphone, and geolocation prompts request runtime permissions on demand.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.filter { permission ->
        ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
    }
}
