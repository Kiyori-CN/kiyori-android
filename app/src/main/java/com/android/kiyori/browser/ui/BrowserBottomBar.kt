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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import com.android.kiyori.browser.domain.BrowserPageState

private val BottomBarIconColor = Color(0xFF000000)
private val BottomBarIconDisabledColor = Color(0xFF9CA3AF)

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
                .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = state.canGoBack,
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
                    onClick = onShowHistory
                )
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BrowserBottomBarIcon(
                    enabled = true,
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
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (enabled) BottomBarIconColor else BottomBarIconDisabledColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BrowserBottomBarHistoryIcon(
    count: Int,
    onClick: () -> Unit
) {
    val displayCount = when {
        count > 99 -> "99+"
        count < 0 -> "0"
        else -> count.toString()
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(1.75.dp))
                .border(1.7.dp, BottomBarIconColor, RoundedCornerShape(1.75.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayCount,
                color = BottomBarIconColor,
                fontSize = if (displayCount.length > 2) 7.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (displayCount.length > 2) 7.sp else 9.sp
            )
        }
    }
}
