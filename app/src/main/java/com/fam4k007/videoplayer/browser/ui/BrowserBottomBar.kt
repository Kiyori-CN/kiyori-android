package com.fam4k007.videoplayer.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fam4k007.videoplayer.browser.domain.BrowserPageState

@Composable
fun BrowserBottomBar(
    state: BrowserPageState,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onShowHistory: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenExternalBrowser: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onClearCookies: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BrowserBottomBarIcon(
                enabled = state.canGoBack,
                onClick = onGoBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null
                )
            }
            BrowserBottomBarIcon(
                enabled = state.canGoForward,
                onClick = onGoForward
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null
                )
            }
            BrowserBottomBarIcon(
                enabled = true,
                onClick = onGoHome
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
            }
            BrowserBottomBarIcon(
                enabled = state.historyEntries.isNotEmpty(),
                onClick = onShowHistory
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null
                )
            }
            Box {
                BrowserBottomBarIcon(
                    enabled = true,
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (state.isDesktopMode) "关闭桌面版 UA" else "开启桌面版 UA") },
                        onClick = {
                            showMenu = false
                            onToggleDesktopMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("外部浏览器打开") },
                        onClick = {
                            showMenu = false
                            onOpenExternalBrowser()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("复制当前链接") },
                        onClick = {
                            showMenu = false
                            onCopyCurrentUrl()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("清除当前站点 Cookie") },
                        onClick = {
                            showMenu = false
                            onClearCookies()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserBottomBarIcon(
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            }
        ) {
            icon()
        }
    }
}
