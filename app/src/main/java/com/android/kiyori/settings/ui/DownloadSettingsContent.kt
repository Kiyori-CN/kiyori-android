package com.android.kiyori.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.android.kiyori.download.DownloadEngineMode
import com.android.kiyori.download.DownloadPreferencesRepository

private val DownloadSettingsCardBorderColor = Color(0xFFE8E8E2)
private val DownloadSheetSurfaceColor = Color.White
private val DownloadSheetPrimaryTextColor = Color(0xFF111111)
private val DownloadSheetSecondaryTextColor = Color(0xFF666666)
private val DownloadSheetDividerColor = Color(0xFFEFEFEF)
private val DownloadSheetScrimColor = Color(0x73000000)

private val DownloadConcurrentTaskOptions = (1..8).toList()
private val DownloadNormalThreadOptions = listOf(3, 6, 8, 12, 16, 20, 32, 48, 64)
private val DownloadM3u8ThreadOptions = listOf(3, 8, 16, 20, 32, 48, 64)
private val DownloadChunkSizeOptionsKb = listOf(12288, 8192, 4096, 2048, 1024, 512, 256)

private sealed interface DownloadSettingRow {
    val title: String

    data class Navigation(
        override val title: String,
        val value: String? = null,
        val onClick: () -> Unit
    ) : DownloadSettingRow

    data class Toggle(
        override val title: String,
        val enabled: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : DownloadSettingRow
}

private data class DownloadSettingSheetItem(
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

private data class DownloadSettingSheetState(
    val title: String,
    val items: List<DownloadSettingSheetItem>,
    val footerText: String? = null
)

@Composable
fun DownloadSettingsContent() {
    val context = LocalContext.current
    val repository = remember { DownloadPreferencesRepository(context) }
    var settings by remember { mutableStateOf(repository.getSettings()) }
    var selectionSheet by remember { mutableStateOf<DownloadSettingSheetState?>(null) }

    fun refreshSettings() {
        settings = repository.getSettings()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val displayName = DocumentFile.fromTreeUri(context, uri)?.name ?: "自定义目录"
            repository.setCustomDirectory(uri.toString(), displayName)
            refreshSettings()
        }
    }

    val groups = remember(settings) {
        listOf(
            listOf(
                DownloadSettingRow.Navigation(
                    title = "自定义下载器",
                    value = settings.defaultEngine.label,
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "默认下载器",
                            items = DownloadEngineMode.entries.map { mode ->
                                DownloadSettingSheetItem(
                                    label = mode.label,
                                    selected = settings.defaultEngine == mode,
                                    onClick = {
                                        repository.setDefaultEngine(mode)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            }
                        )
                    }
                ),
                DownloadSettingRow.Navigation(
                    title = "自定义下载目录",
                    value = settings.customDirectoryName.ifBlank { "应用下载目录" },
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "自定义下载目录",
                            items = buildList {
                                add(
                                    DownloadSettingSheetItem(
                                        label = "选择目录",
                                        onClick = {
                                            selectionSheet = null
                                            folderPickerLauncher.launch(
                                                settings.customDirectoryUri.takeIf { it.isNotBlank() }
                                                    ?.let(Uri::parse)
                                            )
                                        }
                                    )
                                )
                                if (settings.customDirectoryUri.isNotBlank()) {
                                    add(
                                        DownloadSettingSheetItem(
                                            label = "恢复默认目录",
                                            onClick = {
                                                repository.clearCustomDirectory()
                                                refreshSettings()
                                                selectionSheet = null
                                            }
                                        )
                                    )
                                }
                            }
                        )
                    }
                ),
                DownloadSettingRow.Navigation(
                    title = "同时下载任务数",
                    value = settings.maxConcurrentTasks.toString(),
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "同时下载任务数，当前：${settings.maxConcurrentTasks}",
                            items = DownloadConcurrentTaskOptions.map { value ->
                                DownloadSettingSheetItem(
                                    label = value.toString(),
                                    selected = value == settings.maxConcurrentTasks,
                                    onClick = {
                                        repository.setMaxConcurrentTasks(value)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            }
                        )
                    }
                ),
                DownloadSettingRow.Navigation(
                    title = "普通格式下载线程数",
                    value = settings.normalThreadCount.toString(),
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "下载线程数，当前：${settings.normalThreadCount}",
                            items = DownloadNormalThreadOptions.map { value ->
                                DownloadSettingSheetItem(
                                    label = value.toString(),
                                    selected = value == settings.normalThreadCount,
                                    onClick = {
                                        repository.setNormalThreadCount(value)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            }
                        )
                    }
                ),
                DownloadSettingRow.Navigation(
                    title = "M3U8下载线程数",
                    value = settings.m3u8ThreadCount.toString(),
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "下载线程数，当前：${settings.m3u8ThreadCount}",
                            items = DownloadM3u8ThreadOptions.map { value ->
                                DownloadSettingSheetItem(
                                    label = value.toString(),
                                    selected = value == settings.m3u8ThreadCount,
                                    onClick = {
                                        repository.setM3u8ThreadCount(value)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            }
                        )
                    }
                )
            ),
            listOf(
                DownloadSettingRow.Toggle(
                    title = "M3U8自动合并",
                    enabled = settings.autoMergeM3u8,
                    onToggle = {
                        repository.setAutoMergeM3u8(it)
                        refreshSettings()
                    }
                ),
                DownloadSettingRow.Toggle(
                    title = "自动转存公开目录",
                    enabled = settings.autoTransferToPublicDir,
                    onToggle = {
                        repository.setAutoTransferToPublicDir(it)
                        refreshSettings()
                    }
                ),
                DownloadSettingRow.Navigation(
                    title = "自定义下载分块大小",
                    value = formatChunkSize(settings.chunkSizeKb),
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "分块大小，当前：${formatChunkSize(settings.chunkSizeKb)}",
                            items = DownloadChunkSizeOptionsKb.map { value ->
                                DownloadSettingSheetItem(
                                    label = formatChunkSize(value),
                                    selected = value == settings.chunkSizeKb,
                                    onClick = {
                                        repository.setChunkSizeKb(value)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            }
                        )
                    }
                )
            ),
            listOf(
                DownloadSettingRow.Toggle(
                    title = "安装包自动清理",
                    enabled = settings.autoCleanApk,
                    onToggle = {
                        repository.setAutoCleanApk(it)
                        refreshSettings()
                    }
                ),
                DownloadSettingRow.Toggle(
                    title = "下载无需弹窗确认",
                    enabled = settings.skipConfirm,
                    onToggle = {
                        repository.setSkipConfirm(it)
                        refreshSettings()
                    }
                ),
                DownloadSettingRow.Toggle(
                    title = "下载完成强提示",
                    enabled = settings.showCompletionTip,
                    onToggle = {
                        repository.setShowCompletionTip(it)
                        refreshSettings()
                    }
                )
            ),
            listOf(
                DownloadSettingRow.Navigation(
                    title = "切换下载协议",
                    value = if (settings.enableHttp2) "优先 HTTP/2" else "仅 HTTP/1.1",
                    onClick = {
                        selectionSheet = DownloadSettingSheetState(
                            title = "切换下载协议",
                            items = listOf(
                                DownloadSettingSheetItem(
                                    label = "优先 HTTP/2",
                                    selected = settings.enableHttp2,
                                    onClick = {
                                        repository.setEnableHttp2(true)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                ),
                                DownloadSettingSheetItem(
                                    label = "仅 HTTP/1.1",
                                    selected = !settings.enableHttp2,
                                    onClick = {
                                        repository.setEnableHttp2(false)
                                        refreshSettings()
                                        selectionSheet = null
                                    }
                                )
                            )
                        )
                    }
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            Text(
                text = "下载器及自定义",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF202020),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
            )
        }

        itemsIndexed(groups) { _, group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DownloadSettingsCardBorderColor),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.forEachIndexed { index, entry ->
                        DownloadSettingsRow(entry = entry)
                        if (index != group.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0xFFF2F2EE))
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    selectionSheet?.let { sheet ->
        DownloadSettingsSelectionSheet(
            state = sheet,
            onDismiss = { selectionSheet = null }
        )
    }
}

@Composable
private fun DownloadSettingsRow(entry: DownloadSettingRow) {
    val clickableModifier = when (entry) {
        is DownloadSettingRow.Navigation -> Modifier.clickable(onClick = entry.onClick)
        is DownloadSettingRow.Toggle -> Modifier.clickable { entry.onToggle(!entry.enabled) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2B2B2B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (entry) {
            is DownloadSettingRow.Navigation -> {
                if (!entry.value.isNullOrBlank()) {
                    Text(
                        text = entry.value,
                        fontSize = 13.sp,
                        color = Color(0xFF9A9895),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBDBDB8),
                    modifier = Modifier.size(18.dp)
                )
            }

            is DownloadSettingRow.Toggle -> {
                DownloadSettingsIndicator(enabled = entry.enabled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSettingsSelectionSheet(
    state: DownloadSettingSheetState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = DownloadSheetSurfaceColor,
        scrimColor = DownloadSheetScrimColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = state.title,
                color = DownloadSheetPrimaryTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
            Divider(color = DownloadSheetDividerColor)

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                state.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            color = DownloadSheetPrimaryTextColor,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = DownloadSheetPrimaryTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Divider(color = DownloadSheetDividerColor)
                }

                state.footerText?.let { footerText ->
                    Text(
                        text = footerText,
                        color = DownloadSheetSecondaryTextColor,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 13.dp)
                    )
                    Divider(color = DownloadSheetDividerColor)
                }
            }

            Text(
                text = "取消",
                color = DownloadSheetPrimaryTextColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun DownloadSettingsIndicator(enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(17.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (enabled) Color(0xFF111111) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun formatChunkSize(valueKb: Int): String {
    return when {
        valueKb >= 1024 && valueKb % 1024 == 0 -> "${valueKb / 1024}MB"
        else -> "${valueKb}KB"
    }
}
