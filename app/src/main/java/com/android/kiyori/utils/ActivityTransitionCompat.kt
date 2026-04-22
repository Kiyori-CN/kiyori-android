package com.android.kiyori.utils

import android.app.Activity
import android.os.Build

fun Activity.applyOpenActivityTransitionCompat(enterAnim: Int, exitAnim: Int) {
    applyActivityTransitionCompat(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
}

fun Activity.applyCloseActivityTransitionCompat(enterAnim: Int, exitAnim: Int) {
    applyActivityTransitionCompat(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
}

private fun Activity.applyActivityTransitionCompat(
    transitionType: Int,
    enterAnim: Int,
    exitAnim: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(transitionType, enterAnim, exitAnim)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(enterAnim, exitAnim)
    }
}
