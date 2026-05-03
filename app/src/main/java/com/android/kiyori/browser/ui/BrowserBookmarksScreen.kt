package com.android.kiyori.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.kiyori.R
import com.android.kiyori.browser.data.BrowserBookmarkRepository
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import com.android.kiyori.ui.compose.LocalKiyoriDrawerDragModifier

private data class BookmarkFolderRow(
    val folder: BrowserBookmarkFolder,
    val bookmarkCount: Int
)

private data class BookmarkBreadcrumbSegment(
    val folderId: Long?,
    val title: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserBookmarksScreen(
    folders: List<BrowserBookmarkFolder>,
    bookmarks: List<BrowserBookmarkItem>,
    onNavigateBack: () -> Unit,
    onCreateFolder: (String, Long?, Boolean) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onMoveFolder: (Long, Long?) -> Boolean,
    onDeleteFolder: (Long, Boolean) -> Unit,
    onSaveBookmark: (BrowserBookmarkDraft, Boolean) -> Unit,
    onUpdateBookmark: (Long, BrowserBookmarkDraft) -> Unit,
    onMoveBookmark: (Long, Long?) -> Boolean,
    onSetBookmarkSecret: (Long, Boolean) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteAllBookmarks: (Boolean) -> Unit,
    onDeleteBookmarksInFolder: (Long?, Boolean) -> Unit,
    onReorderFolder: (Long, Int) -> Boolean,
    onReorderBookmark: (Long, Int) -> Boolean,
    onExportBookmarks: (Long?, Boolean) -> String,
    onShareBookmarks: (Long?, Boolean) -> String,
    onImportBookmarks: (String, Boolean, Boolean) -> BrowserBookmarkRepository.ImportResult,
    onOpenBookmarkInBackground: (BrowserBookmarkItem) -> Boolean = { false },
    onOpenBookmarkInNewWindow: (BrowserBookmarkItem) -> Boolean = { false },
    onAddBookmarkToHomeNavigation: (BrowserBookmarkItem) -> Boolean = { false },
    onAddFolderToHomeNavigation: (Long) -> Int = { -1 },
    onOpenBookmark: (String) -> Unit
) {
    val drawerDragModifier = LocalKiyoriDrawerDragModifier.current
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    var addBookmarkDraft by remember { mutableStateOf<BrowserBookmarkDraft?>(null) }
    var editBookmark by remember { mutableStateOf<BrowserBookmarkItem?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf<BrowserBookmarkFolder?>(null) }
    var moveFolder by remember { mutableStateOf<BrowserBookmarkFolder?>(null) }
    var moveBookmark by remember { mutableStateOf<BrowserBookmarkItem?>(null) }
    var deleteFolder by remember { mutableStateOf<BrowserBookmarkFolder?>(null) }
    var deleteBookmark by remember { mutableStateOf<BrowserBookmarkItem?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteCurrentFolderBookmarksDialog by remember { mutableStateOf(false) }
    var deleteBookmarksTargetFolderId by remember { mutableStateOf<Long?>(null) }
    var manualSortMode by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("manual") }
    var secretMode by rememberSaveable { mutableStateOf(false) }
    var folderMenuTargetId by remember { mutableStateOf<Long?>(null) }
    var bookmarkMenuTargetId by remember { mutableStateOf<Long?>(null) }

    val visibleFolderIds = remember(folders, bookmarks, secretMode) {
        collectVisibleFolderIdsForMode(
            folders = folders,
            bookmarks = bookmarks,
            secret = secretMode
        )
    }
    LaunchedEffect(currentFolderId, visibleFolderIds) {
        if (currentFolderId != null && !visibleFolderIds.contains(currentFolderId)) {
            currentFolderId = null
            folderMenuTargetId = null
            bookmarkMenuTargetId = null
        }
    }
    val currentFolder = folders.firstOrNull { it.id == currentFolderId && visibleFolderIds.contains(it.id) }
    val currentParentId = currentFolder?.parentId
    val currentBreadcrumb = remember(folders, currentFolderId, visibleFolderIds) {
        buildBookmarkBreadcrumb(currentFolderId?.takeIf { visibleFolderIds.contains(it) }, folders)
    }
    val childFolders = remember(folders, currentFolderId, visibleFolderIds) {
        folders.filter { it.parentId == currentFolderId && visibleFolderIds.contains(it.id) }
    }
    val currentBookmarks = remember(bookmarks, currentFolderId) {
        bookmarks.filter { it.folderId == currentFolderId }
    }
    val normalizedSearchQuery = searchQuery.trim()
    val isSearching = normalizedSearchQuery.isNotBlank()
    val visibleFolders = remember(childFolders, folders, isSearching, visibleFolderIds) {
        if (isSearching) folders.filter { visibleFolderIds.contains(it.id) } else childFolders
    }
    val visibleBookmarks = remember(bookmarks, currentBookmarks, isSearching) {
        if (isSearching) bookmarks else currentBookmarks
    }
    val filteredFolders = remember(visibleFolders, bookmarks, folders, normalizedSearchQuery, secretMode, sortMode) {
        visibleFolders
            .filter {
                normalizedSearchQuery.isBlank() || it.title.contains(normalizedSearchQuery, ignoreCase = true)
            }
            .map { folder ->
                BookmarkFolderRow(
                    folder = folder,
                    bookmarkCount = countBookmarksForFolder(
                        folderId = folder.id,
                        bookmarks = bookmarks,
                        folders = folders,
                        secret = secretMode
                    )
                )
            }
            .let { rows ->
                when (sortMode) {
                    "title" -> rows.sortedBy { it.folder.title }
                    "time" -> rows.sortedByDescending { it.folder.createdAt }
                    else -> rows
                }
            }
    }
    val filteredBookmarks = remember(visibleBookmarks, normalizedSearchQuery, secretMode, sortMode) {
        visibleBookmarks.filter { bookmark ->
            bookmark.secret == secretMode && (
            normalizedSearchQuery.isBlank() ||
                bookmark.title.contains(normalizedSearchQuery, ignoreCase = true) ||
                bookmark.url.contains(normalizedSearchQuery, ignoreCase = true))
        }.let { items ->
            when (sortMode) {
                "title" -> items.sortedBy { it.title }
                "time" -> items.sortedByDescending { it.createdAt }
                else -> items
            }
        }
    }
    val folderOptions = remember(folders, visibleFolderIds) {
        folders.filter { visibleFolderIds.contains(it.id) }.map { folder ->
            BrowserBookmarkFolderOption(
                id = folder.id,
                title = buildBookmarkPath(folder.id, folders).joinToString(" / ")
            )
        }
    }
    val moveTargets = remember(folders, moveFolder, visibleFolderIds) {
        val invalidIds = moveFolder?.let { folder ->
            collectDescendantIds(folder.id, folders) + folder.id
        } ?: emptySet()
        buildList {
            add(null to "/")
            folders
                .filter { visibleFolderIds.contains(it.id) && !invalidIds.contains(it.id) }
                .forEach { folder ->
                    add(folder.id to buildBookmarkPath(folder.id, folders).joinToString(" / "))
                }
        }
    }
    val allMoveTargets = remember(folders, visibleFolderIds) {
        buildList {
            add(null to "/")
            folders.filter { visibleFolderIds.contains(it.id) }.forEach { folder ->
                add(folder.id to buildBookmarkPath(folder.id, folders).joinToString(" / "))
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun getClipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
    }

    fun shareText(title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun importBookmarkText(raw: String) {
        val result = onImportBookmarks(raw, false, secretMode)
        if (result.folderCount == 0 && result.bookmarkCount == 0 && result.skippedCount == 0) {
            Toast.makeText(context, "导入失败，请检查书签 JSON 内容", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "已导入 ${result.folderCount} 个文件夹、${result.bookmarkCount} 个书签，跳过 ${result.skippedCount} 个重复项",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
        }.getOrDefault("")
        importBookmarkText(raw)
    }

    fun leaveCurrentLayerOrSearch() {
        when {
            isSearching -> searchQuery = ""
            manualSortMode -> {
                manualSortMode = false
                folderMenuTargetId = null
                bookmarkMenuTargetId = null
            }
            currentFolderId != null -> {
                currentFolderId = currentParentId
                folderMenuTargetId = null
                bookmarkMenuTargetId = null
            }
            else -> onNavigateBack()
        }
    }

    BackHandler(enabled = isSearching || currentFolderId != null || manualSortMode) {
        leaveCurrentLayerOrSearch()
        folderMenuTargetId = null
        bookmarkMenuTargetId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的书签",
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.offset(x = (-6).dp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.size(52.dp),
                        onClick = { leaveCurrentLayerOrSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            modifier = Modifier.size(44.dp),
                            onClick = { topMenuExpanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多操作",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        BookmarkMenuPopup(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false },
                            width = 176.dp,
                            alignment = Alignment.TopEnd,
                            offsetX = (-2).dp,
                            offsetY = 44.dp
                        ) {
                            BookmarkTopMenuItem("新增书签") {
                                topMenuExpanded = false
                                addBookmarkDraft = BrowserBookmarkDraft(
                                    title = "",
                                    url = "",
                                    iconUrl = "",
                                    folderId = currentFolderId
                                )
                            }
                            BookmarkTopMenuItem("新建文件夹") {
                                topMenuExpanded = false
                                showNewFolderDialog = true
                            }
                            BookmarkTopMenuItem("排序方式") {
                                topMenuExpanded = false
                                showSortDialog = true
                            }
                            BookmarkTopMenuItem("拖拽排序") {
                                topMenuExpanded = false
                                manualSortMode = !manualSortMode
                                if (manualSortMode) {
                                    sortMode = "manual"
                                    searchQuery = ""
                                }
                                Toast.makeText(
                                    context,
                                    if (manualSortMode) "已开启排序模式，长按后可上移/下移" else "已关闭排序模式",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            BookmarkTopMenuItem("书签导出") {
                                topMenuExpanded = false
                                val exported = onExportBookmarks(currentFolderId, secretMode)
                                copyToClipboard("Kiyori bookmarks", exported)
                                shareText("书签导出", exported)
                            }
                            BookmarkTopMenuItem("分享该分组") {
                                topMenuExpanded = false
                                val exported = onShareBookmarks(currentFolderId, secretMode)
                                copyToClipboard("Kiyori hiker bookmarks", exported)
                                shareText("分享该分组", exported)
                            }
                            BookmarkTopMenuItem("从本地导入") {
                                topMenuExpanded = false
                                importFileLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "text/*",
                                        "application/octet-stream"
                                    )
                                )
                            }
                            BookmarkTopMenuItem("从剪贴板导入") {
                                topMenuExpanded = false
                                showImportDialog = true
                            }
                            BookmarkTopMenuItem("删除全部") {
                                topMenuExpanded = false
                                showDeleteAllDialog = true
                            }
                            BookmarkTopMenuItem("秘密空间") {
                                topMenuExpanded = false
                                secretMode = !secretMode
                                currentFolderId = null
                                searchQuery = ""
                                manualSortMode = false
                                bookmarkMenuTargetId = null
                                folderMenuTargetId = null
                                Toast.makeText(
                                    context,
                                    if (secretMode) "已进入秘密空间" else "已返回普通书签",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            BookmarkTopMenuItem("失效检测") {
                                topMenuExpanded = false
                                val invalidCount = filteredBookmarks.count {
                                    !it.url.startsWith("http://") && !it.url.startsWith("https://")
                                }
                                Toast.makeText(context, "已检测 ${filteredBookmarks.size} 个书签，发现 ${invalidCount} 个非网页链接", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier
                    .background(Color.White)
                    .height(64.dp)
                    .then(drawerDragModifier)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            BookmarkSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )
            if (isSearching) {
                Text(
                    text = "搜索结果",
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp)
                )
            } else {
                BookmarkBreadcrumbBar(
                    segments = currentBreadcrumb,
                    currentFolderId = currentFolderId,
                    onSelectFolder = { folderId ->
                        currentFolderId = folderId
                        folderMenuTargetId = null
                        bookmarkMenuTargetId = null
                        manualSortMode = false
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredFolders, key = { it.folder.id }) { row ->
                    BookmarkFolderItem(
                        row = row,
                        menuExpanded = folderMenuTargetId == row.folder.id,
                        onClick = {
                            currentFolderId = row.folder.id
                            searchQuery = ""
                            folderMenuTargetId = null
                            bookmarkMenuTargetId = null
                        },
                        onLongClick = {
                            folderMenuTargetId = row.folder.id
                            bookmarkMenuTargetId = null
                        },
                        onDismissMenu = {
                            folderMenuTargetId = null
                        },
                        onRename = {
                            folderMenuTargetId = null
                            renameFolder = row.folder
                        },
                        onMove = {
                            folderMenuTargetId = null
                            moveFolder = row.folder
                        },
                        onDelete = {
                            folderMenuTargetId = null
                            deleteFolder = row.folder
                        },
                        onSendToHome = {
                            folderMenuTargetId = null
                            val addedCount = onAddFolderToHomeNavigation(row.folder.id)
                            if (addedCount > 0) {
                                Toast.makeText(context, "已发送 ${addedCount} 个书签到主页导航", Toast.LENGTH_SHORT).show()
                            } else if (addedCount == 0) {
                                Toast.makeText(context, "文件夹内没有可发送的普通书签", Toast.LENGTH_SHORT).show()
                            } else {
                                val exported = onExportBookmarks(row.folder.id, secretMode)
                                copyToClipboard(row.folder.title, exported)
                                Toast.makeText(context, "文件夹书签已复制，可粘贴到主页或其它设备导入", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBatch = {
                            folderMenuTargetId = null
                            deleteBookmarksTargetFolderId = row.folder.id
                            showDeleteCurrentFolderBookmarksDialog = true
                        },
                        onMoveUp = {
                            folderMenuTargetId = null
                            if (!onReorderFolder(row.folder.id, -1)) {
                                Toast.makeText(context, "已经在最上方", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMoveDown = {
                            folderMenuTargetId = null
                            if (!onReorderFolder(row.folder.id, 1)) {
                                Toast.makeText(context, "已经在最下方", Toast.LENGTH_SHORT).show()
                            }
                        },
                        manualSortMode = manualSortMode
                    )
                }

                items(filteredBookmarks, key = { it.id }) { bookmark ->
                    BookmarkPageItem(
                        bookmark = bookmark,
                        onClick = { onOpenBookmark(bookmark.url) },
                        menuExpanded = bookmarkMenuTargetId == bookmark.id,
                        onLongClick = {
                            bookmarkMenuTargetId = bookmark.id
                            folderMenuTargetId = null
                        },
                        onDismissMenu = { bookmarkMenuTargetId = null },
                        onBackgroundOpen = {
                            bookmarkMenuTargetId = null
                            if (onOpenBookmarkInBackground(bookmark)) {
                                Toast.makeText(context, "已在后台打开", Toast.LENGTH_SHORT).show()
                            } else {
                                copyToClipboard(bookmark.title, bookmark.url)
                                Toast.makeText(context, "链接已复制，可稍后打开", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNewWindowOpen = {
                            bookmarkMenuTargetId = null
                            if (!onOpenBookmarkInNewWindow(bookmark)) {
                                onOpenBookmark(bookmark.url)
                            }
                        },
                        onEditBookmark = {
                            bookmarkMenuTargetId = null
                            editBookmark = bookmark
                        },
                        onDragSort = {
                            bookmarkMenuTargetId = null
                            manualSortMode = true
                            sortMode = "manual"
                            searchQuery = ""
                            Toast.makeText(context, "已开启排序模式，长按后可上移/下移", Toast.LENGTH_SHORT).show()
                        },
                        onAddToHome = {
                            bookmarkMenuTargetId = null
                            if (onAddBookmarkToHomeNavigation(bookmark)) {
                                Toast.makeText(context, "已添加到主页导航", Toast.LENGTH_SHORT).show()
                            } else {
                                copyToClipboard(bookmark.title, bookmark.url)
                                Toast.makeText(context, "书签链接已复制，可添加到主页导航", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteBookmark = {
                            bookmarkMenuTargetId = null
                            deleteBookmark = bookmark
                        },
                        onBatch = {
                            bookmarkMenuTargetId = null
                            onSetBookmarkSecret(bookmark.id, !bookmark.secret)
                            Toast.makeText(context, if (bookmark.secret) "已移出秘密空间" else "已移入秘密空间", Toast.LENGTH_SHORT).show()
                        },
                        onCopyShare = {
                            bookmarkMenuTargetId = null
                            val text = "${bookmark.title}\n${bookmark.url}"
                            copyToClipboard(bookmark.title, text)
                            shareText(bookmark.title, text)
                        },
                        onMove = {
                            bookmarkMenuTargetId = null
                            moveBookmark = bookmark
                        },
                        onMoveUp = {
                            bookmarkMenuTargetId = null
                            if (!onReorderBookmark(bookmark.id, -1)) {
                                Toast.makeText(context, "已经在最上方", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onMoveDown = {
                            bookmarkMenuTargetId = null
                            if (!onReorderBookmark(bookmark.id, 1)) {
                                Toast.makeText(context, "已经在最下方", Toast.LENGTH_SHORT).show()
                            }
                        },
                        manualSortMode = manualSortMode
                    )
                }
            }
        }
    }
        if (topMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { topMenuExpanded = false }
                    )
            )
        }
        if (manualSortMode && !topMenuExpanded) {
            BrowserBookmarkSortDoneButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                onClick = {
                    manualSortMode = false
                    folderMenuTargetId = null
                    bookmarkMenuTargetId = null
                }
            )
        }
    }
    if (addBookmarkDraft != null) {
        BrowserAddBookmarkDialog(
            title = "新建书签",
            initialDraft = addBookmarkDraft!!,
            folderOptions = folderOptions,
            onDismiss = { addBookmarkDraft = null },
            onConfirm = { draft ->
                if (draft.url.isBlank()) {
                    Toast.makeText(context, "书签链接不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserAddBookmarkDialog
                }
                onSaveBookmark(draft, secretMode)
                addBookmarkDraft = null
                Toast.makeText(context, "书签已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showNewFolderDialog) {
        BrowserSingleInputDialog(
            title = "新建文件夹",
            initialValue = "",
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { title ->
                if (title.isBlank()) {
                    Toast.makeText(context, "文件夹名称不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserSingleInputDialog
                }
                onCreateFolder(title, currentFolderId, secretMode)
                showNewFolderDialog = false
            }
        )
    }

    if (renameFolder != null) {
        BrowserSingleInputDialog(
            title = "重命名文件夹",
            initialValue = renameFolder!!.title,
            onDismiss = { renameFolder = null },
            onConfirm = { title ->
                if (title.isBlank()) {
                    Toast.makeText(context, "文件夹名称不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserSingleInputDialog
                }
                onRenameFolder(renameFolder!!.id, title)
                renameFolder = null
            }
        )
    }

    if (moveFolder != null) {
        BrowserSelectFolderDialog(
            options = moveTargets,
            onDismiss = { moveFolder = null },
            onSelect = { parentId ->
                val moved = onMoveFolder(moveFolder!!.id, parentId)
                moveFolder = null
                if (!moved) {
                    Toast.makeText(context, "不能移动到当前文件夹内部", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (editBookmark != null) {
        val bookmark = editBookmark!!
        BrowserAddBookmarkDialog(
            title = "编辑书签",
            initialDraft = BrowserBookmarkDraft(
                title = bookmark.title,
                url = bookmark.url,
                iconUrl = bookmark.iconUrl,
                folderId = bookmark.folderId
            ),
            folderOptions = folderOptions,
            onDismiss = { editBookmark = null },
            onConfirm = { draft ->
                if (draft.url.isBlank()) {
                    Toast.makeText(context, "书签链接不能为空", Toast.LENGTH_SHORT).show()
                    return@BrowserAddBookmarkDialog
                }
                onUpdateBookmark(bookmark.id, draft)
                editBookmark = null
                Toast.makeText(context, "书签已更新", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (moveBookmark != null) {
        BrowserSelectFolderDialog(
            options = allMoveTargets,
            onDismiss = { moveBookmark = null },
            onSelect = { folderId ->
                val moved = onMoveBookmark(moveBookmark!!.id, folderId)
                moveBookmark = null
                Toast.makeText(
                    context,
                    if (moved) "书签已移动" else "不能移动到其它空间的文件夹",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showImportDialog) {
        BrowserSingleInputDialog(
            title = "书签导入",
            initialValue = getClipboardText(),
            confirmText = "导入",
            onDismiss = { showImportDialog = false },
            onConfirm = { raw ->
                showImportDialog = false
                importBookmarkText(raw)
            }
        )
    }

    if (showSortDialog) {
        BrowserBookmarkOptionsDialog(
            title = "排序方式",
            options = listOf("手动排序", "标题排序", "时间排序"),
            onDismiss = { showSortDialog = false },
            onSelect = { option ->
                sortMode = when (option) {
                    "标题排序" -> "title"
                    "时间排序" -> "time"
                    else -> "manual"
                }
                manualSortMode = sortMode == "manual" && manualSortMode
                showSortDialog = false
            }
        )
    }

    if (showDeleteAllDialog) {
        BrowserBookmarkConfirmDialog(
            title = "删除全部",
            message = "确定删除全部书签和文件夹吗？此操作不可撤销。",
            onDismiss = { showDeleteAllDialog = false },
            onConfirm = {
                onDeleteAllBookmarks(secretMode)
                currentFolderId = null
                showDeleteAllDialog = false
                Toast.makeText(context, if (secretMode) "秘密书签已清空" else "普通书签已清空", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (deleteFolder != null) {
        val folder = deleteFolder!!
        BrowserBookmarkConfirmDialog(
            title = "删除文件夹",
            message = "确定删除“${folder.title}”和里面的全部书签吗？此操作不可撤销。",
            onDismiss = { deleteFolder = null },
            onConfirm = {
                val deletedIds = collectDescendantIds(folder.id, folders) + folder.id
                onDeleteFolder(folder.id, secretMode)
                if (currentFolderId != null && deletedIds.contains(currentFolderId)) {
                    currentFolderId = folder.parentId
                }
                deleteFolder = null
                Toast.makeText(context, "文件夹已删除", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (deleteBookmark != null) {
        val bookmark = deleteBookmark!!
        BrowserBookmarkConfirmDialog(
            title = "删除书签",
            message = "确定删除“${bookmark.title}”吗？",
            onDismiss = { deleteBookmark = null },
            onConfirm = {
                onDeleteBookmark(bookmark.id)
                deleteBookmark = null
                Toast.makeText(context, "书签已删除", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteCurrentFolderBookmarksDialog) {
        BrowserBookmarkConfirmDialog(
            title = "批量操作",
            message = "确定删除所选文件夹下的全部书签吗？子文件夹不会被删除。",
            onDismiss = {
                showDeleteCurrentFolderBookmarksDialog = false
                deleteBookmarksTargetFolderId = null
            },
            onConfirm = {
                onDeleteBookmarksInFolder(deleteBookmarksTargetFolderId, secretMode)
                showDeleteCurrentFolderBookmarksDialog = false
                deleteBookmarksTargetFolderId = null
                Toast.makeText(context, "当前文件夹书签已删除", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun BookmarkTopMenuItem(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color(0xFF202124),
            fontSize = 15.sp
        )
    }
}

@Composable
private fun BrowserBookmarkSortDoneButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFF202124),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "排序完成",
                color = Color(0xFF62C76B),
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BookmarkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .background(Color(0xFFF6F6F6), RoundedCornerShape(8.dp))
            .height(35.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF202124),
                fontSize = 12.sp
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isBlank()) {
                        Text(
                            text = "搜索书签标题、链接",
                            color = Color(0xFFC6C6C6),
                            fontSize = 12.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "清空搜索",
                    tint = Color(0xFF9AA0A6),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BookmarkBreadcrumbBar(
    segments: List<BookmarkBreadcrumbSegment>,
    currentFolderId: Long?,
    onSelectFolder: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            val isCurrent = segment.folderId == currentFolderId
            Text(
                text = segment.title,
                color = if (isCurrent) Color(0xFF374151) else Color(0xFF6B7280),
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = !isCurrent) {
                    onSelectFolder(segment.folderId)
                }
            )
            if (index != segments.lastIndex) {
                Text(
                    text = "  >  ",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkFolderItem(
    row: BookmarkFolderRow,
    menuExpanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onSendToHome: () -> Unit,
    onBatch: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    manualSortMode: Boolean
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder),
                    contentDescription = null,
                    tint = Color(0xFF9AA0A6),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp)
            ) {
                Text(
                    text = row.folder.title,
                    color = Color(0xFF202124),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${row.bookmarkCount}个书签",
                    color = Color(0xFF9AA0A6),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.BottomCenter)
                .padding(start = 64.dp)
                .background(Color(0xFFF1F3F4))
        )
        BookmarkMenuPopup(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            width = 156.dp,
            alignment = Alignment.TopEnd,
            offsetX = (-6).dp,
            offsetY = (-2).dp
        ) {
            if (manualSortMode) {
                BookmarkTopMenuItem("上移", onMoveUp)
                BookmarkTopMenuItem("下移", onMoveDown)
            }
            BookmarkTopMenuItem("重命名文件夹", onRename)
            BookmarkTopMenuItem("移动文件夹", onMove)
            BookmarkTopMenuItem("发送到主页", onSendToHome)
            BookmarkTopMenuItem("删除文件夹", onDelete)
            BookmarkTopMenuItem("批量操作", onBatch)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkPageItem(
    bookmark: BrowserBookmarkItem,
    onClick: () -> Unit,
    menuExpanded: Boolean,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onBackgroundOpen: () -> Unit,
    onNewWindowOpen: () -> Unit,
    onEditBookmark: () -> Unit,
    onDragSort: () -> Unit,
    onAddToHome: () -> Unit,
    onDeleteBookmark: () -> Unit,
    onBatch: () -> Unit,
    onCopyShare: () -> Unit,
    onMove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    manualSortMode: Boolean
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF7F9FC)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_kiyori_nav_globe),
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(
                    text = bookmark.title,
                    color = Color(0xFF202124),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.url,
                    color = Color(0xFF9AA0A6),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .align(Alignment.BottomCenter)
                .padding(start = 61.dp)
                .background(Color(0xFFF1F3F4))
        )
        BookmarkMenuPopup(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            width = 148.dp,
            alignment = Alignment.TopEnd,
            offsetX = (-4).dp,
            offsetY = (-6).dp
        ) {
            if (manualSortMode) {
                BookmarkTopMenuItem("上移", onMoveUp)
                BookmarkTopMenuItem("下移", onMoveDown)
            }
            BookmarkTopMenuItem("后台打开", onBackgroundOpen)
            BookmarkTopMenuItem("新窗口打开", onNewWindowOpen)
            BookmarkTopMenuItem("编辑书签", onEditBookmark)
            BookmarkTopMenuItem("移动书签", onMove)
            BookmarkTopMenuItem("拖拽排序", onDragSort)
            BookmarkTopMenuItem("加到主页", onAddToHome)
            BookmarkTopMenuItem("删除书签", onDeleteBookmark)
            BookmarkTopMenuItem("批量操作", onBatch)
            BookmarkTopMenuItem("复制分享", onCopyShare)
        }
    }
}

@Composable
private fun BookmarkMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    width: androidx.compose.ui.unit.Dp,
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return

    val density = LocalDensity.current
    val x = with(density) { offsetX.roundToPx() }
    val y = with(density) { offsetY.roundToPx() }
    val shape = RoundedCornerShape(18.dp)

    Popup(
        alignment = alignment,
        offset = IntOffset(x, y),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = width, max = width)
                .border(0.8.dp, Color(0xFFE1E4EA), shape),
            shape = shape,
            color = Color.White,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White, shape)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BrowserBookmarkOptionsDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 308.dp, max = 308.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = option,
                            color = Color(0xFF202124),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserBookmarkConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 308.dp, max = 308.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Text(
                    text = message,
                    color = Color(0xFF4B5563),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFF3F4F6))
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookmarkDialogButton(
                        text = "取消",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6))
                            .size(width = 0.5.dp, height = 54.dp)
                    )
                    BookmarkDialogButton(
                        text = "确定",
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkDialogButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF374151),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun BrowserSelectFolderDialog(
    options: List<Pair<Long?, String>>,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 308.dp, max = 308.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            ) {
                Text(
                    text = "选择目标文件夹",
                    color = Color(0xFF1F2937),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .clickable(onClick = { onSelect(option.first) })
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.second,
                            color = Color(0xFF202124),
                            fontSize = 16.sp
                        )
                    }
                    if (index != options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(horizontal = 18.dp)
                                .background(Color(0xFFF1F3F4))
                        )
                    }
                }
            }
        }
    }
}

private fun countBookmarksForFolder(
    folderId: Long,
    bookmarks: List<BrowserBookmarkItem>,
    folders: List<BrowserBookmarkFolder>,
    secret: Boolean
): Int {
    val descendantIds = collectDescendantIds(folderId, folders) + folderId
    return bookmarks.count { descendantIds.contains(it.folderId) && it.secret == secret }
}

private fun collectVisibleFolderIdsForMode(
    folders: List<BrowserBookmarkFolder>,
    bookmarks: List<BrowserBookmarkItem>,
    secret: Boolean
): Set<Long> {
    val folderMap = folders.associateBy { it.id }
    val visibleIds = mutableSetOf<Long>()
    folders
        .filter { it.secret == secret }
        .forEach { visibleIds += it.id }
    bookmarks
        .filter { it.secret == secret }
        .mapNotNull { it.folderId }
        .forEach { folderId ->
            var currentId: Long? = folderId
            while (currentId != null) {
                visibleIds += currentId
                currentId = folderMap[currentId]?.parentId
            }
        }
    return visibleIds
}

private fun collectDescendantIds(
    folderId: Long,
    folders: List<BrowserBookmarkFolder>
): Set<Long> {
    val descendants = mutableSetOf<Long>()
    fun walk(parentId: Long) {
        folders
            .filter { it.parentId == parentId }
            .forEach { child ->
                if (descendants.add(child.id)) {
                    walk(child.id)
                }
            }
    }
    walk(folderId)
    return descendants
}

private fun buildBookmarkPath(
    folderId: Long?,
    folders: List<BrowserBookmarkFolder>
): List<String> {
    return buildBookmarkBreadcrumb(folderId, folders).map { it.title }
}

private fun buildBookmarkBreadcrumb(
    folderId: Long?,
    folders: List<BrowserBookmarkFolder>
): List<BookmarkBreadcrumbSegment> {
    if (folderId == null) {
        return listOf(BookmarkBreadcrumbSegment(folderId = null, title = "根目录"))
    }

    val folderMap = folders.associateBy { it.id }
    val segments = mutableListOf<BookmarkBreadcrumbSegment>()
    var currentId = folderId
    while (currentId != null) {
        val folder = folderMap[currentId] ?: break
        segments += BookmarkBreadcrumbSegment(folderId = folder.id, title = folder.title)
        currentId = folder.parentId
    }
    return listOf(BookmarkBreadcrumbSegment(folderId = null, title = "根目录")) + segments.asReversed()
}
