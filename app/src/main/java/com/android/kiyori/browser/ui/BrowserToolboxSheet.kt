package com.android.kiyori.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R

private data class BrowserToolboxItem(
    val title: String,
    val iconRes: Int,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun BrowserToolboxSheet(
    isIncognitoMode: Boolean,
    onAddBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDetectedVideos: () -> Unit,
    onOpenUserAgent: () -> Unit,
    onOpenNetworkLog: () -> Unit,
    onReload: () -> Unit,
    onToggleIncognitoMode: () -> Unit,
    onOpenViewSource: () -> Unit,
    onOpenPlaceholder: (String) -> Unit,
    onCloseBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val rows = listOf(
        listOf(
            BrowserToolboxItem("加书签", R.drawable.ic_kiyori_tool_bookmark_add, onClick = onAddBookmark),
            BrowserToolboxItem("书签", R.drawable.ic_kiyori_tool_bookmarks, onClick = onOpenBookmarks),
            BrowserToolboxItem("历史", R.drawable.ic_kiyori_tool_history, onClick = { onOpenPlaceholder("历史") }),
            BrowserToolboxItem("下载", R.drawable.ic_kiyori_tool_download, onClick = { onOpenPlaceholder("下载") }),
            BrowserToolboxItem("插件", R.drawable.ic_kiyori_tool_plugin, onClick = { onOpenPlaceholder("插件") })
        ),
        listOf(
            BrowserToolboxItem("悬浮嗅探", R.drawable.ic_kiyori_tool_sniffer, onClick = onOpenDetectedVideos),
            BrowserToolboxItem("UA标识", R.drawable.ic_kiyori_tool_ua, onClick = onOpenUserAgent),
            BrowserToolboxItem("网络日志", R.drawable.ic_kiyori_tool_network_log, onClick = onOpenNetworkLog),
            BrowserToolboxItem("刷新", R.drawable.ic_kiyori_tool_refresh, onClick = onReload),
            BrowserToolboxItem("工具箱", R.drawable.ic_kiyori_browser_bottom_toolbox, onClick = { onOpenPlaceholder("工具箱") })
        ),
        listOf(
            BrowserToolboxItem(
                title = if (isIncognitoMode) "关闭无痕" else "开启无痕",
                iconRes = R.drawable.ic_kiyori_tool_incognito,
                onClick = onToggleIncognitoMode
            ),
            BrowserToolboxItem("阅读模式", R.drawable.ic_kiyori_tool_reader_mode, onClick = { onOpenPlaceholder("阅读模式") }),
            BrowserToolboxItem("查看源码", R.drawable.ic_kiyori_tool_view_source, onClick = onOpenViewSource),
            BrowserToolboxItem("标记广告", R.drawable.ic_kiyori_tool_ad_block, onClick = { onOpenPlaceholder("标记广告") }),
            BrowserToolboxItem("网站配置", R.drawable.ic_kiyori_tool_site_config, onClick = { onOpenPlaceholder("网站配置") })
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color.White)
            .navigationBarsPadding()
            .padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                rowItems.forEach { item ->
                    BrowserToolboxGridItem(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 8.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserToolboxBottomIcon(
                iconRes = R.drawable.ic_kiyori_tool_power,
                onClick = onCloseBrowser
            )
            BrowserToolboxBottomIcon(
                iconRes = R.drawable.ic_kiyori_tool_collapse,
                onClick = onDismiss
            )
            BrowserToolboxBottomIcon(
                iconRes = R.drawable.ic_kiyori_tool_settings,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun BrowserToolboxGridItem(
    item: BrowserToolboxItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .clickable(enabled = item.enabled, onClick = item.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.title,
                tint = Color(0xFF000000),
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = item.title,
            color = if (item.enabled) Color(0xFF303030) else Color(0xFFBFC3C8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun BrowserToolboxBottomIcon(
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFF000000),
            modifier = Modifier.size(22.dp)
        )
    }
}
