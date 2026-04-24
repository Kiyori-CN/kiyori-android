package com.android.kiyori.history.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.kiyori.browser.data.BrowserHistoryRepository
import com.android.kiyori.browser.data.BrowserVideoHistoryRepository
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.remote.RemotePlaybackRequest
import com.android.kiyori.ui.compose.LocalKiyoriDrawerDragModifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    playbackHistoryManager: PlaybackHistoryManager,
    initialSection: HistorySection,
    useStatusBarPadding: Boolean = true,
    onHistoryChanged: () -> Unit = {},
    onBack: () -> Unit,
    onOpenBrowserHistory: (String) -> Unit,
    onOpenNetworkVideoHistory: (RemotePlaybackRequest) -> Unit,
    onOpenPlaybackHistory: (Uri, Long) -> Unit
) {
    val drawerDragModifier = LocalKiyoriDrawerDragModifier.current
    val context = LocalContext.current
    val browserHistoryRepository = remember(context) { BrowserHistoryRepository(context) }
    val browserVideoHistoryRepository = remember(context) { BrowserVideoHistoryRepository(context) }

    var selectedSection by rememberSaveable { mutableStateOf(initialSection) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var browserHistory by remember { mutableStateOf(browserHistoryRepository.getHistory()) }
    var networkVideoHistory by remember { mutableStateOf(browserVideoHistoryRepository.getHistory()) }
    var playbackHistory by remember { mutableStateOf(playbackHistoryManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<HistoryListEntry?>(null) }

    LaunchedEffect(initialSection) {
        selectedSection = initialSection
    }

    val entries = remember(
        selectedSection,
        searchQuery,
        browserHistory,
        networkVideoHistory,
        playbackHistory
    ) {
        val normalizedQuery = searchQuery.trim()
        when (selectedSection) {
            HistorySection.MINI_PROGRAM -> emptyList()
            HistorySection.WEB -> browserHistory
                .map { HistoryListEntry.Browser(it) }
                .filter { it.matches(normalizedQuery) }
            HistorySection.NETWORK_VIDEO -> networkVideoHistory
                .map { HistoryListEntry.NetworkVideo(it) }
                .filter { it.matches(normalizedQuery) }
            HistorySection.LOCAL_VIDEO -> playbackHistory
                .map { HistoryListEntry.LocalVideo(it) }
                .filter { it.matches(normalizedQuery) }
        }
    }

    val canDelete = when (selectedSection) {
        HistorySection.MINI_PROGRAM -> false
        HistorySection.WEB -> browserHistory.isNotEmpty()
        HistorySection.NETWORK_VIDEO -> networkVideoHistory.isNotEmpty()
        HistorySection.LOCAL_VIDEO -> playbackHistory.isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        HistoryTopBar(
            searchQuery = searchQuery,
            canDelete = canDelete,
            drawerDragModifier = drawerDragModifier,
            useStatusBarPadding = useStatusBarPadding,
            onBack = onBack,
            onSearchQueryChange = { searchQuery = it },
            onDeleteClick = { showClearDialog = true }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistorySection.values().forEach { section ->
                HistorySectionChip(
                    modifier = Modifier.width(84.dp),
                    label = section.label,
                    selected = selectedSection == section,
                    onClick = { selectedSection = section }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F6FA))
        ) {
            if (entries.isEmpty()) {
                HistoryEmptyState(
                    section = selectedSection,
                    hasSearchQuery = searchQuery.isNotBlank()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 14.dp,
                        end = 16.dp,
                        bottom = 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = entries, key = { it.key }) { entry ->
                        HistoryRecordCard(
                            entry = entry,
                            onClick = {
                                when (entry) {
                                    is HistoryListEntry.Browser -> onOpenBrowserHistory(entry.item.url)
                                    is HistoryListEntry.NetworkVideo -> {
                                        onOpenNetworkVideoHistory(entry.item.toRemotePlaybackRequest())
                                    }
                                    is HistoryListEntry.LocalVideo -> {
                                        onOpenPlaybackHistory(Uri.parse(entry.item.uri), entry.item.position)
                                    }
                                }
                            },
                            onLongClick = { pendingDeleteItem = entry }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "删除${selectedSection.label}",
                    color = Color(0xFF111111)
                )
            },
            text = {
                Text(
                    text = when (selectedSection) {
                        HistorySection.MINI_PROGRAM -> "当前没有可删除的小程序历史。"
                        HistorySection.WEB -> "确定清空全部网页浏览历史吗？"
                        HistorySection.NETWORK_VIDEO -> "确定清空全部网络视频记录吗？"
                        HistorySection.LOCAL_VIDEO -> "确定清空全部本地视频记录吗？"
                    },
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (selectedSection) {
                            HistorySection.MINI_PROGRAM -> Unit
                            HistorySection.WEB -> {
                                browserHistoryRepository.clearHistory()
                                browserHistory = emptyList()
                                onHistoryChanged()
                            }
                            HistorySection.NETWORK_VIDEO -> {
                                browserVideoHistoryRepository.clearHistory()
                                networkVideoHistory = emptyList()
                            }
                            HistorySection.LOCAL_VIDEO -> {
                                playbackHistoryManager.clearHistory()
                                playbackHistory = emptyList()
                                onHistoryChanged()
                            }
                        }
                        showClearDialog = false
                    },
                    enabled = canDelete
                ) {
                    Text(
                        text = "删除",
                        color = if (canDelete) Color(0xFFCC4B4B) else Color(0xFFBDBDBD)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = Color(0xFF666666))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    pendingDeleteItem?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = {
                Text(
                    text = "删除这条记录",
                    color = Color(0xFF111111)
                )
            },
            text = {
                Text(
                    text = entry.title,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (entry) {
                            is HistoryListEntry.Browser -> {
                                browserHistoryRepository.deleteHistory(entry.item.id)
                                browserHistory = browserHistoryRepository.getHistory()
                                onHistoryChanged()
                            }
                            is HistoryListEntry.NetworkVideo -> {
                                browserVideoHistoryRepository.deleteHistory(entry.item.id)
                                networkVideoHistory = browserVideoHistoryRepository.getHistory()
                            }
                            is HistoryListEntry.LocalVideo -> {
                                playbackHistoryManager.removeHistory(entry.item.uri)
                                playbackHistory = playbackHistoryManager.getHistory()
                                onHistoryChanged()
                            }
                        }
                        pendingDeleteItem = null
                    }
                ) {
                    Text("删除", color = Color(0xFFCC4B4B))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("取消", color = Color(0xFF666666))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun HistoryTopBar(
    searchQuery: String,
    canDelete: Boolean,
    drawerDragModifier: Modifier,
    useStatusBarPadding: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (useStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .height(64.dp)
            .padding(start = 0.dp, end = 12.dp)
            .then(drawerDragModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF111827),
                modifier = Modifier.size(25.dp)
            )
        }

        Text(
            text = "历史记录",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827),
            modifier = Modifier.offset(x = (-4).dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(32.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F1F1)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF1C1C1C),
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "搜索",
                                color = Color(0xFF9E9E9E),
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(5.dp))

        Surface(
            modifier = Modifier
                .alpha(if (canDelete) 1f else 0.55f)
                .width(58.dp)
                .height(32.dp),
            shape = RoundedCornerShape(11.dp),
            shadowElevation = 4.dp,
            color = Color.White
        ) {
            TextButton(
                onClick = onDeleteClick,
                enabled = canDelete,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "删除",
                    color = Color(0xFF1C1C1C),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HistorySectionChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFE8F6EA) else Color(0xFFF4F4F4),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color(0xFF4E9B65) else Color(0xFF2B2B2B)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRecordCard(
    entry: HistoryListEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryThumbnail(
                previewPath = entry.previewPath,
                title = entry.title
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.title,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111111),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.secondaryText,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                entry.tertiaryText?.let { tertiaryText ->
                    Text(
                        text = tertiaryText,
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(
    previewPath: String?,
    title: String
) {
    val previewFile = remember(previewPath) {
        previewPath?.takeIf { it.isNotBlank() }?.let(::File)
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE1EBF7)),
        contentAlignment = Alignment.Center
    ) {
        if (previewFile?.exists() == true) {
            AsyncImage(
                model = previewFile,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = title.take(1).ifBlank { "记" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFAAB6C5)
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(
    section: HistorySection,
    hasSearchQuery: Boolean
) {
    val message = when {
        hasSearchQuery -> "没有找到匹配的记录"
        section == HistorySection.MINI_PROGRAM -> "当前还没有小程序历史"
        section == HistorySection.WEB -> "继续浏览网页后，记录会显示在这里"
        section == HistorySection.NETWORK_VIDEO -> "继续在浏览器播放网络视频后，记录会显示在这里"
        else -> "继续播放本地视频后，记录会显示在这里"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp
        )
    }
}

private sealed interface HistoryListEntry {
    val key: String
    val title: String
    val secondaryText: String
    val tertiaryText: String?
    val previewPath: String?

    fun matches(query: String): Boolean

    data class Browser(
        val item: BrowserHistoryRepository.HistoryItem
    ) : HistoryListEntry {
        override val key: String = "browser_${item.id}"
        override val title: String = item.title.ifBlank {
            extractHost(item.url).ifBlank { item.url }
        }
        override val secondaryText: String = item.url
        override val tertiaryText: String = formatRecentTime(item.createdAt)
        override val previewPath: String? = item.previewPath.takeIf { it.isNotBlank() }

        override fun matches(query: String): Boolean {
            if (query.isBlank()) {
                return true
            }
            return title.contains(query, ignoreCase = true) ||
                item.url.contains(query, ignoreCase = true)
        }
    }

    data class NetworkVideo(
        val item: BrowserVideoHistoryRepository.HistoryItem
    ) : HistoryListEntry {
        override val key: String = "network_video_${item.id}"
        override val title: String = item.title.ifBlank {
            extractHost(item.url).ifBlank { "网络视频" }
        }
        override val secondaryText: String = item.url
        override val tertiaryText: String = buildString {
            val sourceText = item.sourcePageUrl.takeIf { it.isNotBlank() }
                ?.let { extractHost(it).ifBlank { it } }
                ?: "浏览器播放"
            append(sourceText)
            append(" · ")
            append(formatRecentTime(item.createdAt))
        }
        override val previewPath: String? = null

        override fun matches(query: String): Boolean {
            if (query.isBlank()) {
                return true
            }
            return title.contains(query, ignoreCase = true) ||
                item.url.contains(query, ignoreCase = true) ||
                item.sourcePageUrl.contains(query, ignoreCase = true)
        }
    }

    data class LocalVideo(
        val item: PlaybackHistoryManager.HistoryItem
    ) : HistoryListEntry {
        override val key: String = "local_video_${item.uri}"
        override val title: String = item.fileName
        override val secondaryText: String = item.folderName.ifBlank {
            extractHost(item.uri).ifBlank { "本地视频" }
        }
        override val tertiaryText: String = buildString {
            append("足迹：已播放 ")
            append(formatPlaybackPosition(item.position))
            append(" · ")
            append(formatRecentTime(item.lastPlayed))
        }
        override val previewPath: String? = item.thumbnailPath?.takeIf { it.isNotBlank() }

        override fun matches(query: String): Boolean {
            if (query.isBlank()) {
                return true
            }
            return item.fileName.contains(query, ignoreCase = true) ||
                item.folderName.contains(query, ignoreCase = true)
        }
    }
}

private fun extractHost(url: String): String {
    return runCatching {
        Uri.parse(url).host.orEmpty().removePrefix("www.")
    }.getOrDefault("")
}

private fun formatPlaybackPosition(positionMs: Long): String {
    val totalSeconds = positionMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatRecentTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val pattern = if (android.text.format.DateUtils.isToday(timestampMs)) {
        "'今天' HH:mm"
    } else if (now - timestampMs < 7L * 24L * 60L * 60L * 1000L) {
        "MM-dd HH:mm"
    } else {
        "yyyy-MM-dd"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))
}
