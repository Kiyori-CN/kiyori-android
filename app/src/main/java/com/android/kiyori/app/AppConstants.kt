package com.android.kiyori.app

/**
 * Centralized app constants.
 */
object AppConstants {

    object Preferences {
        const val VIDEO_PLAYBACK = "VideoPlayback"
        const val PLAYER_PREFS = "player_preferences"
        const val PLAYBACK_HISTORY = "playback_history"

        const val PRECISE_SEEKING = "precise_seeking"
        const val VOLUME_BOOST_ENABLED = "volume_boost_enabled"
        const val SEEK_TIME = "seek_time"
        const val LONG_PRESS_SPEED = "long_press_speed"
        const val ANIME4K_MEMORY_ENABLED = "anime4k_memory_enabled"
        const val ANIME4K_LAST_MODE = "anime4k_last_mode"

        const val DOUBLE_TAP_MODE = "double_tap_mode"
        const val DOUBLE_TAP_SEEK_SECONDS = "double_tap_seek_seconds"

        const val HISTORY_LIST = "history_list"
    }

    object URLs {
        const val GITHUB_URL = "https://github.com/Kiyori-CN/kiyori-android"
        const val CONTACT_URL = "https://github.com/Kiyori-CN"
        const val HOME_PAGE = "https://github.com/Kiyori-CN/kiyori-android"
        const val GITHUB_ISSUES_URL = "https://github.com/Kiyori-CN/kiyori-android/issues"
    }

    object Defaults {
        const val DEFAULT_SEEK_TIME = 5
        const val MIN_SEEK_TIME = 3
        const val MAX_SEEK_TIME = 30

        const val DEFAULT_LONG_PRESS_SPEED = 2.0f
        const val MIN_LONG_PRESS_SPEED = 1.5f
        const val MAX_LONG_PRESS_SPEED = 3.5f

        const val DEFAULT_DOUBLE_TAP_MODE = 0
        const val DEFAULT_DOUBLE_TAP_SEEK_SECONDS = 10
        const val MIN_DOUBLE_TAP_SEEK_SECONDS = 5
        const val MAX_DOUBLE_TAP_SEEK_SECONDS = 30

        const val MAX_HISTORY_SIZE = 50
    }

    object Timings {
        const val CONTROL_AUTO_HIDE_TIME = 5000L
        const val TOAST_DURATION = 2000L
        const val PROGRESS_UPDATE_INTERVAL = 500L
    }

    object Files {
        const val SCREENSHOT_DIR_NAME = "Screenshots"

        val SUPPORTED_VIDEO_EXTENSIONS = arrayOf(
            "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m3u8", "mpd", "ts",
            "3gp", "3g2", "mxf", "ogv", "m2ts", "mts"
        )

        val SUPPORTED_SUBTITLE_EXTENSIONS = arrayOf(
            "srt", "ass", "ssa", "sub", "vtt", "sbv", "json"
        )
    }

    object Logging {
        const val TAG_PREFIX = "Kiyori"
        const val TAG_PLAYBACK = "Playback"
        const val TAG_PLAYER = "Player"
        const val TAG_GESTURE = "Gesture"
        const val TAG_CONTROLS = "Controls"
    }
}
