package com.android.kiyori.webdav

import android.content.Context
import android.content.SharedPreferences
import com.android.kiyori.utils.EncryptedPreferencesFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class WebDavAccount(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val serverUrl: String,
    val account: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
)

class WebDavAccountManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        EncryptedPreferencesFactory.create(context, PREFS_NAME)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "webdav_accounts"
        private const val KEY_ACCOUNTS = "accounts"

        @Volatile
        private var instance: WebDavAccountManager? = null

        fun getInstance(context: Context): WebDavAccountManager {
            return instance ?: synchronized(this) {
                instance ?: WebDavAccountManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun getAllAccounts(): List<WebDavAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WebDavAccount>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addAccount(account: WebDavAccount): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            if (accounts.any { it.serverUrl == account.serverUrl && it.account == account.account }) {
                return false
            }

            accounts.add(account)
            saveAccounts(accounts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAccount(accountId: String): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            accounts.removeAll { it.id == accountId }
            saveAccounts(accounts)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updateAccount(account: WebDavAccount): Boolean {
        return try {
            val accounts = getAllAccounts().toMutableList()
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) {
                accounts[index] = account
                saveAccounts(accounts)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAccountById(accountId: String): WebDavAccount? {
        return getAllAccounts().find { it.id == accountId }
    }

    fun clearAllAccounts() {
        prefs.edit().clear().apply()
    }

    private fun saveAccounts(accounts: List<WebDavAccount>) {
        val json = gson.toJson(accounts)
        prefs.edit().putString(KEY_ACCOUNTS, json).apply()
    }
}
