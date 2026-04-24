package com.android.kiyori.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.android.kiyori.app.MainActivity
import com.android.kiyori.settings.ui.UserAgreementScreen

class UserAgreementActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "user_agreement_prefs"
        private const val KEY_AGREED = "user_agreed"

        fun isAgreed(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AGREED, false)
        }

        fun setAgreed(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AGREED, true).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            // Block system back until the user explicitly agrees or declines.
        }

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    UserAgreementScreen(
                        onAgree = {
                            setAgreed(this)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onDecline = {
                            finishAffinity()
                        }
                    )
                }
            }
        }
    }
}
