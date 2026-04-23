package com.android.kiyori.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import com.android.kiyori.browser.domain.BrowserPageState

private val BottomBarIconColor = Color(0xFF000000)
private val BottomBarIconDisabledColor = Color(0xFF9CA3AF)
private val BrowserBottomBarIconSize = 26.dp
private val BrowserBottomBarItemSize = 44.dp

@Composable
fun BrowserBottomBar(
    state: BrowserPageState,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onShowHistory: () -> Unit,
    onOpenToolbox: () -> Unit
) {
    val windowCount = state.historyEntries.size + if (state.historyEntries.any { it.url == state.currentUrl }) 0 else 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = state.canGoBack,
                    contentDescription = "返回上一页",
                    iconRes = R.drawable.ic_kiyori_browser_bottom_back,
                    onClick = onGoBack
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = state.canGoForward,
                    contentDescription = "前进下一页",
                    iconRes = R.drawable.ic_kiyori_browser_bottom_forward,
                    onClick = onGoForward
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = true,
                    contentDescription = "浏览器主页",
                    iconRes = R.drawable.ic_kiyori_browser_bottom_home,
                    onClick = onGoHome
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarHistoryIcon(
                    count = windowCount,
                    contentDescription = "浏览器窗口管理",
                    onClick = onShowHistory
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = true,
                    contentDescription = "浏览器工具箱",
                    iconRes = R.drawable.ic_kiyori_tool_toolbox,
                    onClick = onOpenToolbox
                )
            }
        }
    }
}

@Composable
private fun BrowserBottomBarIcon(
    enabled: Boolean,
    contentDescription: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(BrowserBottomBarItemSize)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) BottomBarIconColor else BottomBarIconDisabledColor,
            modifier = Modifier.size(BrowserBottomBarIconSize)
        )
    }
}

@Composable
private fun BrowserBottomBarHistoryIcon(
    count: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val displayCount = when {
        count > 99 -> "99+"
        count < 0 -> "0"
        else -> count.toString()
    }

    Box(
        modifier = Modifier
            .size(BrowserBottomBarItemSize)
            .clip(CircleShape)
            .background(Color.Transparent)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(1.75.dp))
                .border(1.75.dp, BottomBarIconColor, RoundedCornerShape(1.75.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayCount,
                modifier = Modifier.align(Alignment.Center),
                color = BottomBarIconColor,
                fontSize = if (displayCount.length > 2) 7.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (displayCount.length > 2) 7.sp else 9.sp
            )
        }
    }
}
