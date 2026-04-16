package com.fam4k007.videoplayer.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fam4k007.videoplayer.browser.domain.BrowserHistoryEntry
import com.fam4k007.videoplayer.browser.domain.BrowserPageState
import com.fam4k007.videoplayer.browser.domain.BrowserSearchEngine
import com.fam4k007.videoplayer.browser.playback.BrowserPlaybackInteractor
import com.fam4k007.videoplayer.browser.playback.BrowserPlaybackInteractor.BrowserVideoCandidate
import com.fam4k007.videoplayer.browser.playback.BrowserPlaybackInteractor.BrowserVideoFilter
import com.fam4k007.videoplayer.sniffer.VideoSnifferManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserPageState,
    onAttachWebView: (WebView) -> Unit,
    onBackPressed: () -> Unit,
    onToggleUrlBar: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmitInput: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenExternalBrowser: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onClearCookies: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onSelectSearchEngine: (BrowserSearchEngine) -> Unit,
    onOpenCurrentUrl: () -> Unit,
    onEditCurrentUrl: () -> Unit,
    onOpenSearchRecord: (String) -> Unit,
    onDeleteSearchRecord: (Long) -> Unit,
    onClearSearchRecords: () -> Unit,
    onOpenHistoryItem: (String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val detectedVideos by VideoSnifferManager.detectedVideos.collectAsStateWithLifecycle()
    var showDetectedVideos by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val candidates = remember(detectedVideos, state.currentUrl) {
        BrowserPlaybackInteractor.buildCandidates(
            videos = detectedVideos,
            currentPageUrl = state.currentUrl
        )
    }
    val bestVideo = remember(detectedVideos, state.currentUrl) {
        BrowserPlaybackInteractor.selectBestVideo(detectedVideos, state.currentUrl)
    }

    BackHandler {
        if (state.canGoBack) {
            onGoBack()
        } else {
            onBackPressed()
        }
    }

    LaunchedEffect(state.blockedExternalUrl) {
        val url = state.blockedExternalUrl ?: return@LaunchedEffect
        Toast.makeText(context, "已拦截外部链接：$url", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            BrowserTopBar(
                state = state,
                detectedVideoCount = candidates.size,
                onBackPressed = onBackPressed,
                onToggleUrlBar = onToggleUrlBar,
                onReload = onReload,
                onShowDetectedVideos = { showDetectedVideos = true },
                onPlayBestVideo = {
                    bestVideo?.let { BrowserPlaybackInteractor.play(context, it) }
                }
            )
        },
        bottomBar = {
            BrowserBottomBar(
                state = state,
                onGoBack = onGoBack,
                onGoForward = onGoForward,
                onGoHome = onGoHome,
                onShowHistory = { showHistorySheet = true },
                onToggleDesktopMode = onToggleDesktopMode,
                onOpenExternalBrowser = onOpenExternalBrowser,
                onCopyCurrentUrl = onCopyCurrentUrl,
                onClearCookies = onClearCookies
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { webContext ->
                    WebView(webContext).also(onAttachWebView)
                },
                modifier = Modifier.fillMaxSize()
            )

            if (state.isBlankPage) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "空白首页",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "顶部搜索栏支持输入网址，也支持直接搜索关键词。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    progress = state.progress.coerceIn(0, 100) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            BrowserUrlDropdownOverlay(
                state = state,
                onDismiss = onToggleUrlBar,
                onInputChanged = onInputChanged,
                onSubmitInput = onSubmitInput,
                onSelectSearchEngine = onSelectSearchEngine,
                onOpenCurrentUrl = onOpenCurrentUrl,
                onCopyCurrentUrl = onCopyCurrentUrl,
                onEditCurrentUrl = onEditCurrentUrl,
                onOpenSearchRecord = onOpenSearchRecord,
                onDeleteSearchRecord = onDeleteSearchRecord,
                onClearSearchRecords = onClearSearchRecords
            )
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false }
        ) {
            BrowserHistorySheet(
                historyEntries = state.historyEntries,
                currentUrl = state.currentUrl,
                onOpen = { entry ->
                    showHistorySheet = false
                    onOpenHistoryItem(entry.url)
                },
                onDelete = onDeleteHistoryItem,
                onClearAll = onClearHistory
            )
        }
    }

    if (showDetectedVideos) {
        ModalBottomSheet(
            onDismissRequest = { showDetectedVideos = false }
        ) {
            BrowserDetectedVideosSheet(
                candidates = candidates,
                onPlay = { candidate ->
                    showDetectedVideos = false
                    BrowserPlaybackInteractor.play(context, candidate.video)
                },
                onCopy = { candidate ->
                    copyBrowserUrl(context, candidate.video.url)
                }
            )
        }
    }
}

@Composable
private fun BrowserHistorySheet(
    historyEntries: List<BrowserHistoryEntry>,
    currentUrl: String,
    onOpen: (BrowserHistoryEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史网页",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF111827)
            )
            if (historyEntries.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null
                    )
                    Text("一键清空")
                }
            }
        }
        Text(
            text = if (historyEntries.isEmpty()) {
                "当前还没有可查看的历史网页。"
            } else {
                "最近 30 个网页按时间倒序显示，点击缩略图即可切换。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (historyEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "继续浏览网页后，历史记录会显示在这里。",
                    color = Color(0xFF6B7280)
                )
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(620.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(historyEntries, key = { it.id }) { entry ->
                BrowserHistoryCard(
                    entry = entry,
                    selected = entry.url == currentUrl,
                    onClick = { onOpen(entry) },
                    onDelete = { onDelete(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun BrowserHistoryCard(
    entry: BrowserHistoryEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val previewBitmap = remember(entry.previewPath) {
        entry.previewPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> BitmapFactory.decodeFile(path) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            } else {
                Color(0xFFF8FAFC)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 19f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF4FF)
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.url.substringAfter("://").substringBefore("/").ifBlank { "网页预览" },
                                color = Color(0xFF1D4ED8),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                Text(
                    text = entry.title,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = entry.url.substringAfter("://"),
                    color = Color(0xFF6B7280),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除网页历史",
                        tint = Color(0xFF374151)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserDetectedVideosSheet(
    candidates: List<BrowserVideoCandidate>,
    onPlay: (BrowserVideoCandidate) -> Unit,
    onCopy: (BrowserVideoCandidate) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(BrowserVideoFilter.RECOMMENDED) }
    val visibleCandidates = remember(candidates, selectedFilter) {
        BrowserPlaybackInteractor.filterCandidates(candidates, selectedFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "嗅探结果",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF111827)
        )
        Text(
            text = if (candidates.isEmpty()) {
                "当前页面未发现可播放媒体资源。"
            } else {
                "已整理 ${candidates.size} 个候选资源，默认优先展示推荐结果。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Text(
            text = "规则：优先 m3u8/dash/mp4，降低分片、字幕、封面和广告类噪声。",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (candidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "继续浏览页面后，新的请求会自动出现在这里。",
                    color = Color(0xFF6B7280)
                )
            }
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrowserFilterChip(
                label = "推荐",
                count = BrowserPlaybackInteractor.filterCandidates(
                    candidates,
                    BrowserVideoFilter.RECOMMENDED
                ).size,
                selected = selectedFilter == BrowserVideoFilter.RECOMMENDED,
                onClick = { selectedFilter = BrowserVideoFilter.RECOMMENDED }
            )
            BrowserFilterChip(
                label = "M3U8/DASH",
                count = BrowserPlaybackInteractor.filterCandidates(
                    candidates,
                    BrowserVideoFilter.MANIFEST
                ).size,
                selected = selectedFilter == BrowserVideoFilter.MANIFEST,
                onClick = { selectedFilter = BrowserVideoFilter.MANIFEST }
            )
            BrowserFilterChip(
                label = "MP4",
                count = BrowserPlaybackInteractor.filterCandidates(
                    candidates,
                    BrowserVideoFilter.MP4
                ).size,
                selected = selectedFilter == BrowserVideoFilter.MP4,
                onClick = { selectedFilter = BrowserVideoFilter.MP4 }
            )
            BrowserFilterChip(
                label = "全部",
                count = candidates.size,
                selected = selectedFilter == BrowserVideoFilter.ALL,
                onClick = { selectedFilter = BrowserVideoFilter.ALL }
            )
        }

        if (visibleCandidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前筛选条件下没有结果，切换到“全部”可查看所有候选。",
                    color = Color(0xFF6B7280)
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            items(visibleCandidates, key = { "${it.video.url}_${it.video.timestamp}" }) { candidate ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = candidate.video.getDisplayText(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f)
                        )
                        if (candidate.isRecommended) {
                            Text(
                                text = "推荐",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                    Text(
                        text = "${candidate.format} · 评分 ${candidate.score}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = candidate.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = candidate.video.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (candidate.video.pageUrl.isNotBlank()) {
                        Text(
                            text = "来源页: ${candidate.video.pageUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(onClick = { onPlay(candidate) }) {
                            Text("播放")
                        }
                        TextButton(onClick = { onCopy(candidate) }) {
                            Text("复制")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label ($count)") }
    )
}

fun openBrowserUrlExternally(context: Context, url: String) {
    if (url.isBlank()) {
        return
    }
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun copyBrowserUrl(context: Context, url: String) {
    if (url.isBlank()) {
        return
    }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("browser_url", url))
    Toast.makeText(context, "已复制当前链接", Toast.LENGTH_SHORT).show()
}
