package com.android.kiyori.history.ui

import android.os.Bundle
import com.android.kiyori.app.BaseActivity

class PlaybackHistoryComposeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HistoryComposeActivity.start(
            this,
            initialSection = HistorySection.PLAYBACK
        )
        startActivityWithDefaultTransition()
        finish()
    }
}
