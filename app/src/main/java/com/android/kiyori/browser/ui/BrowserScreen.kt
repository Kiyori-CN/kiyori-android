package com.android.kiyori.browser.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import coil.compose.AsyncImage
import com.android.kiyori.R
import com.android.kiyori.app.MainActivity
import com.android.kiyori.browser.data.BrowserBookmarkRepository
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption
import com.android.kiyori.browser.domain.BrowserHistoryEntry
import com.android.kiyori.browser.domain.BrowserPageState
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor.BrowserVideoCandidate
import com.android.kiyori.browser.playback.BrowserPlaybackInteractor
import com.android.kiyori.browser.domain.BrowserUserAgentMode
import com.android.kiyori.download.InternalDownloadRequest
import com.android.kiyori.download.requestDownloadWithPreferences
import com.android.kiyori.download.ui.DownloadCenterScreen
import com.android.kiyori.browser.web.BrowserNetworkLogEntry
import com.android.kiyori.browser.web.BrowserNetworkLogManager
import com.android.kiyori.browser.web.BrowserRequestBlocklistManager
import com.android.kiyori.history.PlaybackHistoryManager
import com.android.kiyori.history.ui.HistoryScreen
import com.android.kiyori.history.ui.HistorySection
import com.android.kiyori.player.ui.VideoPlayerActivity
import com.android.kiyori.remote.RemotePlaybackHeaders
import com.android.kiyori.sniffer.DetectedVideo
import com.android.kiyori.sniffer.UrlDetector
import com.android.kiyori.sniffer.VideoSnifferManager
import com.android.kiyori.ui.compose.KiyoriBottomDrawer
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
    getCustomGlobalUserAgent: () -> String,
    getCustomSiteUserAgent: () -> String,
    getCurrentSiteHost: () -> String,
    onSaveCustomGlobalUserAgent: (String) -> Unit,
    onSaveCustomSiteUserAgent: (String) -> Unit,
    onDownloadDetectedVideo: (BrowserVideoCandidate) -> Unit,
    onRequestPageSource: ((String) -> Unit) -> Unit,
    bookmarkFolders: List<BrowserBookmarkFolderOption>,
    onSaveBookmark: (BrowserBookmarkDraft) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val bookmarkRepository = remember(context) { BrowserBookmarkRepository(context) }
    val playbackHistoryManager = remember(context) { PlaybackHistoryManager(context) }
    val detectedVideos by VideoSnifferManager.detectedVideos.collectAsStateWithLifecycle()
    val networkLogs by BrowserNetworkLogManager.entries.collectAsStateWithLifecycle()
    var showDetectedVideos by remember { mutableStateOf(false) }
    var showBookmarkDrawer by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showDownloadDrawer by remember { mutableStateOf(false) }
    var showNetworkLog by remember { mutableStateOf(false) }
    var showWindowPage by remember { mutableStateOf(false) }
    var showToolboxSheet by remember { mutableStateOf(false) }
    var addBookmarkDraft by remember { mutableStateOf<BrowserBookmarkDraft?>(null) }
    var toolboxPlaceholderTitle by remember { mutableStateOf<String?>(null) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var userAgentInputMode by remember { mutableStateOf<BrowserUserAgentMode?>(null) }
    var sourceCodeContent by remember { mutableStateOf<String?>(null) }
    var selectedWindowMode by remember { mutableStateOf(if (state.isIncognitoMode) BrowserWindowMode.Incognito else BrowserWindowMode.Normal) }
    var bookmarkDrawerFolders by remember { mutableStateOf(bookmarkRepository.getFolders()) }
    var bookmarkDrawerItems by remember { mutableStateOf(bookmarkRepository.getBookmarks()) }
    var bookmarkFolderOptionsState by remember {
        mutableStateOf(bookmarkDrawerFolders.map { folder ->
            BrowserBookmarkFolderOption(id = folder.id, title = folder.title)
        })
    }
    val hasFullscreenOverlay = showWindowPage || toolboxPlaceholderTitle != null || sourceCodeContent != null
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

    fun refreshBookmarkDrawerState() {
        val folders = bookmarkRepository.getFolders()
        bookmarkDrawerFolders = folders
        bookmarkDrawerItems = bookmarkRepository.getBookmarks()
        bookmarkFolderOptionsState = folders.map { folder ->
            BrowserBookmarkFolderOption(id = folder.id, title = folder.title)
        }
    }

    LaunchedEffect(bookmarkFolders) {
        refreshBookmarkDrawerState()
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
        } else if (userAgentInputMode != null) {
            userAgentInputMode = null
        } else if (showUserAgentDialog) {
            showUserAgentDialog = false
        } else if (addBookmarkDraft != null) {
            addBookmarkDraft = null
        } else if (showToolboxSheet) {
            showToolboxSheet = false
        } else if (showDownloadDrawer) {
            showDownloadDrawer = false
        } else if (showHistoryDrawer) {
            showHistoryDrawer = false
        } else if (showBookmarkDrawer) {
            showBookmarkDrawer = false
        } else if (showWindowPage) {
            showWindowPage = false
        } else if (showNetworkLog) {
            showNetworkLog = false
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
            AndroidView(
                factory = { webContext ->
                    WebView(webContext).also(onAttachWebView)
                },
                modifier = Modifier.fillMaxSize()
            )

            if (false) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ç©ºç™½é¦–é¡µ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "é¡¶éƒ¨æœç´¢æ æ”¯æŒè¾“å…¥ç½‘å€ï¼Œä¹Ÿæ”¯æŒç›´æŽ¥æœç´¢å…³é”®è¯ã€‚",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            if (!hasFullscreenOverlay && state.isBlankPage) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "\u7a7a\u767d\u9996\u9875",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "\u9876\u90e8\u641c\u7d22\u680f\u652f\u6301\u8f93\u5165\u7f51\u5740\uff0c\u4e5f\u652f\u6301\u76f4\u63a5\u641c\u7d22\u5173\u952e\u8bcd\u3002",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            if (!hasFullscreenOverlay && state.isLoading) {
                LinearProgressIndicator(
                    progress = state.progress.coerceIn(0, 100) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

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
            }

            if (false) {
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

            if (false) {
                LinearProgressIndicator(
                    progress = state.progress.coerceIn(0, 100) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

                if (!hasFullscreenOverlay) BrowserUrlDropdownOverlay(
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
                },
                onDownload = { candidate ->
                    onDownloadDetectedVideo(candidate)
                }
            )
        }
    }

    if (showNetworkLog) {
        KiyoriBottomDrawer(
            onDismissRequest = { showNetworkLog = false }
        ) {
            BrowserNetworkLogSheet(
                entries = networkLogs,
                currentPageUrl = state.currentUrl
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
                    refreshBookmarkDrawerState()
                    showBookmarkDrawer = true
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
                    showNetworkLog = true
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
                    if (title == "\u5386\u53f2") {
                        showHistoryDrawer = true
                    } else if (title == "下载") {
                        showDownloadDrawer = true
                    } else {
                        toolboxPlaceholderTitle = title
                    }
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

    if (showDownloadDrawer) {
        DownloadCenterScreen(
            onDismissRequest = { showDownloadDrawer = false },
            onOpenDownloadSettings = {
                showDownloadDrawer = false
                MainActivity.start(
                    context = context,
                    initialTab = MainActivity.TAB_SETTINGS,
                    initialSettingsPage = MainActivity.SETTINGS_PAGE_DOWNLOAD
                )
                activity?.finish()
                activity?.overridePendingTransition(
                    R.anim.no_anim,
                    R.anim.no_anim
                )
            }
        )
    }

    if (showHistoryDrawer) {
        KiyoriBottomDrawer(
            onDismissRequest = { showHistoryDrawer = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                HistoryScreen(
                    playbackHistoryManager = playbackHistoryManager,
                    initialSection = HistorySection.WEB,
                    useStatusBarPadding = false,
                    onBack = { showHistoryDrawer = false },
                    onOpenBrowserHistory = { url ->
                        showHistoryDrawer = false
                        if (url != state.currentUrl) {
                            onOpenHistoryItem(url)
                        }
                    },
                    onOpenPlaybackHistory = { uri, startPosition ->
                        showHistoryDrawer = false
                        context.startActivity(
                            Intent(context, VideoPlayerActivity::class.java).apply {
                                data = uri
                                putExtra("lastPosition", startPosition)
                            }
                        )
                        activity?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
        }
    }

    if (showBookmarkDrawer) {
        BrowserBookmarksDrawer(
            folders = bookmarkDrawerFolders,
            bookmarks = bookmarkDrawerItems,
            onDismissRequest = {
                showBookmarkDrawer = false
                refreshBookmarkDrawerState()
            },
            onCreateFolder = { title, parentId ->
                bookmarkRepository.addFolder(title, parentId)
                refreshBookmarkDrawerState()
            },
            onRenameFolder = { folderId, title ->
                bookmarkRepository.renameFolder(folderId, title)
                refreshBookmarkDrawerState()
            },
            onMoveFolder = { folderId, parentId ->
                bookmarkRepository.moveFolder(folderId, parentId).also {
                    refreshBookmarkDrawerState()
                }
            },
            onDeleteFolder = { folderId ->
                bookmarkRepository.deleteFolder(folderId)
                refreshBookmarkDrawerState()
            },
            onSaveBookmark = { draft ->
                onSaveBookmark(draft)
                refreshBookmarkDrawerState()
            },
            onDeleteBookmark = { bookmarkId ->
                bookmarkRepository.deleteBookmark(bookmarkId)
                refreshBookmarkDrawerState()
            },
            onOpenBookmark = { url ->
                showBookmarkDrawer = false
                if (url != state.currentUrl) {
                    onOpenHistoryItem(url)
                }
            }
        )
    }

    if (addBookmarkDraft != null) {
        BrowserAddBookmarkDialog(
            title = "新建书签",
            initialDraft = addBookmarkDraft!!,
            folderOptions = bookmarkFolderOptionsState,
            onDismiss = { addBookmarkDraft = null },
            onConfirm = { draft ->
                if (draft.url.isBlank()) {
                    Toast.makeText(context, "书签链接不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserAddBookmarkDialog
                }
                onSaveBookmark(draft)
                refreshBookmarkDrawerState()
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
                if (mode == BrowserUserAgentMode.CUSTOM_GLOBAL) {
                    showUserAgentDialog = false
                    userAgentInputMode = mode
                } else if (mode == BrowserUserAgentMode.CUSTOM_SITE) {
                    if (getCurrentSiteHost().isBlank()) {
                        Toast.makeText(context, "当前页面没有可保存的站点UA", Toast.LENGTH_SHORT).show()
                    } else {
                        showUserAgentDialog = false
                        userAgentInputMode = mode
                    }
                } else if (mode.isImplemented) {
                    onSelectUserAgentMode(mode)
                    showUserAgentDialog = false
                } else {
                    Toast.makeText(context, "${mode.displayName}暂未实现", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (userAgentInputMode != null) {
        val inputMode = userAgentInputMode!!
        val siteHost = getCurrentSiteHost()
        BrowserSingleInputDialog(
            title = if (inputMode == BrowserUserAgentMode.CUSTOM_GLOBAL) {
                "自定义全局UA"
            } else {
                "自定义当前网站UA（$siteHost）"
            },
            initialValue = if (inputMode == BrowserUserAgentMode.CUSTOM_GLOBAL) {
                getCustomGlobalUserAgent()
            } else {
                getCustomSiteUserAgent()
            },
            confirmText = "保存",
            onDismiss = { userAgentInputMode = null },
            onConfirm = { value ->
                if (inputMode == BrowserUserAgentMode.CUSTOM_GLOBAL) {
                    onSaveCustomGlobalUserAgent(value)
                } else {
                    onSaveCustomSiteUserAgent(value)
                }
                onSelectUserAgentMode(inputMode)
                userAgentInputMode = null
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
private fun BrowserBookmarksDrawer(
    folders: List<BrowserBookmarkFolder>,
    bookmarks: List<BrowserBookmarkItem>,
    onDismissRequest: () -> Unit,
    onCreateFolder: (String, Long?) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onMoveFolder: (Long, Long?) -> Boolean,
    onDeleteFolder: (Long) -> Unit,
    onSaveBookmark: (BrowserBookmarkDraft) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onOpenBookmark: (String) -> Unit
) {
    KiyoriBottomDrawer(
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            BrowserBookmarksScreen(
                folders = folders,
                bookmarks = bookmarks,
                onNavigateBack = onDismissRequest,
                onCreateFolder = onCreateFolder,
                onRenameFolder = onRenameFolder,
                onMoveFolder = onMoveFolder,
                onDeleteFolder = onDeleteFolder,
                onSaveBookmark = onSaveBookmark,
                onDeleteBookmark = onDeleteBookmark,
                onOpenBookmark = onOpenBookmark
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
    onCopy: (BrowserVideoCandidate) -> Unit,
    onDownload: (BrowserVideoCandidate) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("ALL") }
    val formatCounts = remember(candidates) {
        candidates.groupingBy { it.format }
            .eachCount()
            .filterKeys { it.isNotBlank() && it != "UNKNOWN" }
    }
    val formatFilters = remember(formatCounts) {
        buildList {
            add("ALL")
            addAll(
                formatCounts.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.value }
                            .thenBy { UrlDetector.getFormatSortOrder(it.key) }
                            .thenBy { it.key }
                    )
                    .map { it.key }
            )
        }
    }
    val safeSelectedFormat = remember(selectedFormat, formatFilters) {
        selectedFormat.takeIf { it == "ALL" || it in formatFilters } ?: "ALL"
    }
    LaunchedEffect(safeSelectedFormat) {
        if (selectedFormat != safeSelectedFormat) {
            selectedFormat = safeSelectedFormat
        }
    }
    val visibleCandidates = remember(candidates, safeSelectedFormat) {
        when (safeSelectedFormat) {
            "ALL" -> candidates
            else -> candidates.filter { it.format == safeSelectedFormat }
        }
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
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formatFilters.forEach { format ->
                val count = if (format == "ALL") candidates.size else (formatCounts[format] ?: 0)
                BrowserFilterChip(
                    label = if (format == "ALL") "全部" else format,
                    count = count,
                    selected = safeSelectedFormat == format,
                    onClick = { selectedFormat = format }
                )
            }
        }

        if (visibleCandidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前格式下没有结果，切换到“全部”可查看所有候选。",
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
                    }
                    Text(
                        text = candidate.format,
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
                        if (candidate.canPreview) {
                            Button(onClick = { onPlay(candidate) }) {
                                Text("播放")
                            }
                        }
                        androidx.compose.material3.OutlinedButton(onClick = { onDownload(candidate) }) {
                            Text("下载")
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

private enum class BrowserNetworkLogTab(
    val label: String,
    val category: UrlDetector.NetworkCategory?
) {
    ALL("全部", null),
    VIDEO("视频", UrlDetector.NetworkCategory.VIDEO),
    AUDIO("音乐", UrlDetector.NetworkCategory.AUDIO),
    IMAGE("图片", UrlDetector.NetworkCategory.IMAGE),
    WEB("网页", UrlDetector.NetworkCategory.WEB),
    OTHER("其他", UrlDetector.NetworkCategory.OTHER),
    BLOCKED("拦截", UrlDetector.NetworkCategory.BLOCKED)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowserNetworkLogSheet(
    entries: List<BrowserNetworkLogEntry>,
    currentPageUrl: String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BrowserNetworkLogTab.ALL) }
    var actionEntry by remember { mutableStateOf<BrowserNetworkLogEntry?>(null) }
    var fullLinkEntry by remember { mutableStateOf<BrowserNetworkLogEntry?>(null) }
    val filteredEntries = remember(entries, selectedTab) {
        when (selectedTab.category) {
            null -> entries
            else -> entries.filter { it.category == selectedTab.category }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrowserNetworkLogTab.values().forEach { tab ->
                val count = if (tab.category == null) {
                    entries.size
                } else {
                    entries.count { it.category == tab.category }
                }
                BrowserFilterChip(
                    label = tab.label,
                    count = count,
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab }
                )
            }
        }

        Text(
            text = "当前加载的网址域名与原网站域名不一致时字体为黄色，红色则表示该链接被拦截",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(top = 14.dp)
        )

        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前分类下还没有网络请求记录。",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredEntries, key = { "${it.timestamp}_${it.category}_${it.url}" }) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF3F4F6))
                        .combinedClickable(
                            onClick = { actionEntry = entry },
                            onLongClick = { actionEntry = entry }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (entry.category == UrlDetector.NetworkCategory.IMAGE) {
                        AsyncImage(
                            model = entry.url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = buildBrowserNetworkLogLabel(entry),
                        color = resolveBrowserNetworkLogColor(
                            entry = entry,
                            currentPageUrl = currentPageUrl
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (actionEntry != null) {
        BrowserNetworkLogActionDialog(
            entry = actionEntry!!,
            onDismiss = { actionEntry = null },
            onCopy = {
                copyTextToClipboard(context, "network_log_url", actionEntry!!.url)
                Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                actionEntry = null
            },
            onPlay = {
                playBrowserNetworkLogEntry(context, actionEntry!!)
                actionEntry = null
            },
            onDownload = {
                downloadBrowserNetworkLogEntry(context, actionEntry!!)
                actionEntry = null
            },
            onBlock = {
                val entry = actionEntry!!
                BrowserRequestBlocklistManager.add(entry.url)
                BrowserNetworkLogManager.addBlocked(
                    url = entry.url,
                    pageUrl = entry.pageUrl,
                    pageTitle = entry.title,
                    headers = entry.headers
                )
                Toast.makeText(context, "已加入拦截列表", Toast.LENGTH_SHORT).show()
                actionEntry = null
            },
            onOpenExternal = {
                openBrowserUrlExternally(context, actionEntry!!.url)
                actionEntry = null
            },
            onViewFullLink = {
                fullLinkEntry = actionEntry
                actionEntry = null
            }
        )
    }

    if (fullLinkEntry != null) {
        BrowserNetworkLogFullLinkDialog(
            entry = fullLinkEntry!!,
            onDismiss = { fullLinkEntry = null },
            onCopy = {
                copyTextToClipboard(context, "network_log_full_url", fullLinkEntry!!.url)
                Toast.makeText(context, "已复制完整链接", Toast.LENGTH_SHORT).show()
                fullLinkEntry = null
            }
        )
    }
}

private fun buildBrowserNetworkLogLabel(entry: BrowserNetworkLogEntry): String {
    val displayUrl = buildBrowserNetworkLogDisplayUrl(entry.url)
    val extension = runCatching {
        Uri.parse(entry.url).lastPathSegment.orEmpty()
    }.getOrDefault("")
        .substringBefore("?")
        .substringAfterLast('.', "")
        .takeIf { it.isNotBlank() && it.length <= 10 }

    val format = UrlDetector.getDetectedResourceFormat(entry.url, entry.headers)
        .takeUnless { it == "UNKNOWN" || it == "VIDEO" || it == "AUDIO" }

    val prefix = when {
        extension != null -> "[.$extension] "
        format != null -> "[${format.lowercase()}] "
        entry.category == UrlDetector.NetworkCategory.BLOCKED -> "[blocked] "
        else -> ""
    }

    return prefix + displayUrl
}

private fun buildBrowserNetworkLogDisplayUrl(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
    val host = uri.host.orEmpty()
    val path = uri.encodedPath.orEmpty()
    val query = uri.encodedQuery.orEmpty()
    val compactPath = when {
        path.isBlank() -> ""
        path.length <= 34 -> path
        else -> {
            val segments = path.split('/').filter { it.isNotBlank() }
            when {
                segments.isEmpty() -> path.take(34)
                segments.size == 1 -> "/.../${segments.last().takeLast(22)}"
                else -> "/${segments.first()}/.../${segments.last().takeLast(22)}"
            }
        }
    }
    val compactQuery = when {
        query.isBlank() -> ""
        query.length <= 16 -> "?$query"
        else -> "?${query.take(16)}..."
    }

    return buildString {
        append(host.ifBlank { url.take(44) })
        append(compactPath)
        append(compactQuery)
    }
}

private fun resolveBrowserNetworkLogColor(
    entry: BrowserNetworkLogEntry,
    currentPageUrl: String
): Color {
    if (entry.isBlocked) {
        return Color(0xFFD93025)
    }

    val currentHost = runCatching { Uri.parse(currentPageUrl).host.orEmpty().lowercase() }.getOrDefault("")
    val requestHost = runCatching { Uri.parse(entry.url).host.orEmpty().lowercase() }.getOrDefault("")
    val sameSite = currentHost.isNotBlank() &&
        requestHost.isNotBlank() &&
        (
            currentHost == requestHost ||
                currentHost.endsWith(".$requestHost") ||
                requestHost.endsWith(".$currentHost")
            )

    return if (!sameSite && requestHost.isNotBlank()) {
        Color(0xFFD4B24C)
    } else {
        Color(0xFF111111)
    }
}

@Composable
private fun BrowserNetworkLogActionDialog(
    entry: BrowserNetworkLogEntry,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onBlock: () -> Unit,
    onOpenExternal: () -> Unit,
    onViewFullLink: () -> Unit
) {
    val isPlayable = UrlDetector.isVideo(entry.url, entry.headers) || UrlDetector.isAudio(entry.url, entry.headers)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(10.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "请选择操作",
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 14.dp)
                )
                BrowserNetworkLogActionRow("复制链接", onCopy)
                if (isPlayable) {
                    BrowserNetworkLogActionRow("播放资源", onPlay)
                }
                BrowserNetworkLogActionRow("下载资源", onDownload)
                BrowserNetworkLogActionRow("拦截网址", onBlock)
                BrowserNetworkLogActionRow("外部打开", onOpenExternal)
                BrowserNetworkLogActionRow("查看完整链接", onViewFullLink, drawDivider = false)
            }
        }
    }
}

@Composable
private fun BrowserNetworkLogActionRow(
    title: String,
    onClick: () -> Unit,
    drawDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = Color(0xFF202124),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 14.dp)
        )
        if (drawDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color(0xFFF1F3F4))
            )
        }
    }
}

@Composable
private fun BrowserNetworkLogFullLinkDialog(
    entry: BrowserNetworkLogEntry,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(10.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "完整链接",
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = entry.url,
                    color = Color(0xFF202124),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                    TextButton(onClick = onCopy) {
                        Text("复制")
                    }
                }
            }
        }
    }
}

private fun playBrowserNetworkLogEntry(context: Context, entry: BrowserNetworkLogEntry) {
    if (UrlDetector.isVideo(entry.url, entry.headers) || UrlDetector.isAudio(entry.url, entry.headers)) {
        BrowserPlaybackInteractor.play(
            context,
            DetectedVideo(
                url = entry.url,
                title = entry.title,
                pageUrl = entry.pageUrl,
                headers = entry.headers
            )
        )
    } else {
        Toast.makeText(context, "当前链接不是可播放的音视频资源", Toast.LENGTH_SHORT).show()
    }
}

private fun downloadBrowserNetworkLogEntry(context: Context, entry: BrowserNetworkLogEntry) {
    if (entry.url.isBlank()) {
        return
    }

    val contentDisposition = RemotePlaybackHeaders.get(entry.headers, "Content-Disposition")
    val mimeType = UrlDetector.getMimeTypeForFormat(
        UrlDetector.getDetectedResourceFormat(entry.url, entry.headers),
        entry.headers
    )
    val fileName = URLUtil.guessFileName(entry.url, contentDisposition, mimeType.ifBlank { null })
    val headers = RemotePlaybackHeaders.enrich(entry.headers, entry.pageUrl)

    requestDownloadWithPreferences(
        context = context,
        request = InternalDownloadRequest(
            url = entry.url,
            title = entry.title.takeIf { it.isNotBlank() } ?: fileName,
            fileName = fileName,
            mimeType = mimeType,
            description = "网络日志下载任务",
            sourcePageUrl = entry.pageUrl,
            sourcePageTitle = entry.title,
            mediaType = when (entry.category) {
                UrlDetector.NetworkCategory.VIDEO -> "video"
                UrlDetector.NetworkCategory.AUDIO -> "audio"
                UrlDetector.NetworkCategory.IMAGE -> "image"
                else -> ""
            },
            headers = headers
        )
    )
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

