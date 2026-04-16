package com.fam4k007.videoplayer.browser.domain

sealed interface BrowserCommand {
    data class LoadUrl(val value: String) : BrowserCommand
    data object LoadHome : BrowserCommand
    data object GoBack : BrowserCommand
    data object GoForward : BrowserCommand
    data object Reload : BrowserCommand
    data object StopLoading : BrowserCommand
    data object ToggleDesktopMode : BrowserCommand
}
