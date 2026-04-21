package com.android.kiyori.settings.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.browser.x5.BrowserX5KernelManager

@Composable
fun BrowserX5DebugPage() {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val kernelState = remember(refreshTick) {
        BrowserX5KernelManager.getState(context)
    }
    var uploadEnabled by remember(refreshTick) {
        mutableStateOf(BrowserX5KernelManager.isNetLogEnabled())
    }

    val items = listOf(
        X5DebugAction("清除TBS内核", Icons.Outlined.DeleteOutline) {
            BrowserX5KernelManager.reset(context)
            refreshTick++
            toast(context, "已重置 TBS 内核状态")
        },
        X5DebugAction("查看版本信息", Icons.Outlined.Info) {
            copyToClipboard(
                context = context,
                label = "x5_info",
                text = BrowserX5KernelManager.buildDiagnosticReport(context)
            )
            refreshTick++
            toast(context, "已复制 X5 内核信息")
        },
        X5DebugAction("安装线上内核", Icons.Outlined.Download) {
            BrowserX5KernelManager.refresh(context)
            refreshTick++
            toast(context, "已发起 X5 预初始化")
        },
        X5DebugAction("上传tbslog", Icons.Outlined.Upload) {
            BrowserX5KernelManager.uploadNetLog()
            toast(context, "已请求上传 X5 网络日志")
        },
        X5DebugAction("安装本地内核", Icons.Rounded.ArrowUpward) {
            toast(context, "本地内核安装入口暂时保留")
        },
        X5DebugAction("清除DebugTbs", Icons.Outlined.DeleteOutline) {
            BrowserX5KernelManager.clearDebugArtifacts(context)
            refreshTick++
            toast(context, "已清理 X5 调试残留与缓存")
        },
        X5DebugAction("清除Crash禁用标记", Icons.Outlined.DeleteOutline) {
            BrowserX5KernelManager.clearCrashDisableMark(context)
            refreshTick++
            toast(context, "已清理 X5 Crash 禁用标记")
        },
        X5DebugAction("发送日志", Icons.Outlined.Send) {
            val exportedPath = BrowserX5KernelManager.exportNetLog()
            if (exportedPath.isNotBlank()) {
                copyToClipboard(context, "x5_log_path", exportedPath)
                toast(context, "已导出 X5 网络日志，路径已复制")
            } else {
                toast(context, "当前没有可导出的 X5 网络日志")
            }
        },
        X5DebugAction("清除本地缓存", Icons.Outlined.CleaningServices) {
            BrowserX5KernelManager.clearAllCache(context)
            toast(context, "已清除 WebView/TBS 缓存")
        },
        X5DebugAction("清除本地安装标记", Icons.Outlined.DeleteOutline) {
            BrowserX5KernelManager.clearLocalInstallMark(context)
            refreshTick++
            toast(context, "已清理 X5 本地安装标记")
        },
        X5DebugAction(
            title = if (kernelState.isEnabled) "内核未被禁用" else "内核已被禁用",
            icon = if (kernelState.isEnabled) Icons.Rounded.ToggleOn else Icons.Rounded.ToggleOff
        ) {
            BrowserX5KernelManager.applyKernelSwitch(context, !kernelState.isEnabled)
            refreshTick++
            toast(context, if (!kernelState.isEnabled) "已启用 X5 开关" else "已切回系统内核开关")
        },
        X5DebugAction("DebugX5", Icons.Outlined.WebAsset) {
            refreshTick++
            toast(
                context,
                if (kernelState.canLoadX5) "当前可加载 X5 内核" else "当前仍会回退系统内核"
            )
        },
        X5DebugAction("拷贝内核", Icons.Outlined.ContentCopy) {
            copyToClipboard(
                context = context,
                label = "x5_core",
                text = "SDK ${kernelState.sdkVersion} / Core ${kernelState.coreVersion}"
            )
            toast(context, "已复制 X5 内核版本")
        },
        X5DebugAction("合作方加载检测", Icons.Outlined.SettingsApplications) {
            refreshTick++
            toast(
                context,
                if (kernelState.isCoreInited) {
                    "TBS Core 已初始化，进程：${kernelState.currentProcessName}"
                } else {
                    "TBS Core 尚未初始化完成"
                }
            )
        },
        X5DebugAction(
            title = if (uploadEnabled) "上传开关" else "上传已关",
            icon = if (uploadEnabled) Icons.Outlined.UploadFile else Icons.Outlined.Inventory2
        ) {
            val nextEnabled = !uploadEnabled
            BrowserX5KernelManager.setNetLogEnabled(nextEnabled)
            uploadEnabled = nextEnabled
            toast(
                context,
                if (nextEnabled) "已开启 X5 网络日志捕获" else "已关闭 X5 网络日志捕获"
            )
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F2)),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "X5 内核状态",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2B2B)
                    )
                    Text(
                        text = "SDK ${kernelState.sdkVersion}  Core ${kernelState.coreVersion}",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7B76)
                    )
                    Text(
                        text = "可加载X5：${if (kernelState.canLoadX5) "是" else "否"}  已初始化：${if (kernelState.isCoreInited) "是" else "否"}",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7B76)
                    )
                    Text(
                        text = "禁用版本：${kernelState.disabledCoreVersion}  预检禁用：${kernelState.preloadDisableVersion}",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7B76)
                    )
                    Text(
                        text = "安装中：${if (kernelState.isInstalling) "是" else "否"}  安装中断码：${kernelState.installInterruptCode}",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7B76)
                    )
                    Text(
                        text = "下载进度：${if (kernelState.lastDownloadProgress >= 0) "${kernelState.lastDownloadProgress}%" else "未开始"}",
                        fontSize = 13.sp,
                        color = Color(0xFF7D7B76)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp),
                    userScrollEnabled = false
                ) {
                    items(items) { item ->
                        X5DebugCell(action = item)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun X5DebugCell(
    action: X5DebugAction
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = action.onClick)
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF8F4F5), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = Color(0xFF5A5A58),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = action.title,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = Color(0xFF2B2B2B),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class X5DebugAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun copyToClipboard(
    context: Context,
    label: String,
    text: String
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
}
