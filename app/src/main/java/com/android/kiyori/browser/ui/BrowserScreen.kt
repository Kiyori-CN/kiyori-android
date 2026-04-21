package com.android.kiyori.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption
import com.android.kiyori.browser.domain.BrowserHistoryEntry
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor.BrowserVideoCandidate
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor.BrowserVideoFilter
import com.android.kiyori.browser.domain.BrowserUserAgentMode
import com.android.kiyori.browser.x5.BrowserX5KernelManager
import com.android.kiyori.sniffer.VideoSnifferManager
import com.tencent.smtt.sdk.WebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserPageState,
    onAttachWebView: (WebView) -> Unit,
    onBackPressed: () -> Unit,
    onCloseBrowser: () -> Unit,
    onOpenSettingsPage: () -> Unit,
    onToggleUrlBar: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmitInput: () -> Unit,
    onReload: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onGoHome: () -> Unit,
    onSelectSearchEngine: (BrowserSearchEngine) -> Unit,
    onOpenCurrentUrl: () -> Unit,
    onEditCurrentUrl: () -> Unit,
    onOpenSearchRecord: (String) -> Unit,
    onDeleteSearchRecord: (Long) -> Unit,
    onClearSearchRecords: () -> Unit,
    onToggleIncognitoMode: () -> Unit,
    onOpenHistoryItem: (String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onSelectUserAgentMode: (BrowserUserAgentMode) -> Unit,
    onRequestPageSource: ((String) -> Unit) -> Unit,
    bookmarkFolders: List<BrowserBookmarkFolderOption>,
    onSaveBookmark: (BrowserBookmarkDraft) -> Unit,
    onOpenBookmarksPage: () -> Unit
) {
    val context = LocalContext.current
    val detectedVideos by VideoSnifferManager.detectedVideos.collectAsStateWithLifecycle()
    var showDetectedVideos by remember { mutableStateOf(false) }
    var showWindowPage by remember { mutableStateOf(false) }
    var showToolboxSheet by remember { mutableStateOf(false) }
    var addBookmarkDraft by remember { mutableStateOf<BrowserBookmarkDraft?>(null) }
    var toolboxPlaceholderTitle by remember { mutableStateOf<String?>(null) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var sourceCodeContent by remember { mutableStateOf<String?>(null) }
    var selectedWindowMode by remember { mutableStateOf(if (state.isIncognitoMode) BrowserWindowMode.Incognito else BrowserWindowMode.Normal) }
    val normalWindowEntries = remember(state.currentUrl, state.title, state.historyEntries, state.isIncognitoMode) {
        buildWindowEntries(
            currentUrl = state.currentUrl,
            currentTitle = state.title,
            historyEntries = state.historyEntries,
            includeCurrent = !state.isIncognitoMode
        )
    }
    val incognitoWindowEntries = remember(state.currentUrl, state.title, state.isIncognitoMode) {
        if (state.isIncognitoMode) {
            listOf(
                BrowserHistoryEntry(
                    id = Long.MIN_VALUE,
                    title = state.title.takeIf { it.isNotBlank() } ?: "新标签页",
                    url = state.currentUrl,
                    previewPath = "",
                    createdAt = Long.MAX_VALUE
                )
            )
        } else {
            emptyList()
        }
    }
    val candidates = remember(detectedVideos, state.currentUrl) {
        BrowserPlaybackInteractor.buildCandidates(
            videos = detectedVideos,
            currentPageUrl = state.currentUrl
        )
    }
    LaunchedEffect(showWindowPage, state.isIncognitoMode) {
        if (showWindowPage) {
            selectedWindowMode = if (state.isIncognitoMode) BrowserWindowMode.Incognito else BrowserWindowMode.Normal
        }
    }
    BackHandler {
        if (toolboxPlaceholderTitle != null) {
            toolboxPlaceholderTitle = null
        } else if (sourceCodeContent != null) {
            sourceCodeContent = null
        } else if (showUserAgentDialog) {
            showUserAgentDialog = false
        } else if (addBookmarkDraft != null) {
            addBookmarkDraft = null
        } else if (showToolboxSheet) {
            showToolboxSheet = false
        } else if (showWindowPage) {
            showWindowPage = false
        } else if (showDetectedVideos) {
            showDetectedVideos = false
        } else if (state.showUrlBar) {
            onToggleUrlBar()
        } else if (state.canGoBack) {
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
            if (showWindowPage) {
                Unit
            } else if (sourceCodeContent != null) {
                BrowserPlaceholderTopBar(
                    title = "查看源码",
                    onBack = { sourceCodeContent = null }
                )
            } else if (toolboxPlaceholderTitle != null) {
                BrowserPlaceholderTopBar(
                    title = toolboxPlaceholderTitle.orEmpty(),
                    onBack = { toolboxPlaceholderTitle = null }
                )
            } else if (!state.showUrlBar) {
                BrowserTopBar(
                    state = state,
                    detectedVideoCount = candidates.size,
                    onBackPressed = onBackPressed,
                    onToggleUrlBar = onToggleUrlBar,
                    onReload = onReload,
                    onShowDetectedVideos = { showDetectedVideos = true }
                )
            }
        },
        bottomBar = {
            if (!state.showUrlBar && !showWindowPage && toolboxPlaceholderTitle == null && sourceCodeContent == null) {
                BrowserBottomBar(
                    state = state,
                    onGoBack = onGoBack,
                    onGoForward = onGoForward,
                    onGoHome = onGoHome,
                    onShowHistory = { showWindowPage = true },
                    onOpenToolbox = { showToolboxSheet = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .let { baseModifier ->
                    if (state.showUrlBar) baseModifier else baseModifier.padding(paddingValues)
                }
        ) {
            if (showWindowPage) {
                BrowserWindowPage(
                    selectedMode = selectedWindowMode,
                    normalEntries = normalWindowEntries,
                    incognitoEntries = incognitoWindowEntries,
                    currentUrl = state.currentUrl,
                    onSelectMode = { selectedWindowMode = it },
                    onClosePage = { showWindowPage = false },
                    onCreateWindow = {
                        showWindowPage = false
                        onGoHome()
                    },
                    onClearVisibleWindows = {
                        if (selectedWindowMode == BrowserWindowMode.Normal) {
                            onClearHistory()
                        }
                        showWindowPage = false
                        onGoHome()
                    },
                    onOpenWindow = { entry ->
                        showWindowPage = false
                        if (entry.url != state.currentUrl) {
                            onOpenHistoryItem(entry.url)
                        }
                    },
                    onCloseWindow = { entry ->
                        val visibleEntries = if (selectedWindowMode == BrowserWindowMode.Normal) {
                            normalWindowEntries
                        } else {
                            incognitoWindowEntries
                        }
                        if (entry.id > 0L) {
                            onDeleteHistoryItem(entry.id)
                        }
                        if (entry.url == state.currentUrl) {
                            val nextEntry = visibleEntries.firstOrNull { it.url != entry.url }
                            showWindowPage = false
                            if (nextEntry != null) {
                                onOpenHistoryItem(nextEntry.url)
                            } else {
                                onGoHome()
                            }
                        }
                    }
                )
            } else if (sourceCodeContent != null) {
                BrowserSourceCodePage(
                    content = sourceCodeContent.orEmpty(),
                    onCopy = {
                        copyTextToClipboard(context, "browser_source_code", sourceCodeContent.orEmpty())
                        Toast.makeText(context, "已复制页面源码", Toast.LENGTH_SHORT).show()
                    }
                )
            } else if (toolboxPlaceholderTitle != null) {
                BrowserToolPlaceholderPage(title = toolboxPlaceholderTitle.orEmpty())
            } else {
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
                onClearSearchRecords = onClearSearchRecords,
                onToggleIncognitoMode = onToggleIncognitoMode
                )
            }
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

    if (showToolboxSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolboxSheet = false },
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            containerColor = Color.White
        ) {
            BrowserToolboxSheet(
                isIncognitoMode = state.isIncognitoMode,
                onAddBookmark = {
                    showToolboxSheet = false
                    addBookmarkDraft = BrowserBookmarkDraft(
                        title = state.title.takeUnless { state.isBlankPage || it == "Kiyori" }.orEmpty(),
                        url = state.currentUrl.takeUnless { it == "about:blank" }.orEmpty(),
                        iconUrl = buildDefaultBookmarkIconUrl(state.currentUrl),
                        folderId = null
                    )
                },
                onOpenBookmarks = {
                    showToolboxSheet = false
                    onOpenBookmarksPage()
                },
                onOpenDetectedVideos = {
                    showToolboxSheet = false
                    showDetectedVideos = true
                },
                onOpenUserAgent = {
                    showToolboxSheet = false
                    showUserAgentDialog = true
                },
                onOpenNetworkLog = {
                    showToolboxSheet = false
                    copyTextToClipboard(
                        context = context,
                        label = "x5_diagnostic",
                        text = BrowserX5KernelManager.buildDiagnosticReport(context)
                    )
                    Toast.makeText(context, "已复制 X5 诊断信息", Toast.LENGTH_SHORT).show()
                },
                onReload = {
                    showToolboxSheet = false
                    onReload()
                },
                onToggleIncognitoMode = {
                    showToolboxSheet = false
                    onToggleIncognitoMode()
                    Toast.makeText(
                        context,
                        if (!state.isIncognitoMode) "已开启无痕模式" else "已关闭无痕模式",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onOpenViewSource = {
                    showToolboxSheet = false
                    onRequestPageSource { pageSource ->
                        sourceCodeContent = pageSource.ifBlank { "当前页面暂无可显示的源码。" }
                    }
                },
                onOpenPlaceholder = { title ->
                    showToolboxSheet = false
                    toolboxPlaceholderTitle = title
                },
                onCloseBrowser = {
                    showToolboxSheet = false
                    onCloseBrowser()
                },
                onOpenSettings = {
                    showToolboxSheet = false
                    onOpenSettingsPage()
                },
                onDismiss = { showToolboxSheet = false }
            )
        }
    }

    if (addBookmarkDraft != null) {
        BrowserAddBookmarkDialog(
            title = "新建书签",
            initialDraft = addBookmarkDraft!!,
            folderOptions = bookmarkFolders,
            onDismiss = { addBookmarkDraft = null },
            onConfirm = { draft ->
                if (draft.url.isBlank()) {
                    Toast.makeText(context, "书签链接不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserAddBookmarkDialog
                }
                onSaveBookmark(draft)
                addBookmarkDraft = null
                Toast.makeText(context, "书签已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showUserAgentDialog) {
        BrowserUserAgentDialog(
            currentMode = state.userAgentMode,
            onDismiss = { showUserAgentDialog = false },
            onSelectMode = { mode ->
                if (mode.isImplemented) {
                    onSelectUserAgentMode(mode)
                    showUserAgentDialog = false
                } else {
                    Toast.makeText(context, "${mode.displayName}暂未实现", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserPlaceholderTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color(0xFF111827),
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = com.android.kiyori.R.drawable.ic_kiyori_browser_bottom_back),
                    contentDescription = "返回",
                    tint = Color(0xFF111827)
                )
            }
        }
    )
}

@Composable
private fun BrowserToolPlaceholderPage(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color(0xFFE5E7EB),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BrowserSourceCodePage(
    content: String,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制源码",
                    tint = Color(0xFF111827)
                )
                Text(
                    text = "复制",
                    color = Color(0xFF111827)
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp)),
            color = Color(0xFFF8FAFC)
        ) {
            Text(
                text = content,
                color = Color(0xFF111827),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
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

private enum class BrowserWindowMode {
    Normal,
    Incognito
}

private fun buildWindowEntries(
    currentUrl: String,
    currentTitle: String,
    historyEntries: List<BrowserHistoryEntry>,
    includeCurrent: Boolean
): List<BrowserHistoryEntry> {
    val dedupedEntries = LinkedHashMap<String, BrowserHistoryEntry>()
    if (includeCurrent) {
        val currentEntry = historyEntries.firstOrNull { it.url == currentUrl } ?: BrowserHistoryEntry(
            id = Long.MIN_VALUE,
            title = currentTitle.takeIf { it.isNotBlank() } ?: "新标签页",
            url = currentUrl,
            previewPath = "",
            createdAt = Long.MAX_VALUE
        )
        dedupedEntries[currentEntry.url] = currentEntry
    }
    historyEntries.forEach { entry ->
        dedupedEntries.putIfAbsent(entry.url, entry)
    }
    return dedupedEntries.values.toList()
}

@Composable
private fun BrowserWindowPage(
    selectedMode: BrowserWindowMode,
    normalEntries: List<BrowserHistoryEntry>,
    incognitoEntries: List<BrowserHistoryEntry>,
    currentUrl: String,
    onSelectMode: (BrowserWindowMode) -> Unit,
    onClosePage: () -> Unit,
    onCreateWindow: () -> Unit,
    onClearVisibleWindows: () -> Unit,
    onOpenWindow: (BrowserHistoryEntry) -> Unit,
    onCloseWindow: (BrowserHistoryEntry) -> Unit
) {
    val visibleEntries = if (selectedMode == BrowserWindowMode.Normal) normalEntries else incognitoEntries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserWindowTab(
                title = "普通窗口",
                selected = selectedMode == BrowserWindowMode.Normal,
                onClick = { onSelectMode(BrowserWindowMode.Normal) }
            )
            Spacer(modifier = Modifier.size(24.dp))
            BrowserWindowTab(
                title = "无痕窗口",
                selected = selectedMode == BrowserWindowMode.Incognito,
                onClick = { onSelectMode(BrowserWindowMode.Incognito) }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 34.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                top = 0.dp,
                end = 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            gridItems(visibleEntries, key = { "${it.id}_${it.url}" }) { entry ->
                BrowserWindowCard(
                    entry = entry,
                    selected = entry.url == currentUrl,
                    onClick = { onOpenWindow(entry) },
                    onClose = { onCloseWindow(entry) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 34.dp, end = 34.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onClosePage),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.android.kiyori.R.drawable.ic_kiyori_browser_bottom_back),
                    contentDescription = "返回",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF5B6DFF))
                    .clickable(onClick = onCreateWindow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建窗口",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onClearVisibleWindows),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "清理窗口",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun BrowserWindowTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color(0xFF111111) else Color(0xFF9CA3AF)
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 54.dp, height = 3.dp)
                .background(if (selected) Color(0xFF5B6DFF) else Color.Transparent)
        )
    }
}

@Composable
private fun BrowserWindowCard(
    entry: BrowserHistoryEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val previewBitmap = remember(entry.previewPath) {
        entry.previewPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> BitmapFactory.decodeFile(path) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF5B6DFF) else Color(0xFFE6E9F0),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.android.kiyori.R.drawable.ic_kiyori_nav_globe),
                        contentDescription = null,
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = entry.title.ifBlank { "新标签页" },
                        color = Color(0xFF111111),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭窗口",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .aspectRatio(9f / 16f),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF7F8FC)
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
                            text = entry.url.substringAfter("://").substringBefore("/").ifBlank { "新标签页" },
                            color = Color(0xFFBCC3D1),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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

@Composable
private fun BrowserUserAgentDialog(
    currentMode: BrowserUserAgentMode,
    onDismiss: () -> Unit,
    onSelectMode: (BrowserUserAgentMode) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "浏览器UA标识",
                    color = Color(0xFF111827),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                )
                BrowserUserAgentMode.selectableEntries.forEachIndexed { index, mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 22.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode.displayName,
                            color = Color(0xFF202124),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (mode == currentMode && mode.isImplemented) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index != BrowserUserAgentMode.selectableEntries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(horizontal = 14.dp)
                                .background(Color(0xFFF1F3F4))
                        )
                    }
                }
            }
        }
    }
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

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun buildDefaultBookmarkIconUrl(url: String): String {
    if (url.isBlank() || url == "about:blank") {
        return ""
    }
    return runCatching {
        val uri = Uri.parse(url)
        val scheme = uri.scheme.orEmpty()
        val host = uri.host.orEmpty()
        if (scheme.isBlank() || host.isBlank()) {
            ""
        } else {
            "$scheme://$host/favicon.ico"
        }
    }.getOrDefault("")
}

