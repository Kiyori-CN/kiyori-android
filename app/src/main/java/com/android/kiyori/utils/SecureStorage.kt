package com.android.kiyori.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SecureStorage keeps the existing public API while routing creation through a shared factory.
 */
object SecureStorage {

    private const val FILE_NAME = "secure_prefs"

    fun getEncryptedPreferences(context: Context): SharedPreferences {
        return EncryptedPreferencesFactory.create(context, FILE_NAME)
    }

    fun putString(context: Context, key: String, value: String?) {
        getEncryptedPreferences(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getEncryptedPreferences(context).getString(key, defaultValue)
    }

    fun remove(context: Context, key: String) {
        getEncryptedPreferences(context).edit().remove(key).apply()
    }

    fun clear(context: Context) {
        getEncryptedPreferences(context).edit().clear().apply()
    }
}
