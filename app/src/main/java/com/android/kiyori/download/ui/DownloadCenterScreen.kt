package com.android.kiyori.download.ui

import android.app.DownloadManager as AndroidDownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.documentfile.provider.DocumentFile
import com.android.kiyori.download.DownloadEngineMode
import com.android.kiyori.download.InternalDownloadEntity
import com.android.kiyori.download.DownloadPreferencesRepository
import com.android.kiyori.download.InternalDownloadRequest
import com.android.kiyori.download.InternalDownloadStatus
import com.android.kiyori.download.InternalDownloadViewModel
import com.android.kiyori.ui.compose.KiyoriBottomDrawer
import com.android.kiyori.ui.compose.LocalKiyoriDrawerDragModifier

private enum class DownloadPageTab(val label: String) {
    Downloaded("已下载"),
    Downloading("下载中")
}

private enum class DownloadSortMode(val label: String) {
    Newest("最新优先"),
    Oldest("最早优先"),
    Name("按名称")
}

private enum class DownloadTargetMode(val label: String) {
    Internal("内置下载器"),
    External("其它下载器")
}

private enum class RenameDialogMode {
    Rename,
    Suffix
}

private data class DownloadSection(
    val title: String,
    val records: List<InternalDownloadEntity>
)

private data class DownloadActionButton(
    val title: String,
    val onClick: () -> Unit
)

@Composable
fun DownloadCenterScreen(
    onDismissRequest: () -> Unit,
    onOpenDownloadSettings: () -> Unit,
    viewModel: InternalDownloadViewModel = viewModel()
) {
    KiyoriBottomDrawer(
        onDismissRequest = onDismissRequest
    ) {
        DownloadCenterSheetContent(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest,
            onOpenDownloadSettings = onOpenDownloadSettings
        )
    }
}

@Composable
private fun DownloadCenterSheetContent(
    viewModel: InternalDownloadViewModel,
    onDismissRequest: () -> Unit,
    onOpenDownloadSettings: () -> Unit
) {
    val drawerDragModifier = LocalKiyoriDrawerDragModifier.current
    val context = LocalContext.current
    val records by viewModel.records.collectAsState()
    var selectedTab by remember { mutableStateOf(DownloadPageTab.Downloaded) }
    var sortMode by remember { mutableStateOf(DownloadSortMode.Newest) }
    var batchMode by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var classifyRecords by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var downloadingActionRecord by remember { mutableStateOf<InternalDownloadEntity?>(null) }
    var downloadedActionRecord by remember { mutableStateOf<InternalDownloadEntity?>(null) }
    var pendingDirectoryRecord by remember { mutableStateOf<InternalDownloadEntity?>(null) }
    var renameDialogRecord by remember { mutableStateOf<InternalDownloadEntity?>(null) }
    var renameDialogMode by remember { mutableStateOf(RenameDialogMode.Rename) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val targetRecord = pendingDirectoryRecord
        pendingDirectoryRecord = null
        uri ?: return@rememberLauncherForActivityResult
        if (targetRecord == null) {
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        val folderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "目标目录"
        viewModel.moveToDirectory(targetRecord, uri.toString()) { result ->
            result.onSuccess {
                Toast.makeText(context, "已移动到$folderName", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, it.message ?: "修改文件夹失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val visibleRecords = remember(records, selectedTab, sortMode) {
        val filtered = when (selectedTab) {
            DownloadPageTab.Downloaded -> records.filter { it.status == InternalDownloadStatus.SUCCESS }
            DownloadPageTab.Downloading -> records.filter { it.status != InternalDownloadStatus.SUCCESS }
        }
        when (sortMode) {
            DownloadSortMode.Newest -> filtered.sortedByDescending { it.createdAt }
            DownloadSortMode.Oldest -> filtered.sortedBy { it.createdAt }
            DownloadSortMode.Name -> filtered.sortedBy { it.title.lowercase() }
        }
    }

    val sections = remember(visibleRecords, classifyRecords) {
        if (!classifyRecords) {
            listOf(DownloadSection("", visibleRecords))
        } else {
            visibleRecords
                .groupBy(::resolveDownloadCategory)
                .map { (title, items) -> DownloadSection(title, items) }
                .sortedBy { it.title }
        }
    }

    fun clearSelection() {
        selectedIds.clear()
        batchMode = false
    }

    fun toggleSelection(record: InternalDownloadEntity) {
        if (record.id in selectedIds) {
            selectedIds.remove(record.id)
        } else {
            selectedIds.add(record.id)
        }
    }

    fun enableBatchMode(record: InternalDownloadEntity, tab: DownloadPageTab) {
        selectedTab = tab
        batchMode = true
        if (record.id !in selectedIds) {
            selectedIds.add(record.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 0.dp, top = 0.dp, end = 12.dp, bottom = 0.dp)
                .then(drawerDragModifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.width(0.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "我的下载",
                    color = Color(0xFF111827),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.offset(x = (-4).dp)
                )
                Box {
                    DownloadMenuTrigger(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 1.dp)
                            .clickable { menuExpanded = true }
                    )
                    if (menuExpanded) {
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(34, 44),
                            onDismissRequest = { menuExpanded = false },
                            properties = PopupProperties(focusable = true)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.width(126.dp)
                                ) {
                                    DownloadMenuItem("排序方式") {
                                        menuExpanded = false
                                        showSortDialog = true
                                    }
                                    DownloadMenuItem(if (batchMode) "退出批量删除" else "批量删除") {
                                        menuExpanded = false
                                        if (batchMode) {
                                            clearSelection()
                                        } else {
                                            batchMode = true
                                        }
                                    }
                                    DownloadMenuItem("文件管理") {
                                        menuExpanded = false
                                        runCatching {
                                            context.startActivity(Intent(AndroidDownloadManager.ACTION_VIEW_DOWNLOADS))
                                        }.onFailure {
                                            Toast.makeText(context, "暂时无法打开系统下载页", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    DownloadMenuItem(if (showTime) "隐藏时间" else "显示时间") {
                                        menuExpanded = false
                                        showTime = !showTime
                                    }
                                    DownloadMenuItem(if (classifyRecords) "关闭分类显示" else "分类显示") {
                                        menuExpanded = false
                                        classifyRecords = !classifyRecords
                                    }
                                    DownloadMenuItem("更多设置", drawDivider = false) {
                                        menuExpanded = false
                                        onOpenDownloadSettings()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DownloadOutlinedActionButton(
                title = "新增",
                onClick = { showAddDialog = true }
            )
            Spacer(modifier = Modifier.width(10.dp))
            DownloadOutlinedActionButton(
                title = if (batchMode) "删除" else "清空",
                enabled = if (batchMode) selectedIds.isNotEmpty() else visibleRecords.isNotEmpty(),
                onClick = {
                    if (batchMode) {
                        visibleRecords
                            .filter { it.id in selectedIds }
                            .forEach { viewModel.remove(it, deleteFile = it.status == InternalDownloadStatus.SUCCESS) }
                        clearSelection()
                    } else {
                        visibleRecords.forEach {
                            viewModel.remove(it, deleteFile = it.status == InternalDownloadStatus.SUCCESS)
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .background(Color.White),
            verticalAlignment = Alignment.Bottom
        ) {
            DownloadTab(
                label = DownloadPageTab.Downloaded.label,
                selected = selectedTab == DownloadPageTab.Downloaded,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = DownloadPageTab.Downloaded
                    clearSelection()
                }
            )
            DownloadTab(
                label = DownloadPageTab.Downloading.label,
                selected = selectedTab == DownloadPageTab.Downloading,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = DownloadPageTab.Downloading
                    clearSelection()
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFF5FA))
        ) {
            if (visibleRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == DownloadPageTab.Downloaded) {
                            "还没有已下载内容"
                        } else {
                            "当前没有下载任务"
                        },
                        color = Color(0xFF9099A3),
                        fontSize = 13.sp
                    )
                }
            } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    sections.forEach { section ->
                        if (section.title.isNotBlank()) {
                            item(key = "header_${section.title}") {
                                Text(
                                    text = section.title,
                                    color = Color(0xFF6C7278),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }
                        }
                        items(section.records, key = { it.id }) { record ->
                            DownloadRecordCard(
                                record = record,
                                batchMode = batchMode,
                                selected = record.id in selectedIds,
                                showTime = showTime,
                                onClick = {
                                    if (batchMode) {
                                        toggleSelection(record)
                                    } else if (record.status == InternalDownloadStatus.SUCCESS) {
                                        if (!viewModel.open(record)) {
                                            Toast.makeText(context, "文件暂时无法打开", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (batchMode) {
                                        toggleSelection(record)
                                    } else if (record.status == InternalDownloadStatus.SUCCESS) {
                                        downloadedActionRecord = record
                                    } else {
                                        downloadingActionRecord = record
                                    }
                                },
                                onToggleSelect = { toggleSelection(record) },
                                onOpen = {
                                    if (!viewModel.open(record)) {
                                        Toast.makeText(context, "文件暂时无法打开", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRetry = {
                                    viewModel.retry(record).onFailure {
                                        Toast.makeText(context, it.message ?: "重试失败", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCancel = { viewModel.cancel(record) },
                                onDelete = {
                                    viewModel.remove(record, deleteFile = record.status == InternalDownloadStatus.SUCCESS)
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDownloadDialog(
            onDismiss = { showAddDialog = false },
            onConfirmInternal = { fileName, url, suffix ->
                val resolvedFileName = appendSuffixIfNeeded(fileName, suffix)
                viewModel.addDownload(
                    InternalDownloadRequest(
                        url = url,
                        title = resolvedFileName,
                        fileName = resolvedFileName
                    )
                ).onSuccess {
                    Toast.makeText(context, "已加入内置下载器", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }.onFailure {
                    Toast.makeText(context, it.message ?: "添加失败", Toast.LENGTH_SHORT).show()
                }
            },
            onConfirmExternal = { _, url, _ ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    showAddDialog = false
                }.onFailure {
                    Toast.makeText(context, "未找到可用的外部下载器", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showSortDialog) {
        SortModeDialog(
            currentMode = sortMode,
            onDismiss = { showSortDialog = false },
            onSelectMode = {
                sortMode = it
                showSortDialog = false
            }
        )
    }

    downloadingActionRecord?.let { record ->
        DownloadActionDialog(
            actions = listOf(
                DownloadActionButton("边下边播") {
                    downloadingActionRecord = null
                    if (!viewModel.open(record)) {
                        Toast.makeText(context, "当前任务暂时无法边下边播", Toast.LENGTH_SHORT).show()
                    }
                },
                DownloadActionButton("暂停下载") {
                    downloadingActionRecord = null
                    Toast.makeText(context, "当前内置下载器暂不支持暂停下载", Toast.LENGTH_SHORT).show()
                },
                DownloadActionButton("取消下载") {
                    downloadingActionRecord = null
                    viewModel.cancel(record)
                    Toast.makeText(context, "正在取消下载任务", Toast.LENGTH_SHORT).show()
                },
                DownloadActionButton("批量取消") {
                    downloadingActionRecord = null
                    enableBatchMode(record, DownloadPageTab.Downloading)
                }
            ),
            columns = 1,
            onDismiss = { downloadingActionRecord = null }
        )
    }

    downloadedActionRecord?.let { record ->
        DownloadActionDialog(
            actions = listOf(
                DownloadActionButton("删除下载") {
                    downloadedActionRecord = null
                    viewModel.remove(record, deleteFile = true)
                },
                DownloadActionButton("批量删除") {
                    downloadedActionRecord = null
                    enableBatchMode(record, DownloadPageTab.Downloaded)
                },
                DownloadActionButton("重新下载") {
                    downloadedActionRecord = null
                    viewModel.retry(record).onFailure {
                        Toast.makeText(context, it.message ?: "重新下载失败", Toast.LENGTH_SHORT).show()
                    }
                },
                DownloadActionButton("重命名") {
                    downloadedActionRecord = null
                    renameDialogRecord = record
                    renameDialogMode = RenameDialogMode.Rename
                },
                DownloadActionButton("修改后缀") {
                    downloadedActionRecord = null
                    renameDialogRecord = record
                    renameDialogMode = RenameDialogMode.Suffix
                },
                DownloadActionButton("修改文件夹") {
                    downloadedActionRecord = null
                    pendingDirectoryRecord = record
                    folderPickerLauncher.launch(null)
                },
                DownloadActionButton("复制下载链接") {
                    downloadedActionRecord = null
                    copyToClipboard(context, "download_url", record.url)
                    Toast.makeText(context, "已复制下载链接", Toast.LENGTH_SHORT).show()
                },
                DownloadActionButton("分享本地文件") {
                    downloadedActionRecord = null
                    if (!shareDownloadedFile(context, record)) {
                        Toast.makeText(context, "当前文件暂时无法分享", Toast.LENGTH_SHORT).show()
                    }
                },
                DownloadActionButton("复制文件路径") {
                    downloadedActionRecord = null
                    val path = resolveRecordDisplayPath(record)
                    if (path.isBlank()) {
                        Toast.makeText(context, "当前文件路径不可用", Toast.LENGTH_SHORT).show()
                    } else {
                        copyToClipboard(context, "download_path", path)
                        Toast.makeText(context, "已复制文件路径", Toast.LENGTH_SHORT).show()
                    }
                },
                DownloadActionButton("转存到公开目录") {
                    downloadedActionRecord = null
                    viewModel.transferToPublicDirectory(record) { result ->
                        result.onSuccess {
                            Toast.makeText(context, "已转存到公开目录", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "转存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ),
            columns = 2,
            onDismiss = { downloadedActionRecord = null }
        )
    }

    renameDialogRecord?.let { record ->
        RenameDownloadDialog(
            record = record,
            mode = renameDialogMode,
            onDismiss = { renameDialogRecord = null },
            onConfirm = { value ->
                val targetFileName = buildRenameTarget(record, renameDialogMode, value)
                if (targetFileName.isBlank()) {
                    Toast.makeText(context, "输入内容不能为空", Toast.LENGTH_SHORT).show()
                    return@RenameDownloadDialog
                }
                viewModel.rename(record, targetFileName).onSuccess {
                    Toast.makeText(
                        context,
                        if (renameDialogMode == RenameDialogMode.Suffix) "后缀修改成功" else "重命名成功",
                        Toast.LENGTH_SHORT
                    ).show()
                    renameDialogRecord = null
                }.onFailure {
                    Toast.makeText(context, it.message ?: "修改失败", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun DownloadOutlinedActionButton(
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADDE1)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF222222),
            disabledContainerColor = Color.White,
            disabledContentColor = Color(0xFFBBBBBB)
        ),
        modifier = Modifier.height(38.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DownloadMenuTrigger(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6B6B6B)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadMenuTriggerBar(
            color = color,
            width = 2.dp,
            height = 10.dp,
            shape = RoundedCornerShape(999.dp)
        )
        DownloadMenuTriggerBar(
            color = color,
            width = 2.dp,
            height = 14.dp,
            shape = RoundedCornerShape(999.dp)
        )
        DownloadMenuTriggerBar(
            color = color,
            width = 2.dp,
            height = 14.dp,
            shape = RoundedCornerShape(999.dp)
        )
        DownloadMenuTriggerBar(
            color = color,
            width = 2.dp,
            height = 10.dp,
            shape = RoundedCornerShape(999.dp)
        )
    }
}

@Composable
private fun DownloadMenuTriggerBar(
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shape: Shape
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(color, shape)
    )
}

@Composable
private fun DownloadTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF2FB66E) else Color(0xFF6E737A),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(3.dp)
                .background(
                    color = if (selected) Color(0xFF2FB66E) else Color.Transparent,
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRecordCard(
    record: InternalDownloadEntity,
    batchMode: Boolean,
    selected: Boolean,
    showTime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val statusText = when (record.status) {
        InternalDownloadStatus.PENDING -> "排队中"
        InternalDownloadStatus.RUNNING -> "下载中"
        InternalDownloadStatus.PAUSED -> "已暂停"
        InternalDownloadStatus.SUCCESS -> "已完成"
        InternalDownloadStatus.FAILED -> "下载失败"
        InternalDownloadStatus.CANCELLED -> "已取消"
        else -> "未知状态"
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (batchMode) {
                    androidx.compose.material3.Icon(
                        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) Color(0xFF2FB66E) else Color(0xFFB0B5BC),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onToggleSelect)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = record.title.ifBlank { record.fileName },
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!batchMode) {
                    Text(
                        text = statusText,
                        color = when (record.status) {
                            InternalDownloadStatus.SUCCESS -> Color(0xFF2FB66E)
                            InternalDownloadStatus.FAILED -> Color(0xFFD93025)
                            InternalDownloadStatus.RUNNING -> Color(0xFF1A73E8)
                            else -> Color(0xFF7B8087)
                        },
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = record.fileName,
                color = Color(0xFF6F7680),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp)
            )

            Text(
                text = record.url,
                color = Color(0xFF9099A3),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )

            if (record.status == InternalDownloadStatus.FAILED && record.description.isNotBlank()) {
                Text(
                    text = record.description,
                    color = Color(0xFFD93025),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (showTime || record.totalBytes > 0L) {
                Text(
                    text = buildRecordMetaText(context, record, showTime),
                    color = Color(0xFF9AA0A6),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (record.status == InternalDownloadStatus.RUNNING ||
                record.status == InternalDownloadStatus.PENDING ||
                record.status == InternalDownloadStatus.PAUSED
            ) {
                LinearProgressIndicator(
                    progress = record.progressPercent.coerceIn(0, 100) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    color = Color(0xFF2FB66E),
                    trackColor = Color(0xFFE2E8EE)
                )
                Text(
                    text = "${record.progressPercent}% · ${Formatter.formatFileSize(context, record.downloadedBytes.coerceAtLeast(0L))}",
                    color = Color(0xFF6F7680),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            if (!batchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    when (record.status) {
                        InternalDownloadStatus.SUCCESS -> {
                            DownloadInlineAction("打开", onOpen)
                            Spacer(modifier = Modifier.width(8.dp))
                            DownloadInlineAction("删除", onDelete)
                        }
                        InternalDownloadStatus.RUNNING,
                        InternalDownloadStatus.PENDING,
                        InternalDownloadStatus.PAUSED -> {
                            DownloadInlineAction("取消", onCancel)
                        }
                        else -> {
                            DownloadInlineAction("重试", onRetry)
                            Spacer(modifier = Modifier.width(8.dp))
                            DownloadInlineAction("删除", onDelete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadInlineAction(
    title: String,
    onClick: () -> Unit
) {
    Text(
        text = title,
        color = Color(0xFF2FB66E),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun AddDownloadDialog(
    onDismiss: () -> Unit,
    onConfirmInternal: (String, String, String) -> Unit,
    onConfirmExternal: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val preferencesRepository = remember { DownloadPreferencesRepository(context) }
    var fileName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var targetMode by remember {
        mutableStateOf(
            if (preferencesRepository.getSettings().defaultEngine == DownloadEngineMode.SYSTEM) {
                DownloadTargetMode.External
            } else {
                DownloadTargetMode.Internal
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.74f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "添加文件下载",
                    color = Color(0xFF111111),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                DownloadDialogField(
                    value = fileName,
                    placeholder = "文件名称",
                    onValueChange = { fileName = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                DownloadDialogField(
                    value = url,
                    placeholder = "文件所在网址，支持m3u8",
                    onValueChange = { url = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DownloadDialogField(
                            value = suffix,
                            placeholder = "文件后缀，留空自动识别，支持m3u8",
                            onValueChange = { suffix = it }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFF5DB8F0),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadModeToggle(
                        mode = DownloadTargetMode.Internal,
                        currentMode = targetMode,
                        onSelect = { targetMode = it }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DownloadModeToggle(
                        mode = DownloadTargetMode.External,
                        currentMode = targetMode,
                        onSelect = { targetMode = it }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF39AE70),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = "下载",
                        color = Color(0xFF39AE70),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            val safeUrl = url.trim()
                            if (safeUrl.isBlank()) {
                                return@clickable
                            }
                            val safeName = fileName.trim()
                            val safeSuffix = suffix.trim().removePrefix(".")
                            if (targetMode == DownloadTargetMode.Internal) {
                                onConfirmInternal(safeName, safeUrl, safeSuffix)
                            } else {
                                onConfirmExternal(safeName, safeUrl, safeSuffix)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadDialogField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF909090),
                fontSize = 14.sp
            )
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = Color(0xFF39AE70),
            focusedIndicatorColor = Color(0xFF9C9C9C),
            unfocusedIndicatorColor = Color(0xFF9C9C9C),
            focusedTextColor = Color(0xFF202020),
            unfocusedTextColor = Color(0xFF202020)
        )
    )
}

@Composable
private fun DownloadModeToggle(
    mode: DownloadTargetMode,
    currentMode: DownloadTargetMode,
    onSelect: (DownloadTargetMode) -> Unit
) {
    val selected = mode == currentMode
    Button(
        onClick = { onSelect(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = if (selected) Color(0xFF202020) else Color(0xFF333333)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 2.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF39AE70) else Color(0xFFD8DCE0)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = mode.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SortModeDialog(
    currentMode: DownloadSortMode,
    onDismiss: () -> Unit,
    onSelectMode: (DownloadSortMode) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                DownloadSortMode.entries.forEachIndexed { index, mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode.label,
                            color = Color(0xFF202020),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (mode == currentMode) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF39AE70), CircleShape)
                            )
                        }
                    }
                    if (index != DownloadSortMode.entries.lastIndex) {
                        Divider(color = Color(0xFFF1F1F1))
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadActionDialog(
    actions: List<DownloadActionButton>,
    columns: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (columns == 1) 0.72f else 0.82f),
            shape = RoundedCornerShape(10.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "请选择操作",
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 12.dp)
                )
                Divider(color = Color(0xFFF1F1F1))
                if (columns == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.forEach { action ->
                            DownloadActionTile(
                                title = action.title,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = action.onClick
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.chunked(2).forEach { rowActions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowActions.forEach { action ->
                                    DownloadActionTile(
                                        title = action.title,
                                        modifier = Modifier.weight(1f),
                                        onClick = action.onClick
                                    )
                                }
                                if (rowActions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadActionTile(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color(0xFF202020),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RenameDownloadDialog(
    record: InternalDownloadEntity,
    mode: RenameDialogMode,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember(record.id, mode) {
        mutableStateOf(buildRenameInputDefault(record, mode))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (mode == RenameDialogMode.Suffix) "修改后缀" else "重命名",
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF7F7F7),
                        unfocusedContainerColor = Color(0xFFF7F7F7),
                        disabledContainerColor = Color(0xFFF7F7F7)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF666666),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Spacer(modifier = Modifier.width(22.dp))
                    Text(
                        text = "确定",
                        color = Color(0xFF39AE70),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            onConfirm(input.trim())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadMenuItem(
    title: String,
    drawDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
                Text(
                    text = title,
                    color = Color(0xFF202020),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 11.dp)
                )
        if (drawDivider) {
            Divider(
                color = Color(0xFFF1F1F1),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

private fun buildRecordMetaText(
    context: Context,
    record: InternalDownloadEntity,
    showTime: Boolean
): String {
    val parts = mutableListOf<String>()
    if (showTime) {
        parts += DateUtils.getRelativeTimeSpanString(
            record.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    if (record.totalBytes > 0L) {
        parts += Formatter.formatFileSize(context, record.totalBytes)
    }
    return parts.joinToString(" · ")
}

private fun resolveDownloadCategory(record: InternalDownloadEntity): String {
    val extension = record.fileName.substringAfterLast('.', "").lowercase()
    return when {
        record.mediaType == "video" || extension in setOf("mp4", "mkv", "m3u8", "ts", "mov", "avi", "wmv", "flv") -> "视频"
        record.mediaType == "audio" || extension in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg") -> "音频"
        record.mediaType == "image" || extension in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> "图片"
        extension in setOf("apk", "xapk") -> "应用"
        extension in setOf("zip", "7z", "rar", "tar", "gz") -> "压缩包"
        extension in setOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md") -> "文档"
        else -> "其它"
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareDownloadedFile(context: Context, record: InternalDownloadEntity): Boolean {
    val uri = resolveRecordUri(record) ?: return false
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = record.mimeType.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(shareIntent, "分享本地文件"))
        true
    }.getOrDefault(false)
}

private fun resolveRecordUri(record: InternalDownloadEntity): Uri? {
    return when {
        record.localUri.isNotBlank() -> Uri.parse(record.localUri)
        record.localPath.isNotBlank() -> Uri.fromFile(java.io.File(record.localPath))
        else -> null
    }
}

private fun resolveRecordDisplayPath(record: InternalDownloadEntity): String {
    return when {
        record.localPath.isNotBlank() -> record.localPath
        record.localUri.isNotBlank() -> record.localUri
        else -> record.url
    }
}

private fun buildRenameInputDefault(
    record: InternalDownloadEntity,
    mode: RenameDialogMode
): String {
    return when (mode) {
        RenameDialogMode.Rename -> record.fileName.substringBeforeLast('.', record.fileName)
        RenameDialogMode.Suffix -> record.fileName.substringAfterLast('.', "")
    }
}

private fun buildRenameTarget(
    record: InternalDownloadEntity,
    mode: RenameDialogMode,
    rawInput: String
): String {
    val input = rawInput.trim().removePrefix(".")
    if (input.isBlank()) {
        return ""
    }
    return when (mode) {
        RenameDialogMode.Rename -> {
            val extension = record.fileName.substringAfterLast('.', "")
            if (input.contains('.') || extension.isBlank()) {
                input
            } else {
                "$input.$extension"
            }
        }

        RenameDialogMode.Suffix -> {
            val baseName = record.fileName.substringBeforeLast('.', record.fileName)
            if (input.isBlank()) {
                baseName
            } else {
                "$baseName.$input"
            }
        }
    }
}

private fun appendSuffixIfNeeded(fileName: String, suffix: String): String {
    val safeFileName = fileName.trim()
    val safeSuffix = suffix.trim().removePrefix(".")
    if (safeFileName.isBlank()) {
        return safeFileName
    }
    return if (safeSuffix.isNotBlank() && !safeFileName.contains('.')) {
        "$safeFileName.$safeSuffix"
    } else {
        safeFileName
    }
}
