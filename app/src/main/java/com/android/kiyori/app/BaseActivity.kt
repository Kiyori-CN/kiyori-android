package com.android.kiyori.app

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.android.kiyori.R
import com.android.kiyori.utils.ThemeManager
import com.android.kiyori.utils.applyCloseActivityTransitionCompat
import com.android.kiyori.utils.applyOpenActivityTransitionCompat

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        registerDefaultBackNavigation()
    }

    protected fun setupToolbar(
        toolbarId: Int,
        title: String,
        showBackButton: Boolean = true
    ) {
        try {
            val toolbar = findViewById<Toolbar>(toolbarId)
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                this.title = title
                if (showBackButton) {
                    setDisplayHomeAsUpEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_toolbar_back_tinted)
                }
            }
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }

    protected fun startActivityWithTransition(enterAnim: Int, exitAnim: Int) {
        applyOpenActivityTransitionCompat(enterAnim, exitAnim)
    }

    protected fun startActivityWithDefaultTransition() {
        applyOpenActivityTransitionCompat(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    protected fun setOnClickListener(viewId: Int, onClick: (View) -> Unit) {
        findViewById<View>(viewId)?.setOnClickListener(onClick)
    }

    private fun registerDefaultBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
                applyCloseActivityTransitionCompat(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })
    }
}
