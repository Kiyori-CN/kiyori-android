package com.android.kiyori.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class AppPermissionCoordinator(
    private val activity: AppCompatActivity
) {

    private val runtimePermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            requestNextSpecialPermission()
        }

    private val specialPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestNextSpecialPermission()
        }

    private val pendingSpecialPermissions = mutableListOf<SpecialPermissionRequest>()

    fun requestAllPermissionsIfNeeded() {
        val missingRuntimePermissions = collectMissingRuntimePermissions()
        pendingSpecialPermissions.clear()
        pendingSpecialPermissions += collectMissingSpecialPermissions()

        if (missingRuntimePermissions.isNotEmpty()) {
            runtimePermissionLauncher.launch(missingRuntimePermissions.toTypedArray())
        } else {
            requestNextSpecialPermission()
        }
    }

    private fun requestNextSpecialPermission() {
        while (pendingSpecialPermissions.isNotEmpty()) {
            val request = pendingSpecialPermissions.removeAt(0)
            if (request.isGranted(activity)) {
                continue
            }

            val intent = request.intents(activity)
                .firstOrNull { candidate ->
                    candidate.resolveActivity(activity.packageManager) != null
                }
                ?: continue

            specialPermissionLauncher.launch(intent)
            return
        }
    }

    private fun collectMissingRuntimePermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_PHONE_NUMBERS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECEIVE_MMS)
        add(Manifest.permission.RECEIVE_WAP_PUSH)
        add(Manifest.permission.ACTIVITY_RECOGNITION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.filter { permission ->
        ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun collectMissingSpecialPermissions(): List<SpecialPermissionRequest> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            add(
                SpecialPermissionRequest(
                    isGranted = { Environment.isExternalStorageManager() },
                    intents = { context ->
                        listOf(
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ),
                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    }
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(activity)) {
            add(
                SpecialPermissionRequest(
                    isGranted = { context -> Settings.System.canWrite(context) },
                    intents = { context ->
                        listOf(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            add(
                SpecialPermissionRequest(
                    isGranted = { context -> context.packageManager.canRequestPackageInstalls() },
                    intents = { context ->
                        listOf(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
            )
        }
    }

    private data class SpecialPermissionRequest(
        val isGranted: (Context) -> Boolean,
        val intents: (Context) -> List<Intent>
    )
}
