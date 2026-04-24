@file:Suppress("DEPRECATION")

package com.android.kiyori.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Centralizes encrypted SharedPreferences creation so future storage changes stay in one place.
 */
object EncryptedPreferencesFactory {

    private const val TAG = "EncryptedPrefsFactory"

    @Suppress("DEPRECATION")
    fun create(
        context: Context,
        fileName: String,
        fallbackToPlaintext: Boolean = true
    ): SharedPreferences {
        val appContext = context.applicationContext

        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            if (!fallbackToPlaintext) {
                throw e
            }

            Log.e(TAG, "Failed to create encrypted preferences, using normal storage: $fileName", e)
            appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        }
    }
}
