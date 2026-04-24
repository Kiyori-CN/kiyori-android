package com.android.kiyori.browser.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.android.kiyori.browser.domain.BrowserBookmarkFolder
import com.android.kiyori.browser.domain.BrowserBookmarkFolderOption
import com.android.kiyori.browser.domain.BrowserBookmarkItem
import com.android.kiyori.ui.compose.LocalKiyoriDrawerDragModifier

private data class BookmarkFolderRow(
    val folder: BrowserBookmarkFolder,
    val bookmarkCount: Int
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserBookmarksScreen(
    folders: List<BrowserBookmarkFolder>,
    bookmarks: List<BrowserBookmarkItem>,
    onNavigateBack: () -> Unit,
    onCreateFolder: (String, Long?) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onMoveFolder: (Long, Long?) -> Boolean,
    onDeleteFolder: (Long) -> Unit,
    onSaveBookmark: (BrowserBookmarkDraft) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onOpenBookmark: (String) -> Unit
) {
    val drawerDragModifier = LocalKiyoriDrawerDragModifier.current
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    var addBookmarkDraft by remember { mutableStateOf<BrowserBookmarkDraft?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf<BrowserBookmarkFolder?>(null) }
    var moveFolder by remember { mutableStateOf<BrowserBookmarkFolder?>(null) }
    var folderMenuTargetId by remember { mutableStateOf<Long?>(null) }
    var bookmarkMenuTargetId by remember { mutableStateOf<Long?>(null) }

    val currentFolder = folders.firstOrNull { it.id == currentFolderId }
    val currentParentId = currentFolder?.parentId
    val currentPath = remember(folders, currentFolderId) {
        buildBookmarkPath(currentFolderId, folders).joinToString("  >  ")
    }
    val childFolders = remember(folders, currentFolderId) {
        folders.filter { it.parentId == currentFolderId }
    }
    val currentBookmarks = remember(bookmarks, currentFolderId) {
        bookmarks.filter { it.folderId == currentFolderId }
    }
    val filteredFolders = remember(childFolders, bookmarks, folders, searchQuery) {
        childFolders
            .filter {
                searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
            }
            .map { folder ->
                BookmarkFolderRow(
                    folder = folder,
                    bookmarkCount = countBookmarksForFolder(folder.id, bookmarks, folders)
                )
            }
    }
    val filteredBookmarks = remember(currentBookmarks, searchQuery) {
        currentBookmarks.filter { bookmark ->
            searchQuery.isBlank() ||
                bookmark.title.contains(searchQuery, ignoreCase = true) ||
                bookmark.url.contains(searchQuery, ignoreCase = true)
        }
    }
    val folderOptions = remember(folders) {
        folders.map { BrowserBookmarkFolderOption(id = it.id, title = it.title) }
    }
    val moveTargets = remember(folders, moveFolder) {
        val invalidIds = moveFolder?.let { folder ->
            collectDescendantIds(folder.id, folders) + folder.id
        } ?: emptySet()
        buildList {
            add(null to "/")
            folders
                .filterNot { invalidIds.contains(it.id) }
                .forEach { add(it.id to it.title) }
        }
    }

    BackHandler(enabled = currentFolderId != null) {
        currentFolderId = currentParentId
        folderMenuTargetId = null
        bookmarkMenuTargetId = null
    }

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
                        onClick = {
                            if (currentFolderId != null) {
                                currentFolderId = currentParentId
                                folderMenuTargetId = null
                                bookmarkMenuTargetId = null
                            } else {
                                onNavigateBack()
                            }
                        }
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
                            offsetY = 2.dp
                        ) {
                            BookmarkTopMenuItem("新建书签") {
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
                                Toast.makeText(context, "排序方式暂未实现", Toast.LENGTH_SHORT).show()
                            }
                            BookmarkTopMenuItem("拖拽排序") {
                                topMenuExpanded = false
                                Toast.makeText(context, "拖拽排序暂未实现", Toast.LENGTH_SHORT).show()
                            }
                            BookmarkTopMenuItem("书签导出") {
                                topMenuExpanded = false
                                Toast.makeText(context, "书签导出暂未实现", Toast.LENGTH_SHORT).show()
                            }
                            BookmarkTopMenuItem("书签导入") {
                                topMenuExpanded = false
                                Toast.makeText(context, "书签导入暂未实现", Toast.LENGTH_SHORT).show()
                            }
                            BookmarkTopMenuItem("秘密空间") {
                                topMenuExpanded = false
                                Toast.makeText(context, "秘密空间暂未实现", Toast.LENGTH_SHORT).show()
                            }
                            BookmarkTopMenuItem("失效检测") {
                                topMenuExpanded = false
                                Toast.makeText(context, "失效检测暂未实现", Toast.LENGTH_SHORT).show()
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
                onQueryChange = { searchQuery = it }
            )
            Text(
                text = currentPath,
                color = Color(0xFF6B7280),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 4.dp)
            )

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
                            onDeleteFolder(row.folder.id)
                            if (currentFolderId == row.folder.id) {
                                currentFolderId = row.folder.parentId
                            }
                        },
                        onSendToHome = {
                            folderMenuTargetId = null
                            Toast.makeText(context, "发送到主页暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onBatch = {
                            folderMenuTargetId = null
                            Toast.makeText(context, "批量操作暂未实现", Toast.LENGTH_SHORT).show()
                        }
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
                            Toast.makeText(context, "后台打开暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onNewWindowOpen = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "新窗口打开暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onEditBookmark = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "编辑书签暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onDragSort = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "拖拽排序暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onAddToHome = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "加到主页暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteBookmark = {
                            bookmarkMenuTargetId = null
                            onDeleteBookmark(bookmark.id)
                            Toast.makeText(context, "书签已删除", Toast.LENGTH_SHORT).show()
                        },
                        onBatch = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "批量操作暂未实现", Toast.LENGTH_SHORT).show()
                        },
                        onCopyShare = {
                            bookmarkMenuTargetId = null
                            Toast.makeText(context, "复制分享暂未实现", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
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
                onSaveBookmark(draft)
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
                onCreateFolder(title, currentFolderId)
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
private fun BookmarkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
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
            modifier = Modifier.fillMaxWidth(),
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
    onBatch: () -> Unit
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
    onCopyShare: () -> Unit
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
            BookmarkTopMenuItem("后台打开", onBackgroundOpen)
            BookmarkTopMenuItem("新窗口打开", onNewWindowOpen)
            BookmarkTopMenuItem("编辑书签", onEditBookmark)
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

    Popup(
        alignment = alignment,
        offset = IntOffset(x, y),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier.widthIn(min = width, max = width),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(18.dp))
            ) {
                content()
            }
        }
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
    folders: List<BrowserBookmarkFolder>
): Int {
    val descendantIds = collectDescendantIds(folderId, folders) + folderId
    return bookmarks.count { descendantIds.contains(it.folderId) }
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
    if (folderId == null) {
        return listOf("根目录")
    }

    val folderMap = folders.associateBy { it.id }
    val titles = mutableListOf<String>()
    var currentId = folderId
    while (currentId != null) {
        val folder = folderMap[currentId] ?: break
        titles += folder.title
        currentId = folder.parentId
    }
    return listOf("根目录") + titles.asReversed()
}
