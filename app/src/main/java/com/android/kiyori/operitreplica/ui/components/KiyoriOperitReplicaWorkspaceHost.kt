package com.android.kiyori.operitreplica.ui.components

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.kiyori.BuildConfig
import com.android.kiyori.browser.domain.BrowserSearchEngine
import com.android.kiyori.browser.security.BrowserSecurityPolicy
import com.android.kiyori.browser.web.BrowserWebViewFactory
import com.android.kiyori.operitreplica.model.OperitReplicaConversation
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val WORKSPACE_LANDING_URL = "https://workspace.kiyori.local/"
private const val WORKSPACE_PREVIEW_RELATIVE_PATH = "preview/index.html"
private const val WORKSPACE_MAX_FILE_PREVIEW_CHARS = 48_000
private const val REPLICA_WORKSPACE_SHOW_INLINE_FILE_BROWSER = false
private val REPLICA_WORKSPACE_EDITOR_SYMBOLS =
    listOf(
        "{", "}", "(", ")", "[", "]", "=", ".", ",", ";", ":",
        "\"", "'", "+", "-", "*", "/", "_", "<", ">", "&", "|",
        "!", "?"
    )
private const val REPLICA_WORKSPACE_EDITOR_GUTTER_INDEX = 0
private const val REPLICA_WORKSPACE_EDITOR_EDIT_TEXT_INDEX = 1
private val REPLICA_WORKSPACE_EDITOR_BACKGROUND = android.graphics.Color.parseColor("#FFFFFF")
private val REPLICA_WORKSPACE_EDITOR_GUTTER_BACKGROUND = android.graphics.Color.parseColor("#F8F8F8")
private val REPLICA_WORKSPACE_EDITOR_GUTTER_BORDER = android.graphics.Color.parseColor("#E5E5E5")
private val REPLICA_WORKSPACE_EDITOR_TEXT = android.graphics.Color.parseColor("#1F2328")
private val REPLICA_WORKSPACE_EDITOR_LINE_NUMBER = android.graphics.Color.parseColor("#8A8F98")
private val REPLICA_WORKSPACE_EDITOR_KEYWORD = android.graphics.Color.parseColor("#0000FF")
private val REPLICA_WORKSPACE_EDITOR_TYPE = android.graphics.Color.parseColor("#267F99")
private val REPLICA_WORKSPACE_EDITOR_STRING = android.graphics.Color.parseColor("#A31515")
private val REPLICA_WORKSPACE_EDITOR_COMMENT = android.graphics.Color.parseColor("#008000")
private val REPLICA_WORKSPACE_EDITOR_NUMBER = android.graphics.Color.parseColor("#098658")
private val REPLICA_WORKSPACE_EDITOR_MARKDOWN_ACCENT = android.graphics.Color.parseColor("#795E26")

private interface ReplicaWorkspaceEditorHandle {
    fun undo()
    fun redo()
    fun replaceAllText(newText: String)
}

private class ReplicaWorkspaceEditorController(
    private val editText: EditText,
    private val gutterView: TextView,
    initialValue: String,
    private var language: String,
    private var onValueChange: (String) -> Unit,
) : ReplicaWorkspaceEditorHandle {
    private val history = mutableListOf(initialValue)
    private var historyIndex = 0
    private var suppressCallbacks = false

    fun updateBindings(
        value: String,
        language: String,
        onValueChange: (String) -> Unit,
    ) {
        this.language = language
        this.onValueChange = onValueChange
        gutterView.text = buildReplicaWorkspaceLineNumbers(value)
        if (editText.text.toString() != value) {
            val selection = editText.selectionStart.coerceAtLeast(0).coerceAtMost(value.length)
            suppressCallbacks = true
            editText.setText(value)
            editText.setSelection(selection)
            suppressCallbacks = false
        }
        editText.editableText?.let { applyReplicaWorkspaceSyntaxHighlight(it, language) }
        gutterView.scrollTo(0, editText.scrollY)
    }

    fun onEditableChanged(editable: Editable?) {
        val newValue = editable?.toString().orEmpty()
        gutterView.text = buildReplicaWorkspaceLineNumbers(newValue)
        editable?.let { applyReplicaWorkspaceSyntaxHighlight(it, language) }
        if (suppressCallbacks) {
            return
        }
        recordHistory(newValue)
        onValueChange(newValue)
    }

    override fun undo() {
        if (historyIndex <= 0) {
            return
        }
        historyIndex -= 1
        applyProgrammaticValue(history[historyIndex], notifyChange = true)
    }

    override fun redo() {
        if (historyIndex >= history.lastIndex) {
            return
        }
        historyIndex += 1
        applyProgrammaticValue(history[historyIndex], notifyChange = true)
    }

    override fun replaceAllText(newText: String) {
        recordHistory(newText)
        applyProgrammaticValue(newText, notifyChange = true)
    }

    private fun recordHistory(newValue: String) {
        if (history.getOrNull(historyIndex) == newValue) {
            return
        }
        while (history.lastIndex > historyIndex) {
            history.removeAt(history.lastIndex)
        }
        history += newValue
        if (history.size > 100) {
            history.removeAt(0)
        }
        historyIndex = history.lastIndex
    }

    private fun applyProgrammaticValue(
        value: String,
        notifyChange: Boolean,
    ) {
        suppressCallbacks = true
        editText.setText(value)
        editText.setSelection(value.length)
        suppressCallbacks = false
        gutterView.text = buildReplicaWorkspaceLineNumbers(value)
        editText.editableText?.let { applyReplicaWorkspaceSyntaxHighlight(it, language) }
        if (notifyChange) {
            onValueChange(value)
        }
    }
}

private data class ReplicaWorkspaceEntry(
    val name: String,
    val absolutePath: String,
    val relativePath: String,
    val depth: Int,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
)

private data class ReplicaWorkspaceFilePreview(
    val relativePath: String,
    val extension: String,
    val lineNumbers: String,
    val content: String,
)

private data class ReplicaWorkspaceQuickPath(
    val label: String,
    val relativePath: String,
    val icon: ImageVector,
)

private enum class ReplicaWorkspaceCreateMode {
    FILE,
    FOLDER,
}

@Composable
internal fun KiyoriOperitReplicaWorkspaceInteractivePanel(
    activeConversation: OperitReplicaConversation?,
    messageCount: Int,
    onClose: () -> Unit,
    onOpenComputer: () -> Unit,
    onReloadWorkspace: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var workspaceWebView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by rememberSaveable { mutableStateOf("Workspace") }
    var currentUrl by rememberSaveable { mutableStateOf("") }
    var inputUrl by rememberSaveable { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var activeEditor by remember { mutableStateOf<ReplicaWorkspaceEditorHandle?>(null) }
    var isEditorFabExpanded by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    val workspaceRoot =
        remember(context, activeConversation?.id, activeConversation?.title, activeConversation?.preview, messageCount) {
            ensureReplicaWorkspaceRoot(
                context = context,
                conversationId = activeConversation?.id ?: "default",
                title = activeConversation?.title ?: "当前未选中会话",
                preview = activeConversation?.preview ?: "工作区会跟随当前聊天容器保活，隐藏时不销毁。",
                messageCount = messageCount,
            )
        }
    var editedFileContents by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(mapOf<String, String>()) }
    var previewModes by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(mapOf<String, Boolean>()) }
    var unsavedRelativePaths by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(emptySet<String>()) }
    var showFileManager by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(false) }
    var workspaceTreeVersion by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(0) }
    var showHiddenFiles by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(false) }
    var sortMode by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var pendingCreateMode by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf<ReplicaWorkspaceCreateMode?>(null) }
    var pendingCreateName by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf("") }
    var pendingCreateError by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf<String?>(null) }
    var contextMenuRelativePath by remember { mutableStateOf<String?>(null) }
    var pendingDeleteRelativePath by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf<String?>(null) }
    var currentDirectoryRelativePath by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf("") }
    val workspaceEntries =
        remember(workspaceRoot.absolutePath, workspaceTreeVersion, showHiddenFiles, sortMode) {
            buildReplicaWorkspaceEntries(
                workspaceRoot = workspaceRoot,
                showHiddenFiles = showHiddenFiles,
                sortMode = sortMode,
            )
        }
    val currentDirectory =
        remember(workspaceRoot.absolutePath, currentDirectoryRelativePath, workspaceTreeVersion) {
            resolveReplicaWorkspaceDirectory(
                workspaceRoot = workspaceRoot,
                relativePath = currentDirectoryRelativePath,
            )
        }
    val currentDirectoryEntries =
        remember(currentDirectory.absolutePath, workspaceRoot.absolutePath, workspaceTreeVersion, showHiddenFiles, sortMode) {
            buildReplicaWorkspaceDirectoryEntries(
                currentDirectory = currentDirectory,
                workspaceRoot = workspaceRoot,
                showHiddenFiles = showHiddenFiles,
                sortMode = sortMode,
            )
        }
    val quickPaths =
        remember(workspaceRoot.absolutePath, workspaceTreeVersion) {
            buildReplicaWorkspaceQuickPaths(workspaceRoot)
        }
    var selectedRelativePath by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf<String?>(null) }
    var openRelativePaths by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf(emptyList<String>()) }
    var pendingCloseRelativePath by rememberSaveable(workspaceRoot.absolutePath) { mutableStateOf<String?>(null) }
    val selectedEntry =
        remember(selectedRelativePath, workspaceEntries) {
            workspaceEntries.firstOrNull { it.relativePath == selectedRelativePath && !it.isDirectory }
        }
    val selectedPreview =
        remember(selectedEntry?.absolutePath, workspaceRoot.absolutePath) {
            selectedEntry?.absolutePath?.let { path -> buildReplicaWorkspaceFilePreview(file = File(path), root = workspaceRoot) }
        }
    val selectedRelativePathSafe = selectedRelativePath
    val selectedFileText =
        remember(selectedPreview?.relativePath, editedFileContents) {
            selectedPreview?.relativePath?.let { relativePath ->
                editedFileContents[relativePath] ?: selectedPreview.content
            }
        }
    val landingHtml =
        remember(workspaceRoot.absolutePath, activeConversation?.id, activeConversation?.title, activeConversation?.preview, messageCount) {
            buildWorkspaceLandingHtml(
                title = activeConversation?.title ?: "当前未选中会话",
                preview = activeConversation?.preview ?: "工作区会跟随当前聊天容器保活，隐藏时不销毁。",
                messageCount = messageCount,
            )
        }

    fun syncNavigationState(webView: WebView?) {
        canGoBack = webView?.canGoBack() == true
        canGoForward = webView?.canGoForward() == true
    }

    fun loadLandingPage() {
        val previewFile = File(workspaceRoot, WORKSPACE_PREVIEW_RELATIVE_PATH)
        val previewHtml = previewFile.takeIf { it.exists() }?.readText() ?: landingHtml
        workspaceWebView?.loadDataWithBaseURL(
            WORKSPACE_LANDING_URL,
            previewHtml,
            "text/html",
            "utf-8",
            null,
        )
        pageTitle = "Workspace"
        currentUrl = ""
        inputUrl = ""
        isLoading = false
        progress = 100
        syncNavigationState(workspaceWebView)
    }

    fun submitAddress() {
        val normalizedUrl = BrowserSecurityPolicy.normalizeUserInput(inputUrl, BrowserSearchEngine.DEFAULT)
        if (normalizedUrl == null || normalizedUrl == BrowserSecurityPolicy.BLANK_HOME_URL) {
            selectedRelativePath = null
            loadLandingPage()
            return
        }
        selectedRelativePath = null
        workspaceWebView?.loadUrl(normalizedUrl)
    }

    fun closeOpenFile(relativePath: String) {
        val nextOpenPaths = openRelativePaths.filterNot { it == relativePath }
        openRelativePaths = nextOpenPaths
        editedFileContents = editedFileContents - relativePath
        previewModes = previewModes - relativePath
        unsavedRelativePaths = unsavedRelativePaths - relativePath
        if (selectedRelativePath == relativePath) {
            selectedRelativePath = nextOpenPaths.lastOrNull()
            if (selectedRelativePath == null) {
                loadLandingPage()
            }
        }
    }

    fun refreshWorkspaceTree() {
        workspaceTreeVersion += 1
    }

    fun openWorkspaceDirectory(directory: File) {
        currentDirectoryRelativePath =
            directory.relativeToOrSelf(workspaceRoot)
                .invariantSeparatorsPath
                .takeUnless { it == "." }
                .orEmpty()
    }

    fun openWorkspaceFile(file: File) {
        file.parentFile?.let(::openWorkspaceDirectory)
        val relativePath = file.relativeTo(workspaceRoot).invariantSeparatorsPath
        openRelativePaths =
            if (relativePath in openRelativePaths) {
                openRelativePaths
            } else {
                openRelativePaths + relativePath
            }
        if (relativePath !in previewModes) {
            previewModes = previewModes + (relativePath to shouldReplicaWorkspaceStartInPreview(relativePath))
        }
        selectedRelativePath = relativePath
    }

    fun createWorkspaceEntry(mode: ReplicaWorkspaceCreateMode) {
        val cleanedName = pendingCreateName.trim()
        if (cleanedName.isBlank()) {
            pendingCreateError = "名称不能为空"
            return
        }
        if (cleanedName.contains('/') || cleanedName.contains('\\')) {
            pendingCreateError = "名称不能包含路径分隔符"
            return
        }
        val parentDirectory = currentDirectory
        val target = File(parentDirectory, cleanedName)
        if (target.exists()) {
            pendingCreateError = "同名文件或文件夹已存在"
            return
        }
        when (mode) {
            ReplicaWorkspaceCreateMode.FILE -> target.writeText("")
            ReplicaWorkspaceCreateMode.FOLDER -> target.mkdirs()
        }
        refreshWorkspaceTree()
        pendingCreateMode = null
        pendingCreateName = ""
        pendingCreateError = null
        if (mode == ReplicaWorkspaceCreateMode.FILE) {
            openWorkspaceFile(target)
        }
    }

    fun deleteWorkspaceEntry(relativePath: String) {
        val entry = workspaceEntries.firstOrNull { it.relativePath == relativePath } ?: return
        closeOpenFile(relativePath)
        File(entry.absolutePath).delete()
        refreshWorkspaceTree()
    }

    DisposableEffect(lifecycleOwner, workspaceWebView) {
        val observer =
            LifecycleEventObserver { _, event ->
                val webView = workspaceWebView ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_RESUME -> webView.onResume()
                    Lifecycle.Event.ON_PAUSE -> webView.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            workspaceWebView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = null
                removeAllViews()
                destroy()
            }
            workspaceWebView = null
        }
    }

    DisposableEffect(activeEditor, isImeVisible) {
        if (isImeVisible) {
            isEditorFabExpanded = false
        }
        onDispose { }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Workspace",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                )
                OperitSecondaryIconButton(
                    icon = Icons.Default.DesktopWindows,
                    contentDescription = "computer",
                    onClick = onOpenComputer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OperitSecondaryIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "close workspace",
                    onClick = onClose,
                )
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF7F8FB),
                border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = activeConversation?.title ?: "当前未选中会话",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                    )
                    Text(
                        text = pageTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4568B2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            if (currentUrl.isBlank()) {
                                "工作区根目录：${workspaceRoot.name} | 已接入 $messageCount 条消息上下文"
                            } else {
                                currentUrl
                            },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF667085),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF4F6FB),
                border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OperitSecondaryIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "workspace back",
                            onClick = {
                                if (canGoBack) {
                                    selectedRelativePath = null
                                    workspaceWebView?.goBack()
                                    syncNavigationState(workspaceWebView)
                                }
                            },
                        )
                        OperitSecondaryIconButton(
                            icon = Icons.Default.ArrowForward,
                            contentDescription = "workspace forward",
                            onClick = {
                                if (canGoForward) {
                                    selectedRelativePath = null
                                    workspaceWebView?.goForward()
                                    syncNavigationState(workspaceWebView)
                                }
                            },
                        )
                        OperitSecondaryIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "workspace refresh",
                            onClick = {
                                if (selectedRelativePath == null) {
                                    if (currentUrl.isBlank()) {
                                        loadLandingPage()
                                    } else {
                                        workspaceWebView?.reload()
                                    }
                                } else {
                                    selectedRelativePath = selectedRelativePath
                                }
                            },
                        )
                        OperitSecondaryIconButton(
                            icon = Icons.Default.Code,
                            contentDescription = "rebuild workspace host",
                            onClick = onReloadWorkspace,
                        )
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp),
                            placeholder = { Text("搜索或输入网址", fontSize = 13.sp) },
                        )
                        OperitSecondaryIconButton(
                            icon = Icons.Default.Description,
                            contentDescription = "workspace preview home",
                            selected = selectedRelativePath == null,
                            onClick = {
                                selectedRelativePath = null
                                loadLandingPage()
                            },
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().height(540.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDCE3F0)),
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (REPLICA_WORKSPACE_SHOW_INLINE_FILE_BROWSER) {
                            Surface(
                                modifier = Modifier.width(252.dp).fillMaxSize(),
                                color = Color(0xFFF7F8FB),
                                border = BorderStroke(1.dp, Color(0xFFE6EAF2)),
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text =
                                                    buildReplicaWorkspaceDisplayPath(
                                                        relativePath = currentDirectoryRelativePath,
                                                    ),
                                                fontSize = 11.sp,
                                                color = Color(0xFF667085),
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            ReplicaWorkspaceHeaderIconButton(
                                                icon = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "toggle hidden files",
                                                selected = showHiddenFiles,
                                                onClick = { showHiddenFiles = !showHiddenFiles },
                                            )
                                            Box {
                                                ReplicaWorkspaceHeaderIconButton(
                                                    icon = Icons.Default.Sort,
                                                    contentDescription = "sort workspace files",
                                                    selected = showSortMenu,
                                                    onClick = { showSortMenu = true },
                                                )
                                                DropdownMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false },
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("按名称排序${if (sortMode == 0) " · 当前" else ""}") },
                                                        onClick = {
                                                            sortMode = 0
                                                            showSortMenu = false
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("按大小排序${if (sortMode == 1) " · 当前" else ""}") },
                                                        onClick = {
                                                            sortMode = 1
                                                            showSortMenu = false
                                                        },
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("按修改时间排序${if (sortMode == 2) " · 当前" else ""}") },
                                                        onClick = {
                                                            sortMode = 2
                                                            showSortMenu = false
                                                        },
                                                    )
                                                }
                                            }
                                            ReplicaWorkspaceHeaderIconButton(
                                                icon = Icons.Default.Add,
                                                contentDescription = "new workspace file",
                                                onClick = {
                                                    pendingCreateMode = ReplicaWorkspaceCreateMode.FILE
                                                    pendingCreateName = ""
                                                    pendingCreateError = null
                                                },
                                            )
                                            ReplicaWorkspaceHeaderIconButton(
                                                icon = Icons.Default.CreateNewFolder,
                                                contentDescription = "new workspace folder",
                                                onClick = {
                                                    pendingCreateMode = ReplicaWorkspaceCreateMode.FOLDER
                                                    pendingCreateName = ""
                                                    pendingCreateError = null
                                                },
                                            )
                                            ReplicaWorkspaceHeaderIconButton(
                                                icon = Icons.Default.Refresh,
                                                contentDescription = "refresh workspace tree",
                                                onClick = ::refreshWorkspaceTree,
                                            )
                                        }
                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            quickPaths.forEach { quickPath ->
                                                ReplicaWorkspaceQuickPathChip(
                                                    label = quickPath.label,
                                                    icon = quickPath.icon,
                                                    selected =
                                                        currentDirectoryRelativePath == quickPath.relativePath ||
                                                            (
                                                                quickPath.relativePath.isNotBlank() &&
                                                                    currentDirectoryRelativePath.startsWith("${quickPath.relativePath}/")
                                                                ),
                                                    onClick = { currentDirectoryRelativePath = quickPath.relativePath },
                                                )
                                            }
                                        }
                                        if (currentDirectoryRelativePath.isNotBlank()) {
                                            ReplicaWorkspaceDirectoryBar(
                                                relativePath = currentDirectoryRelativePath,
                                                onNavigateToDirectory = { relativePath ->
                                                    currentDirectoryRelativePath = relativePath
                                                },
                                            )
                                        }
                                    }
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color(0xFFE6EAF2)),
                                    )
                                    LazyColumn(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        if (currentDirectory != workspaceRoot) {
                                            item {
                                                ReplicaWorkspaceEntryRow(
                                                    entry =
                                                        ReplicaWorkspaceEntry(
                                                            name = "..",
                                                            absolutePath = currentDirectory.parentFile?.absolutePath ?: workspaceRoot.absolutePath,
                                                            relativePath =
                                                                currentDirectory.parentFile
                                                                    ?.relativeToOrSelf(workspaceRoot)
                                                                    ?.invariantSeparatorsPath
                                                                    ?.takeUnless { it == "." }
                                                                    .orEmpty(),
                                                            depth = 0,
                                                            isDirectory = true,
                                                            size = 0L,
                                                            lastModified = 0L,
                                                        ),
                                                    onClick = {
                                                        currentDirectory.parentFile?.let(::openWorkspaceDirectory)
                                                    },
                                                )
                                            }
                                        }
                                        items(
                                            items = currentDirectoryEntries,
                                            key = { entry -> entry.absolutePath },
                                        ) { entry ->
                                            Box {
                                                ReplicaWorkspaceEntryRow(
                                                    entry = entry,
                                                    onClick = {
                                                        if (entry.isDirectory) {
                                                            openWorkspaceDirectory(File(entry.absolutePath))
                                                        } else {
                                                            openWorkspaceFile(File(entry.absolutePath))
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (!entry.isDirectory) {
                                                            contextMenuRelativePath = entry.relativePath
                                                        }
                                                    },
                                                )
                                                DropdownMenu(
                                                    expanded = contextMenuRelativePath == entry.relativePath,
                                                    onDismissRequest = { contextMenuRelativePath = null },
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("删除文件") },
                                                        onClick = {
                                                            contextMenuRelativePath = null
                                                            pendingDeleteRelativePath = entry.relativePath
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    ReplicaWorkspaceTabsBar(
                                        openRelativePaths = openRelativePaths,
                                        selectedRelativePath = selectedRelativePath,
                                        unsavedRelativePaths = unsavedRelativePaths,
                                        showPreviewToggle =
                                            selectedPreview != null &&
                                                (isReplicaWorkspaceMarkdown(selectedPreview.extension) || isReplicaWorkspaceHtml(selectedPreview.extension)),
                                        previewMode = previewModes[selectedRelativePathSafe].let { it ?: false },
                                        onTogglePreview = {
                                            if (selectedRelativePathSafe != null) {
                                                previewModes =
                                                    previewModes + (selectedRelativePathSafe to !(previewModes[selectedRelativePathSafe] ?: false))
                                            }
                                        },
                                        showSaveAction =
                                            selectedPreview != null &&
                                                isReplicaWorkspaceTextEditable(selectedPreview.extension) &&
                                                selectedRelativePathSafe in unsavedRelativePaths,
                                        onSaveAction = {
                                            if (selectedRelativePathSafe != null && selectedEntry != null) {
                                                val contentToSave = editedFileContents[selectedRelativePathSafe] ?: selectedPreview?.content.orEmpty()
                                                File(selectedEntry.absolutePath).writeText(contentToSave)
                                                editedFileContents = editedFileContents + (selectedRelativePathSafe to contentToSave)
                                                unsavedRelativePaths = unsavedRelativePaths - selectedRelativePathSafe
                                            }
                                        },
                                        onSelectPreview = {
                                            selectedRelativePath = null
                                            loadLandingPage()
                                        },
                                        onSelectFile = { relativePath ->
                                            selectedRelativePath = relativePath
                                        },
                                        onCloseFile = { relativePath ->
                                            if (relativePath in unsavedRelativePaths) {
                                                pendingCloseRelativePath = relativePath
                                            } else {
                                                closeOpenFile(relativePath)
                                            }
                                        },
                                    )

                                    Box(
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                    ) {
                                        if (selectedPreview == null) {
                                            AndroidView(
                                                modifier = Modifier.fillMaxSize(),
                                                factory = { webContext ->
                                                    WebView(webContext).apply {
                                                        BrowserWebViewFactory.configure(
                                                            webView = this,
                                                            geolocationEnabled = false,
                                                        )
                                                        webChromeClient =
                                                            object : WebChromeClient() {
                                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                                    progress = newProgress.coerceIn(0, 100)
                                                                    isLoading = progress in 1..99
                                                                }

                                                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                                                    pageTitle = title?.takeIf { it.isNotBlank() } ?: "Workspace"
                                                                }
                                                            }
                                                        webViewClient =
                                                            object : WebViewClient() {
                                                                override fun onPageStarted(
                                                                    view: WebView?,
                                                                    url: String?,
                                                                    favicon: Bitmap?,
                                                                ) {
                                                                    currentUrl = url.orEmpty().takeUnless { it == WORKSPACE_LANDING_URL }.orEmpty()
                                                                    inputUrl = currentUrl
                                                                    isLoading = true
                                                                    syncNavigationState(view)
                                                                }

                                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                                    currentUrl = url.orEmpty().takeUnless { it == WORKSPACE_LANDING_URL }.orEmpty()
                                                                    inputUrl = currentUrl
                                                                    isLoading = false
                                                                    progress = 100
                                                                    syncNavigationState(view)
                                                                }
                                                            }
                                                        workspaceWebView = this
                                                        loadLandingPage()
                                                    }
                                                },
                                                update = { webView ->
                                                    workspaceWebView = webView
                                                },
                                            )

                                            if (isLoading) {
                                                LinearProgressIndicator(
                                                    progress = progress.coerceIn(0, 100) / 100f,
                                                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                                )
                                            }

                                            if (workspaceWebView == null) {
                                                Column(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .background(Color.White)
                                                            .padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = Color(0xFF6177B2),
                                                        modifier = Modifier.size(28.dp),
                                                        strokeWidth = 3.dp,
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = "正在打开 Workspace",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF111111),
                                                    )
                                                }
                                            }
                                        } else {
                                            ReplicaWorkspaceFilePreviewPane(
                                                workspaceRoot = workspaceRoot,
                                                preview = selectedPreview,
                                                text = selectedFileText ?: selectedPreview.content,
                                                previewMode = previewModes[selectedRelativePathSafe].let { it ?: false },
                                                editorRef = { editor -> activeEditor = editor },
                                                onTextChange = { updatedText ->
                                                    if (selectedRelativePathSafe != null) {
                                                        editedFileContents = editedFileContents + (selectedRelativePathSafe to updatedText)
                                                        unsavedRelativePaths = unsavedRelativePaths + selectedRelativePathSafe
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            if (showFileManager) {
                ReplicaWorkspaceFileManagerSheet(
                    workspaceRoot = workspaceRoot,
                    currentDirectory = currentDirectory,
                    currentDirectoryRelativePath = currentDirectoryRelativePath,
                    currentDirectoryEntries = currentDirectoryEntries,
                    quickPaths = quickPaths,
                    showHiddenFiles = showHiddenFiles,
                    showSortMenu = showSortMenu,
                    sortMode = sortMode,
                    contextMenuRelativePath = contextMenuRelativePath,
                    onDismiss = {
                        showFileManager = false
                        contextMenuRelativePath = null
                        showSortMenu = false
                    },
                    onToggleHiddenFiles = { showHiddenFiles = !showHiddenFiles },
                    onShowSortMenuChange = { showSortMenu = it },
                    onSelectSortMode = { mode ->
                        sortMode = mode
                        showSortMenu = false
                    },
                    onCreateFile = {
                        pendingCreateMode = ReplicaWorkspaceCreateMode.FILE
                        pendingCreateName = ""
                        pendingCreateError = null
                    },
                    onCreateFolder = {
                        pendingCreateMode = ReplicaWorkspaceCreateMode.FOLDER
                        pendingCreateName = ""
                        pendingCreateError = null
                    },
                    onRefresh = ::refreshWorkspaceTree,
                    onOpenQuickPath = { relativePath -> currentDirectoryRelativePath = relativePath },
                    onNavigateToDirectory = { relativePath -> currentDirectoryRelativePath = relativePath },
                    onOpenDirectory = { directory -> openWorkspaceDirectory(directory) },
                    onOpenFile = { file ->
                        openWorkspaceFile(file)
                        showFileManager = false
                        contextMenuRelativePath = null
                    },
                    onShowEntryMenu = { relativePath -> contextMenuRelativePath = relativePath },
                    onHideEntryMenu = { contextMenuRelativePath = null },
                    onDeleteEntry = { relativePath ->
                        contextMenuRelativePath = null
                        pendingDeleteRelativePath = relativePath
                    },
                )
            }
            if (!isImeVisible) {
                val canFormatCurrentFile =
                    selectedPreview?.let { preview ->
                        detectReplicaWorkspaceLanguage(preview.relativePath.substringAfterLast('/')).lowercase() in
                            setOf("javascript", "js", "css", "html", "htm")
                    } == true
                ReplicaWorkspaceEditorFabMenu(
                    isExpanded = isEditorFabExpanded,
                    onToggle = { isEditorFabExpanded = !isEditorFabExpanded },
                    onUndoClick = {
                        activeEditor?.undo()
                        isEditorFabExpanded = false
                    },
                    onRedoClick = {
                        activeEditor?.redo()
                        isEditorFabExpanded = false
                    },
                    onFilesClick = {
                        showFileManager = true
                        isEditorFabExpanded = false
                    },
                    onExportClick = {
                        val exported =
                            runCatching {
                                exportReplicaWorkspace(context, workspaceRoot)
                            }.getOrDefault(false)
                        Toast
                            .makeText(
                                context,
                                if (exported) "已打开工作区导出分享" else "导出工作区失败",
                                Toast.LENGTH_SHORT,
                            ).show()
                        isEditorFabExpanded = false
                    },
                    canFormat = canFormatCurrentFile,
                    onFormatClick = {
                        val preview = selectedPreview
                        val relativePath = selectedRelativePathSafe
                        if (preview != null && relativePath != null) {
                            val language =
                                detectReplicaWorkspaceLanguage(
                                    preview.relativePath.substringAfterLast('/'),
                                )
                            val currentContent = selectedFileText ?: preview.content
                            val formattedContent =
                                ReplicaWorkspaceCodeFormatter.format(
                                    code = currentContent,
                                    language = language,
                                )
                            editedFileContents = editedFileContents + (relativePath to formattedContent)
                            unsavedRelativePaths = unsavedRelativePaths + relativePath
                            activeEditor?.replaceAllText(formattedContent)
                        }
                        isEditorFabExpanded = false
                    },
                )
            }
        }
    }

    if (pendingCloseRelativePath != null) {
        val fileName = pendingCloseRelativePath?.substringAfterLast('/').orEmpty()
        AlertDialog(
            onDismissRequest = { pendingCloseRelativePath = null },
            title = { Text("未保存更改") },
            text = { Text("文件 $fileName 已修改。是否保存后再关闭？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val relativePath = pendingCloseRelativePath
                        val entry = workspaceEntries.firstOrNull { it.relativePath == relativePath }
                        if (relativePath != null && entry != null) {
                            val contentToSave = editedFileContents[relativePath]
                                ?: buildReplicaWorkspaceFilePreview(File(entry.absolutePath), workspaceRoot)?.content
                                ?: ""
                            File(entry.absolutePath).writeText(contentToSave)
                            editedFileContents = editedFileContents + (relativePath to contentToSave)
                            unsavedRelativePaths = unsavedRelativePaths - relativePath
                            closeOpenFile(relativePath)
                        }
                        pendingCloseRelativePath = null
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { pendingCloseRelativePath = null }) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            pendingCloseRelativePath?.let(::closeOpenFile)
                            pendingCloseRelativePath = null
                        },
                    ) {
                        Text("不保存")
                    }
                }
            },
        )
    }

    if (pendingCreateMode != null) {
        val targetLabel =
            when (pendingCreateMode) {
                ReplicaWorkspaceCreateMode.FILE -> "创建新文件"
                ReplicaWorkspaceCreateMode.FOLDER -> "创建文件夹"
                null -> ""
            }
        val targetDirectory = currentDirectory
        AlertDialog(
            onDismissRequest = {
                pendingCreateMode = null
                pendingCreateName = ""
                pendingCreateError = null
            },
            title = { Text(targetLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "位置：${targetDirectory.relativeToOrSelf(workspaceRoot).invariantSeparatorsPath.ifBlank { "/" }}",
                        fontSize = 12.sp,
                        color = Color(0xFF667085),
                    )
                    OutlinedTextField(
                        value = pendingCreateName,
                        onValueChange = {
                            pendingCreateName = it
                            pendingCreateError = null
                        },
                        singleLine = true,
                        placeholder = {
                            Text(if (pendingCreateMode == ReplicaWorkspaceCreateMode.FILE) "输入文件名" else "输入文件夹名")
                        },
                        supportingText = pendingCreateError?.let { error ->
                            { Text(error, color = Color(0xFFB42318)) }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCreateMode?.let(::createWorkspaceEntry)
                    },
                ) {
                    Text(if (pendingCreateMode == ReplicaWorkspaceCreateMode.FILE) "创建文件" else "创建文件夹")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingCreateMode = null
                        pendingCreateName = ""
                        pendingCreateError = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (pendingDeleteRelativePath != null) {
        val fileName = pendingDeleteRelativePath?.substringAfterLast('/').orEmpty()
        val hasUnsavedChanges = pendingDeleteRelativePath in unsavedRelativePaths
        AlertDialog(
            onDismissRequest = { pendingDeleteRelativePath = null },
            title = { Text("删除") },
            text = {
                Text(
                    if (hasUnsavedChanges) {
                        "文件 $fileName 有未保存修改。删除后无法恢复。"
                    } else {
                        "删除后无法恢复：$fileName"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteRelativePath?.let(::deleteWorkspaceEntry)
                        pendingDeleteRelativePath = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRelativePath = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ReplicaWorkspaceTabsBar(
    openRelativePaths: List<String>,
    selectedRelativePath: String?,
    unsavedRelativePaths: Set<String>,
    showPreviewToggle: Boolean,
    previewMode: Boolean,
    onTogglePreview: () -> Unit,
    showSaveAction: Boolean,
    onSaveAction: () -> Unit,
    onSelectPreview: () -> Unit,
    onSelectFile: (String) -> Unit,
    onCloseFile: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F8FB))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReplicaWorkspaceTabChip(
                title = "预览",
                icon = Icons.Default.Visibility,
                selected = selectedRelativePath == null,
                onClick = onSelectPreview,
            )
            openRelativePaths.forEach { relativePath ->
                ReplicaWorkspaceTabChip(
                    title = relativePath.substringAfterLast('/'),
                    icon = resolveReplicaWorkspaceFileIcon(relativePath.substringAfterLast('.', "").lowercase()),
                    selected = selectedRelativePath == relativePath,
                    isUnsaved = relativePath in unsavedRelativePaths,
                    onClick = { onSelectFile(relativePath) },
                    onClose = { onCloseFile(relativePath) },
                )
            }
        }
        if (showPreviewToggle) {
            ReplicaWorkspaceToolbarIconButton(
                icon = if (previewMode) Icons.Default.Edit else Icons.Default.Visibility,
                contentDescription = "toggle file preview",
                selected = previewMode,
                onClick = onTogglePreview,
            )
        }
        if (showSaveAction) {
            ReplicaWorkspaceToolbarIconButton(
                icon = Icons.Default.Save,
                contentDescription = "save workspace file",
                selected = true,
                onClick = onSaveAction,
            )
        }
    }
}

@Composable
private fun ReplicaWorkspaceTabChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    isUnsaved: Boolean = false,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(if (selected) Color.White else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF35579B) else Color(0xFF667085),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color(0xFF111111) else Color(0xFF475467),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onClose != null) {
                Box(
                    modifier =
                        Modifier
                            .size(22.dp)
                            .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isUnsaved) Icons.Default.FiberManualRecord else Icons.Default.Close,
                        contentDescription = null,
                        tint =
                            if (selected) {
                                Color(0xFF35579B)
                            } else {
                                Color(0xFF667085).copy(alpha = if (isUnsaved) 0.9f else 0.7f)
                            },
                        modifier = Modifier.size(if (isUnsaved) 8.dp else 14.dp),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (selected) Color(0xFF35579B) else Color.Transparent),
        )
    }
}

private fun resolveReplicaWorkspaceListEntryIcon(entry: ReplicaWorkspaceEntry): ImageVector {
    if (entry.name == "..") {
        return Icons.Default.FolderOpen
    }
    if (entry.isDirectory) {
        return Icons.Default.Folder
    }
    return resolveReplicaWorkspaceFileIcon(entry.name.substringAfterLast('.', "").lowercase())
}

private fun exportReplicaWorkspace(
    context: Context,
    workspaceRoot: File,
): Boolean {
    if (!workspaceRoot.exists() || !workspaceRoot.isDirectory) {
        return false
    }
    val archiveFile = createReplicaWorkspaceArchive(context, workspaceRoot)
    val archiveUri =
        FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            archiveFile,
        )
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, archiveUri)
            putExtra(Intent.EXTRA_SUBJECT, "${workspaceRoot.name}.zip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooserIntent =
        Intent.createChooser(shareIntent, "导出工作区").apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    context.startActivity(chooserIntent)
    return true
}

private fun createReplicaWorkspaceArchive(
    context: Context,
    workspaceRoot: File,
): File {
    val exportDirectory = File(context.cacheDir, "workspace-exports").apply { mkdirs() }
    val archiveFile =
        File(
            exportDirectory,
            "${sanitizeReplicaWorkspaceArchiveName(workspaceRoot.name)}-${System.currentTimeMillis()}.zip",
        )
    ZipOutputStream(BufferedOutputStream(FileOutputStream(archiveFile))).use { zipOutputStream ->
        addReplicaWorkspaceFileToZip(
            zipOutputStream = zipOutputStream,
            file = workspaceRoot,
            entryPath = "${workspaceRoot.name}/",
        )
    }
    return archiveFile
}

private fun addReplicaWorkspaceFileToZip(
    zipOutputStream: ZipOutputStream,
    file: File,
    entryPath: String,
) {
    if (file.isDirectory) {
        val normalizedDirectoryPath = if (entryPath.endsWith('/')) entryPath else "$entryPath/"
        zipOutputStream.putNextEntry(ZipEntry(normalizedDirectoryPath))
        zipOutputStream.closeEntry()
        file.listFiles()
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.forEach { child ->
                addReplicaWorkspaceFileToZip(
                    zipOutputStream = zipOutputStream,
                    file = child,
                    entryPath = normalizedDirectoryPath + child.name,
                )
            }
        return
    }

    zipOutputStream.putNextEntry(ZipEntry(entryPath))
    file.inputStream().use { inputStream ->
        inputStream.copyTo(zipOutputStream)
    }
    zipOutputStream.closeEntry()
}

private fun sanitizeReplicaWorkspaceArchiveName(name: String): String {
    return name
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "workspace" }
}

@Composable
private fun ReplicaWorkspaceDirectoryBar(
    relativePath: String,
    onNavigateToDirectory: (String) -> Unit,
) {
    val breadcrumbSegments =
        remember(relativePath) {
            buildReplicaWorkspaceBreadcrumbs(relativePath)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReplicaWorkspacePathChip(
            label = "Workspace",
            icon = Icons.Default.Folder,
            selected = breadcrumbSegments.isEmpty(),
            onClick = { onNavigateToDirectory("") },
        )
        breadcrumbSegments.forEach { segment ->
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF98A2B3),
                modifier = Modifier.size(12.dp),
            )
            ReplicaWorkspacePathChip(
                label = segment.label,
                icon = Icons.Default.Folder,
                selected = segment == breadcrumbSegments.lastOrNull(),
                onClick = { onNavigateToDirectory(segment.relativePath) },
            )
        }
    }
}

@Composable
private fun ReplicaWorkspaceFileManagerSheet(
    workspaceRoot: File,
    currentDirectory: File,
    currentDirectoryRelativePath: String,
    currentDirectoryEntries: List<ReplicaWorkspaceEntry>,
    quickPaths: List<ReplicaWorkspaceQuickPath>,
    showHiddenFiles: Boolean,
    showSortMenu: Boolean,
    sortMode: Int,
    contextMenuRelativePath: String?,
    onDismiss: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSelectSortMode: (Int) -> Unit,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onRefresh: () -> Unit,
    onOpenQuickPath: (String) -> Unit,
    onNavigateToDirectory: (String) -> Unit,
    onOpenDirectory: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    onShowEntryMenu: (String) -> Unit,
    onHideEntryMenu: () -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onDismiss),
        )
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "文件浏览器",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111111),
                        modifier = Modifier.weight(1f),
                    )
                    ReplicaWorkspaceToolbarIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "close file manager",
                        onClick = onDismiss,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE6EAF2)),
                )
                ReplicaWorkspaceFileBrowserPanel(
                    workspaceRoot = workspaceRoot,
                    currentDirectory = currentDirectory,
                    currentDirectoryRelativePath = currentDirectoryRelativePath,
                    currentDirectoryEntries = currentDirectoryEntries,
                    quickPaths = quickPaths,
                    showHiddenFiles = showHiddenFiles,
                    showSortMenu = showSortMenu,
                    sortMode = sortMode,
                    contextMenuRelativePath = contextMenuRelativePath,
                    onToggleHiddenFiles = onToggleHiddenFiles,
                    onShowSortMenuChange = onShowSortMenuChange,
                    onSelectSortMode = onSelectSortMode,
                    onCreateFile = onCreateFile,
                    onCreateFolder = onCreateFolder,
                    onRefresh = onRefresh,
                    onOpenQuickPath = onOpenQuickPath,
                    onNavigateToDirectory = onNavigateToDirectory,
                    onOpenDirectory = onOpenDirectory,
                    onOpenFile = onOpenFile,
                    onShowEntryMenu = onShowEntryMenu,
                    onHideEntryMenu = onHideEntryMenu,
                    onDeleteEntry = onDeleteEntry,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReplicaWorkspaceFileBrowserPanel(
    workspaceRoot: File,
    currentDirectory: File,
    currentDirectoryRelativePath: String,
    currentDirectoryEntries: List<ReplicaWorkspaceEntry>,
    quickPaths: List<ReplicaWorkspaceQuickPath>,
    showHiddenFiles: Boolean,
    showSortMenu: Boolean,
    sortMode: Int,
    contextMenuRelativePath: String?,
    onToggleHiddenFiles: () -> Unit,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSelectSortMode: (Int) -> Unit,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onRefresh: () -> Unit,
    onOpenQuickPath: (String) -> Unit,
    onNavigateToDirectory: (String) -> Unit,
    onOpenDirectory: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    onShowEntryMenu: (String) -> Unit,
    onHideEntryMenu: () -> Unit,
    onDeleteEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.White),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = buildReplicaWorkspaceDisplayPath(relativePath = currentDirectoryRelativePath),
                    fontSize = 11.sp,
                    color = Color(0xFF667085),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ReplicaWorkspaceHeaderIconButton(
                    icon = if (showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "toggle hidden files",
                    selected = showHiddenFiles,
                    onClick = onToggleHiddenFiles,
                )
                Box {
                    ReplicaWorkspaceHeaderIconButton(
                        icon = Icons.Default.Sort,
                        contentDescription = "sort workspace files",
                        selected = showSortMenu,
                        onClick = { onShowSortMenuChange(true) },
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { onShowSortMenuChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("按名称排序${if (sortMode == 0) " · 当前" else ""}") },
                            onClick = { onSelectSortMode(0) },
                        )
                        DropdownMenuItem(
                            text = { Text("按大小排序${if (sortMode == 1) " · 当前" else ""}") },
                            onClick = { onSelectSortMode(1) },
                        )
                        DropdownMenuItem(
                            text = { Text("按修改时间排序${if (sortMode == 2) " · 当前" else ""}") },
                            onClick = { onSelectSortMode(2) },
                        )
                    }
                }
                ReplicaWorkspaceHeaderIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "new workspace file",
                    onClick = onCreateFile,
                )
                ReplicaWorkspaceHeaderIconButton(
                    icon = Icons.Default.CreateNewFolder,
                    contentDescription = "new workspace folder",
                    onClick = onCreateFolder,
                )
                ReplicaWorkspaceHeaderIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "refresh workspace tree",
                    onClick = onRefresh,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickPaths.forEach { quickPath ->
                    ReplicaWorkspaceQuickPathChip(
                        label = quickPath.label,
                        icon = quickPath.icon,
                        selected =
                            currentDirectoryRelativePath == quickPath.relativePath ||
                                (
                                    quickPath.relativePath.isNotBlank() &&
                                        currentDirectoryRelativePath.startsWith("${quickPath.relativePath}/")
                                    ),
                        onClick = { onOpenQuickPath(quickPath.relativePath) },
                    )
                }
            }
            if (currentDirectoryRelativePath.isNotBlank()) {
                ReplicaWorkspaceDirectoryBar(
                    relativePath = currentDirectoryRelativePath,
                    onNavigateToDirectory = onNavigateToDirectory,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE6EAF2)),
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            if (currentDirectory != workspaceRoot) {
                item {
                    ReplicaWorkspaceEntryRow(
                        entry =
                            ReplicaWorkspaceEntry(
                                name = "..",
                                absolutePath = currentDirectory.parentFile?.absolutePath ?: workspaceRoot.absolutePath,
                                relativePath =
                                    currentDirectory.parentFile
                                        ?.relativeToOrSelf(workspaceRoot)
                                        ?.invariantSeparatorsPath
                                        ?.takeUnless { it == "." }
                                        .orEmpty(),
                                depth = 0,
                                isDirectory = true,
                                size = 0L,
                                lastModified = 0L,
                            ),
                        onClick = {
                            currentDirectory.parentFile?.let(onOpenDirectory)
                        },
                    )
                }
            }
            items(
                items = currentDirectoryEntries,
                key = { entry -> entry.absolutePath },
            ) { entry ->
                Box {
                    ReplicaWorkspaceEntryRow(
                        entry = entry,
                        onClick = {
                            if (entry.isDirectory) {
                                onOpenDirectory(File(entry.absolutePath))
                            } else {
                                onOpenFile(File(entry.absolutePath))
                            }
                        },
                        onLongClick = {
                            if (!entry.isDirectory) {
                                onShowEntryMenu(entry.relativePath)
                            }
                        },
                    )
                    DropdownMenu(
                        expanded = contextMenuRelativePath == entry.relativePath,
                        onDismissRequest = onHideEntryMenu,
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除文件") },
                            onClick = { onDeleteEntry(entry.relativePath) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplicaWorkspaceQuickPathChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier =
            Modifier.border(
                width = 1.dp,
                color = if (selected) Color(0xFFB9C8EE) else Color(0xFFE0E7F2),
                shape = RoundedCornerShape(999.dp),
            ),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color(0xFF35579B) else Color(0xFF667085),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = Color.White,
                selectedContainerColor = Color(0xFFEAF1FF),
                selectedLabelColor = Color(0xFF35579B),
                labelColor = Color(0xFF667085),
            ),
    )
}

@Composable
private fun ReplicaWorkspacePathChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor =
        when {
            emphasized -> Color.White
            selected -> Color(0xFFEAF1FF)
            else -> Color.White
        }
    val borderColor =
        when {
            emphasized -> Color(0xFFB7C7EB)
            selected -> Color(0xFFB9C8EE)
            else -> Color(0xFFE0E7F2)
        }
    val contentColor =
        when {
            emphasized -> Color(0xFF35579B)
            selected -> Color(0xFF35579B)
            else -> Color(0xFF475467)
        }
    val labelColor =
        when {
            emphasized -> Color(0xFF111111)
            selected -> Color(0xFF35579B)
            else -> Color(0xFF475467)
        }
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected || emphasized) FontWeight.SemiBold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReplicaWorkspaceHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) Color(0xFFEAF1FF) else Color.Transparent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF35579B) else Color(0xFF667085),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ReplicaWorkspaceToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF35579B) else Color(0xFF475467),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ReplicaWorkspaceEditorFabMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onFilesClick: () -> Unit,
    onExportClick: () -> Unit,
    canFormat: Boolean,
    onFormatClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (isExpanded) {
                    ReplicaWorkspaceFabMenuItem(
                        icon = Icons.Default.Undo,
                        text = "撤销",
                        onClick = onUndoClick,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReplicaWorkspaceFabMenuItem(
                        icon = Icons.Default.Redo,
                        text = "重做",
                        onClick = onRedoClick,
                    )
                    if (canFormat) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReplicaWorkspaceFabMenuItem(
                            icon = Icons.Default.AutoFixHigh,
                            text = "格式化",
                            onClick = onFormatClick,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ReplicaWorkspaceFabMenuItem(
                        icon = Icons.Default.Folder,
                        text = "æ–‡ä»¶",
                        onClick = onFilesClick,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReplicaWorkspaceFabMenuItem(
                        icon = Icons.Outlined.Upload,
                        text = "导出",
                        onClick = onExportClick,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                FloatingActionButton(
                    onClick = onToggle,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                        contentDescription = if (isExpanded) "关闭菜单" else "打开菜单",
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplicaWorkspaceFabMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp,
            modifier = Modifier.clickable(onClick = onClick),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
            )
        }
    }
}

private fun detectReplicaWorkspaceLanguage(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "js", "mjs", "jsx" -> "javascript"
        "ts", "tsx" -> "typescript"
        "html", "htm", "xhtml" -> "html"
        "css", "scss", "sass", "less" -> "css"
        "xml", "svg", "xsd", "xsl" -> "xml"
        "json" -> "json"
        "md", "markdown" -> "markdown"
        "py", "pyw", "pyc" -> "python"
        "c", "cpp", "cc", "h", "hpp" -> "cpp"
        "cs" -> "csharp"
        "php", "phtml" -> "php"
        "rb", "rbw" -> "ruby"
        "go" -> "go"
        "rs" -> "rust"
        "swift" -> "swift"
        "dart" -> "dart"
        "sh", "bash", "zsh" -> "shell"
        "sql" -> "sql"
        "yml", "yaml" -> "yaml"
        else -> "text"
    }
}

private fun applyReplicaWorkspaceSyntaxHighlight(
    editable: Editable,
    language: String,
) {
    editable
        .getSpans(0, editable.length, ForegroundColorSpan::class.java)
        .forEach(editable::removeSpan)
    val content = editable.toString()
    if (content.isEmpty()) {
        return
    }

    fun applyColor(regex: Regex, color: Int) {
        regex.findAll(content).forEach { match ->
            editable.setSpan(
                ForegroundColorSpan(color),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    when (language) {
        "markdown" -> {
            applyColor(Regex("(?m)^#{1,6}\\s.*$"), REPLICA_WORKSPACE_EDITOR_KEYWORD)
            applyColor(Regex("(?m)^>\\s.*$"), REPLICA_WORKSPACE_EDITOR_COMMENT)
            applyColor(Regex("(?m)^\\s*[-*+]\\s.*$"), REPLICA_WORKSPACE_EDITOR_MARKDOWN_ACCENT)
            applyColor(Regex("`[^`]+`"), REPLICA_WORKSPACE_EDITOR_MARKDOWN_ACCENT)
            applyColor(Regex("```[\\s\\S]*?```"), REPLICA_WORKSPACE_EDITOR_STRING)
        }
        "html", "xml" -> {
            applyColor(Regex("</?[A-Za-z][^>]*?>"), REPLICA_WORKSPACE_EDITOR_KEYWORD)
            applyColor(Regex("<!--([\\s\\S]*?)-->"), REPLICA_WORKSPACE_EDITOR_COMMENT)
            applyColor(Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"), REPLICA_WORKSPACE_EDITOR_STRING)
        }
        "json" -> {
            applyColor(Regex("\"(?:\\\\.|[^\"\\\\])*\"\\s*:"), REPLICA_WORKSPACE_EDITOR_TYPE)
            applyColor(Regex(":\\s*\"(?:\\\\.|[^\"\\\\])*\""), REPLICA_WORKSPACE_EDITOR_STRING)
            applyColor(Regex("\\b(true|false|null)\\b"), REPLICA_WORKSPACE_EDITOR_KEYWORD)
            applyColor(Regex("\\b\\d+(?:\\.\\d+)?\\b"), REPLICA_WORKSPACE_EDITOR_NUMBER)
        }
        else -> {
            val slashCommentLanguages =
                setOf(
                    "kotlin",
                    "java",
                    "javascript",
                    "typescript",
                    "cpp",
                    "csharp",
                    "go",
                    "rust",
                    "swift",
                    "dart",
                    "css",
                    "sql",
                    "php",
                )
            val hashCommentLanguages = setOf("python", "ruby", "shell", "yaml")
            val keywordPattern =
                when (language) {
                    "kotlin" -> "\\b(package|import|class|interface|object|fun|val|var|if|else|when|return|for|while|try|catch|finally|null|true|false|in|is|as|suspend|data|sealed|private|public|internal|protected|override)\\b"
                    "java" -> "\\b(package|import|class|interface|enum|public|private|protected|static|final|void|new|return|if|else|switch|case|try|catch|finally|for|while|null|true|false|extends|implements)\\b"
                    "javascript", "typescript" -> "\\b(import|export|class|function|const|let|var|return|if|else|switch|case|for|while|async|await|new|true|false|null|undefined|interface|type|extends)\\b"
                    "python" -> "\\b(import|from|class|def|return|if|elif|else|for|while|try|except|finally|with|as|pass|None|True|False|lambda)\\b"
                    "dart" -> "\\b(import|class|enum|extension|mixin|void|final|var|const|return|if|else|switch|case|for|while|async|await|true|false|null)\\b"
                    "shell" -> "\\b(if|then|else|fi|for|do|done|case|esac|function|in)\\b"
                    "sql" -> "\\b(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|JOIN|LEFT|RIGHT|INNER|OUTER|GROUP|ORDER|BY|HAVING|LIMIT|AND|OR|NOT|NULL|VALUES|INTO|SET|AS)\\b"
                    else -> null
                }
            if (language in slashCommentLanguages) {
                applyColor(Regex("//.*?$", setOf(RegexOption.MULTILINE)), REPLICA_WORKSPACE_EDITOR_COMMENT)
                applyColor(Regex("/\\*[\\s\\S]*?\\*/"), REPLICA_WORKSPACE_EDITOR_COMMENT)
            }
            if (language in hashCommentLanguages) {
                applyColor(Regex("#.*?$", setOf(RegexOption.MULTILINE)), REPLICA_WORKSPACE_EDITOR_COMMENT)
            }
            applyColor(Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"), REPLICA_WORKSPACE_EDITOR_STRING)
            applyColor(Regex("\\b\\d+(?:\\.\\d+)?\\b"), REPLICA_WORKSPACE_EDITOR_NUMBER)
            keywordPattern?.let { applyColor(Regex(it), REPLICA_WORKSPACE_EDITOR_KEYWORD) }
            if (language in setOf("kotlin", "java", "typescript", "csharp")) {
                applyColor(Regex("\\b[A-Z][A-Za-z0-9_]*\\b"), REPLICA_WORKSPACE_EDITOR_TYPE)
            }
        }
    }
}

@Composable
private fun ReplicaWorkspaceCodeEditorPane(
    value: String,
    fileName: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    editorRef: ((ReplicaWorkspaceEditorHandle?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val language = remember(fileName) { detectReplicaWorkspaceLanguage(fileName) }
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val latestEditorRef = rememberUpdatedState(editorRef)
    val contentPaddingPx = with(density) { 14.dp.roundToPx() }
    val editTextStartPaddingPx = with(density) { 12.dp.roundToPx() }
    val gutterWidthPx = with(density) { 52.dp.roundToPx() }
    val gutterEndPaddingPx = with(density) { 8.dp.roundToPx() }
    var activeEditText by remember { mutableStateOf<EditText?>(null) }

    DisposableEffect(editorRef) {
        onDispose {
            latestEditorRef.value?.invoke(null)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
    ) {
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(android.graphics.Color.WHITE)

                    val gutterView =
                        TextView(context).apply {
                            setBackgroundColor(REPLICA_WORKSPACE_EDITOR_GUTTER_BACKGROUND)
                            setTextColor(REPLICA_WORKSPACE_EDITOR_LINE_NUMBER)
                            typeface = Typeface.MONOSPACE
                            gravity = Gravity.TOP or Gravity.END
                            setPadding(0, contentPaddingPx, gutterEndPaddingPx, contentPaddingPx)
                            setLineSpacing(0f, 1.35f)
                            textSize = 14f
                            text = buildReplicaWorkspaceLineNumbers(value)
                        }

                    val editText =
                        EditText(context).apply {
                            activeEditText = this
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setTextColor(REPLICA_WORKSPACE_EDITOR_TEXT)
                            setHintTextColor(REPLICA_WORKSPACE_EDITOR_LINE_NUMBER)
                            typeface = Typeface.MONOSPACE
                            gravity = Gravity.TOP or Gravity.START
                            setPadding(editTextStartPaddingPx, contentPaddingPx, contentPaddingPx, contentPaddingPx)
                            setLineSpacing(0f, 1.35f)
                            isVerticalScrollBarEnabled = true
                            overScrollMode = EditText.OVER_SCROLL_IF_CONTENT_SCROLLS
                            setHorizontallyScrolling(true)
                            isHorizontalScrollBarEnabled = true
                            inputType =
                                InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                            textSize = 12f
                            setText(value)
                            if (readOnly) {
                                isFocusable = true
                                isFocusableInTouchMode = true
                                isCursorVisible = false
                                keyListener = null
                                setTextIsSelectable(true)
                            }
                            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                                gutterView.scrollTo(0, scrollY)
                            }
                            val controller =
                                ReplicaWorkspaceEditorController(
                                    editText = this,
                                    gutterView = gutterView,
                                    initialValue = value,
                                    language = language,
                                    onValueChange = { latestOnValueChange.value(it) },
                                )
                            addTextChangedListener(
                                object : TextWatcher {
                                    override fun beforeTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        count: Int,
                                        after: Int,
                                    ) = Unit

                                    override fun onTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        before: Int,
                                        count: Int,
                                    ) = Unit

                                    override fun afterTextChanged(s: Editable?) {
                                        controller.onEditableChanged(s)
                                    }
                                },
                            )
                            tag = controller
                            latestEditorRef.value?.invoke(if (readOnly) null else controller)
                            editableText?.let { applyReplicaWorkspaceSyntaxHighlight(it, language) }
                        }

                    addView(
                        gutterView,
                        LinearLayout.LayoutParams(gutterWidthPx, LinearLayout.LayoutParams.MATCH_PARENT),
                    )
                    addView(
                        editText,
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f),
                    )
                }
            },
            update = { container ->
                val gutterView = container.getChildAt(REPLICA_WORKSPACE_EDITOR_GUTTER_INDEX) as TextView
                val editText = container.getChildAt(REPLICA_WORKSPACE_EDITOR_EDIT_TEXT_INDEX) as EditText
                val controller = editText.tag as ReplicaWorkspaceEditorController
                activeEditText = editText
                controller.updateBindings(
                    value = value,
                    language = language,
                    onValueChange = { latestOnValueChange.value(it) },
                )
                latestEditorRef.value?.invoke(if (readOnly) null else controller)
            },
        )
        if (!readOnly) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                color = Color(0xFFF8F8F8),
                contentColor = Color(0xFF1F2328),
                shadowElevation = 2.dp,
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(REPLICA_WORKSPACE_EDITOR_SYMBOLS) { symbol ->
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFE5E5E5),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        activeEditText?.let { editText ->
                                            val start = editText.selectionStart.coerceAtLeast(0)
                                            val end = editText.selectionEnd.coerceAtLeast(0)
                                            editText.text.replace(
                                                minOf(start, end),
                                                maxOf(start, end),
                                                symbol,
                                            )
                                            editText.setSelection(minOf(start, end) + symbol.length)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = symbol,
                                color = Color(0xFF1F2328),
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplicaWorkspaceEntryRow(
    entry: ReplicaWorkspaceEntry,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .combinedClickable(
                    enabled = true,
                    onClick = onClick,
                    onLongClick = { onLongClick?.invoke() },
                )
                .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = resolveReplicaWorkspaceListEntryIcon(entry),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint =
                if (entry.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReplicaWorkspaceFilePreviewPane(
    workspaceRoot: File,
    preview: ReplicaWorkspaceFilePreview,
    text: String,
    previewMode: Boolean,
    editorRef: (ReplicaWorkspaceEditorHandle?) -> Unit,
    onTextChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val isEditable = isReplicaWorkspaceTextEditable(preview.extension)
        if (previewMode) {
            when {
                isReplicaWorkspaceImage(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                WORKSPACE_LANDING_URL,
                                buildReplicaWorkspaceAssetPreviewHtml(
                                    title = preview.relativePath.substringAfterLast('/'),
                                    assetUrl = "file://${File(workspaceRoot, preview.relativePath).absolutePath}",
                                    tag = "img",
                                ),
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                isReplicaWorkspaceAudio(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                WORKSPACE_LANDING_URL,
                                buildReplicaWorkspaceAssetPreviewHtml(
                                    title = preview.relativePath.substringAfterLast('/'),
                                    assetUrl = "file://${File(workspaceRoot, preview.relativePath).absolutePath}",
                                    tag = "audio",
                                ),
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                isReplicaWorkspaceVideo(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                WORKSPACE_LANDING_URL,
                                buildReplicaWorkspaceAssetPreviewHtml(
                                    title = preview.relativePath.substringAfterLast('/'),
                                    assetUrl = "file://${File(workspaceRoot, preview.relativePath).absolutePath}",
                                    tag = "video",
                                ),
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                isReplicaWorkspaceDocument(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                WORKSPACE_LANDING_URL,
                                buildReplicaWorkspaceDocumentPreviewHtml(
                                    title = preview.relativePath.substringAfterLast('/'),
                                    extension = preview.extension,
                                    assetUrl = "file://${File(workspaceRoot, preview.relativePath).absolutePath}",
                                ),
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                isReplicaWorkspaceMarkdown(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                WORKSPACE_LANDING_URL,
                                buildReplicaWorkspaceMarkdownPreviewHtml(
                                    title = preview.relativePath.substringAfterLast('/'),
                                    markdown = text,
                                ),
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                isReplicaWorkspaceHtml(preview.extension) -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webContext ->
                            WebView(webContext).apply {
                                BrowserWebViewFactory.configure(
                                    webView = this,
                                    geolocationEnabled = false,
                                )
                            }
                        },
                        update = { webView ->
                            val parentDirectoryRelativePath =
                                preview.relativePath.substringBeforeLast('/', "")
                            val baseDirectory =
                                File(workspaceRoot, parentDirectoryRelativePath)
                                    .takeIf { it.exists() && it.isDirectory }
                                    ?: workspaceRoot
                            webView.loadDataWithBaseURL(
                                "file://${baseDirectory.absolutePath}/",
                                text,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        },
                    )
                }
                else -> {
                    ReplicaWorkspaceCodeEditorPane(
                        value = text,
                        fileName = preview.relativePath.substringAfterLast('/'),
                        onValueChange = {},
                        modifier = Modifier.fillMaxSize(),
                        readOnly = true,
                    )
                }
            }
        } else {
            if (isEditable) {
                ReplicaWorkspaceCodeEditorPane(
                    value = text,
                    fileName = preview.relativePath.substringAfterLast('/'),
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxSize(),
                    editorRef = editorRef,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "此类文件仅支持预览。",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF667085),
                    )
                }
            }
        }
    }
}

private fun buildReplicaWorkspaceEntries(
    workspaceRoot: File,
    showHiddenFiles: Boolean,
    sortMode: Int,
): List<ReplicaWorkspaceEntry> {
    if (!workspaceRoot.exists()) {
        return emptyList()
    }
    val entries = mutableListOf<ReplicaWorkspaceEntry>()

    fun walk(directory: File, depth: Int) {
        val children =
            directory.listFiles()
                ?.asSequence()
                ?.filter { showHiddenFiles || !it.name.startsWith(".") }
                ?.sortedWith(replicaWorkspaceFileComparator(sortMode))
                ?.toList()
                .orEmpty()
        children.forEach { child ->
            entries +=
                ReplicaWorkspaceEntry(
                    name = child.name,
                    absolutePath = child.absolutePath,
                    relativePath = child.relativeTo(workspaceRoot).invariantSeparatorsPath,
                    depth = depth,
                    isDirectory = child.isDirectory,
                    size = child.length(),
                    lastModified = child.lastModified(),
                )
            if (child.isDirectory && depth < 4) {
                walk(child, depth + 1)
            }
        }
    }

    walk(workspaceRoot, 0)
    return entries
}

private fun buildReplicaWorkspaceDirectoryEntries(
    currentDirectory: File,
    workspaceRoot: File,
    showHiddenFiles: Boolean,
    sortMode: Int,
): List<ReplicaWorkspaceEntry> {
    return currentDirectory
        .listFiles()
        ?.asSequence()
        ?.filter { showHiddenFiles || !it.name.startsWith(".") }
        ?.sortedWith(replicaWorkspaceFileComparator(sortMode))
        ?.map { child ->
            ReplicaWorkspaceEntry(
                name = child.name,
                absolutePath = child.absolutePath,
                relativePath = child.relativeTo(workspaceRoot).invariantSeparatorsPath,
                depth = 0,
                isDirectory = child.isDirectory,
                size = child.length(),
                lastModified = child.lastModified(),
            )
        }
        ?.toList()
        .orEmpty()
}

private fun buildReplicaWorkspaceFilePreview(
    file: File,
    root: File,
): ReplicaWorkspaceFilePreview? {
    if (!file.exists() || !file.isFile) {
        return null
    }
    val extension = file.extension.lowercase()
    val trimmedText =
        if (isReplicaWorkspaceTextEditable(extension)) {
            val rawText =
                runCatching { file.readText() }
                    .getOrElse { return null }
            if (rawText.length <= WORKSPACE_MAX_FILE_PREVIEW_CHARS) {
                rawText
            } else {
                rawText.take(WORKSPACE_MAX_FILE_PREVIEW_CHARS) + "\n\n... [truncated]"
            }
        } else {
            ""
        }
    val lines = trimmedText.lines().ifEmpty { listOf("") }
    return ReplicaWorkspaceFilePreview(
        relativePath = file.relativeTo(root).invariantSeparatorsPath,
        extension = extension,
        lineNumbers = if (trimmedText.isBlank()) "" else lines.indices.joinToString("\n") { (it + 1).toString() },
        content = trimmedText,
    )
}

private fun buildReplicaWorkspaceLineNumbers(text: String?): String {
    val lines = text?.lines().orEmpty().ifEmpty { listOf("") }
    return lines.indices.joinToString("\n") { (it + 1).toString() }
}

private fun buildReplicaWorkspaceQuickPaths(workspaceRoot: File): List<ReplicaWorkspaceQuickPath> {
    val fixedPaths =
        listOf(
            ReplicaWorkspaceQuickPath(label = "Workspace", relativePath = "", icon = Icons.Default.Folder),
            ReplicaWorkspaceQuickPath(label = "notes", relativePath = "notes", icon = Icons.Default.Description),
            ReplicaWorkspaceQuickPath(label = "prompts", relativePath = "prompts", icon = Icons.Default.Code),
            ReplicaWorkspaceQuickPath(label = "preview", relativePath = "preview", icon = Icons.Default.Visibility),
            ReplicaWorkspaceQuickPath(label = "scratch", relativePath = "scratch", icon = Icons.Default.Edit),
        )
    return fixedPaths.filter { quickPath ->
        quickPath.relativePath.isBlank() || File(workspaceRoot, quickPath.relativePath).isDirectory
    }
}

private fun buildReplicaWorkspaceSortLabel(
    sortMode: Int,
    showHiddenFiles: Boolean,
): String {
    val sortLabel =
        when (sortMode) {
            1 -> "按大小排序"
            2 -> "按修改时间排序"
            else -> "按名称排序"
        }
    return if (showHiddenFiles) {
        "$sortLabel · 显示隐藏文件"
    } else {
        "$sortLabel · 隐藏隐藏文件"
    }
}

private fun buildReplicaWorkspaceStatusSummary(
    sortMode: Int,
    showHiddenFiles: Boolean,
    itemCount: Int,
): String {
    val sortLabel =
        when (sortMode) {
            1 -> "按大小"
            2 -> "按修改时间"
            else -> "按名称"
        }
    val hiddenLabel =
        if (showHiddenFiles) {
            "显示.文件"
        } else {
            "隐藏.文件"
        }
    val itemLabel =
        if (itemCount == 1) {
            "1 项"
        } else {
            "$itemCount 项"
        }
    return "$sortLabel | $itemLabel | $hiddenLabel"
}

private fun buildReplicaWorkspaceDisplayPath(
    relativePath: String,
): String {
    if (relativePath.isBlank()) {
        return "Workspace"
    }
    return "Workspace / $relativePath"
}

private fun replicaWorkspaceFileComparator(sortMode: Int): Comparator<File> {
    return when (sortMode) {
        1 -> compareBy<File>({ !it.isDirectory }, { -it.length() }, { it.name.lowercase() })
        2 -> compareBy<File>({ !it.isDirectory }, { -it.lastModified() }, { it.name.lowercase() })
        else -> compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })
    }
}

private fun shouldReplicaWorkspaceStartInPreview(relativePath: String): Boolean {
    val extension = relativePath.substringAfterLast('.', "").lowercase()
    return isReplicaWorkspaceMarkdown(extension) ||
        isReplicaWorkspaceHtml(extension) ||
        isReplicaWorkspaceImage(extension) ||
        isReplicaWorkspaceAudio(extension) ||
        isReplicaWorkspaceVideo(extension) ||
        isReplicaWorkspaceDocument(extension)
}

private fun resolveReplicaWorkspaceDirectory(
    workspaceRoot: File,
    relativePath: String,
): File {
    if (relativePath.isBlank()) {
        return workspaceRoot
    }
    val resolved = File(workspaceRoot, relativePath)
    return resolved.takeIf { it.exists() && it.isDirectory } ?: workspaceRoot
}

private fun buildReplicaWorkspaceBreadcrumbs(relativePath: String): List<ReplicaWorkspaceQuickPath> {
    if (relativePath.isBlank()) {
        return emptyList()
    }
    val segments = relativePath.split('/').filter { it.isNotBlank() }
    return segments.mapIndexed { index, segment ->
        ReplicaWorkspaceQuickPath(
            label = segment,
            relativePath = segments.take(index + 1).joinToString("/"),
            icon = Icons.Default.Folder,
        )
    }
}

private fun isReplicaWorkspaceMarkdown(extension: String): Boolean {
    return extension == "md" || extension == "markdown"
}

private fun isReplicaWorkspaceHtml(extension: String): Boolean {
    return extension == "html" || extension == "htm"
}

private fun isReplicaWorkspaceImage(extension: String): Boolean {
    return extension in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "svg")
}

private fun isReplicaWorkspaceAudio(extension: String): Boolean {
    return extension in listOf("mp3", "wav", "ogg", "m4a", "aac", "flac")
}

private fun isReplicaWorkspaceVideo(extension: String): Boolean {
    return extension in listOf("mp4", "webm", "mkv", "mov", "m4v")
}

private fun isReplicaWorkspaceDocument(extension: String): Boolean {
    return extension in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
}

private fun isReplicaWorkspaceTextEditable(extension: String): Boolean {
    return extension in
        listOf(
            "md", "markdown", "txt", "json", "kt", "kts", "xml", "html", "htm", "css", "js", "ts",
            "tsx", "jsx", "java", "gradle", "properties", "yml", "yaml", "sh", "py", "sql", "csv",
            "log", "ini",
        )
}

private fun buildReplicaWorkspaceMarkdownPreviewHtml(
    title: String,
    markdown: String,
): String {
    val lines = markdown.lines()
    val body =
        buildString {
            var inCodeBlock = false
            lines.forEach { rawLine ->
                val line = rawLine.trimEnd()
                when {
                    line.trim() == "```" -> {
                        append(if (inCodeBlock) "</code></pre>" else "<pre><code>")
                        inCodeBlock = !inCodeBlock
                    }
                    inCodeBlock -> {
                        append(TextUtils.htmlEncode(line))
                        append('\n')
                    }
                    line.startsWith("### ") -> append("<h3>${TextUtils.htmlEncode(line.removePrefix("### "))}</h3>")
                    line.startsWith("## ") -> append("<h2>${TextUtils.htmlEncode(line.removePrefix("## "))}</h2>")
                    line.startsWith("# ") -> append("<h1>${TextUtils.htmlEncode(line.removePrefix("# "))}</h1>")
                    line.startsWith("- ") -> append("<li>${TextUtils.htmlEncode(line.removePrefix("- "))}</li>")
                    line.isBlank() -> append("<div class=\"spacer\"></div>")
                    else -> append("<p>${TextUtils.htmlEncode(line)}</p>")
                }
            }
            if (inCodeBlock) {
                append("</code></pre>")
            }
        }
    val safeTitle = TextUtils.htmlEncode(title)
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$safeTitle</title>
          <style>
            body {
              margin: 0;
              padding: 28px;
              background: #f6f8fc;
              color: #111111;
              font-family: "Segoe UI", "Microsoft YaHei", sans-serif;
            }
            .sheet {
              max-width: 920px;
              margin: 0 auto;
              background: #ffffff;
              border: 1px solid #e3e8f2;
              border-radius: 20px;
              padding: 24px 28px;
              box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
            }
            h1, h2, h3 {
              color: #111111;
              margin: 0 0 14px;
            }
            p, li {
              color: #344054;
              line-height: 1.75;
              font-size: 14px;
              margin: 0 0 12px;
            }
            li {
              margin-left: 18px;
            }
            pre {
              margin: 0 0 16px;
              padding: 16px;
              background: #101828;
              color: #f8fafc;
              border-radius: 16px;
              overflow: auto;
            }
            code {
              font-family: Consolas, monospace;
              font-size: 12px;
            }
            .spacer {
              height: 10px;
            }
          </style>
        </head>
        <body>
          <article class="sheet">$body</article>
        </body>
        </html>
    """.trimIndent()
}

private fun buildReplicaWorkspaceAssetPreviewHtml(
    title: String,
    assetUrl: String,
    tag: String,
): String {
    val safeTitle = TextUtils.htmlEncode(title)
    val safeUrl = TextUtils.htmlEncode(assetUrl)
    val mediaMarkup =
        when (tag) {
            "img" -> "<img src=\"$safeUrl\" alt=\"$safeTitle\" />"
            "audio" -> "<audio controls src=\"$safeUrl\"></audio>"
            "video" -> "<video controls src=\"$safeUrl\"></video>"
            else -> "<p>Unsupported preview</p>"
        }
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$safeTitle</title>
          <style>
            body {
              margin: 0;
              min-height: 100vh;
              display: grid;
              place-items: center;
              background: #0f172a;
              color: #ffffff;
              font-family: "Segoe UI", sans-serif;
            }
            .stage {
              width: min(92vw, 980px);
              height: min(82vh, 720px);
              display: grid;
              place-items: center;
            }
            img, video, audio {
              max-width: 100%;
              max-height: 100%;
              border-radius: 16px;
              box-shadow: 0 18px 40px rgba(0, 0, 0, 0.3);
            }
            audio {
              width: min(100%, 720px);
            }
          </style>
        </head>
        <body>
          <div class="stage">$mediaMarkup</div>
        </body>
        </html>
    """.trimIndent()
}

private fun buildReplicaWorkspaceDocumentPreviewHtml(
    title: String,
    extension: String,
    assetUrl: String,
): String {
    val safeTitle = TextUtils.htmlEncode(title)
    val safeUrl = TextUtils.htmlEncode(assetUrl)
    val safeExtension = TextUtils.htmlEncode(extension.uppercase())
    val embedMarkup =
        if (extension == "pdf") {
            """
            <iframe src="$safeUrl"></iframe>
            <div class="hint">如果当前设备无法内联显示 PDF，请稍后使用外部应用打开。</div>
            """.trimIndent()
        } else {
            """
            <div class="card">
              <div class="badge">$safeExtension</div>
              <h1>$safeTitle</h1>
              <p>此文档当前以只读方式预览。</p>
              <p class="path">$safeUrl</p>
            </div>
            """.trimIndent()
        }
    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$safeTitle</title>
          <style>
            body {
              margin: 0;
              min-height: 100vh;
              background: #f4f7fb;
              color: #111111;
              font-family: "Segoe UI", sans-serif;
            }
            iframe {
              border: 0;
              width: 100vw;
              height: 100vh;
              background: #ffffff;
            }
            .hint {
              position: fixed;
              left: 16px;
              right: 16px;
              bottom: 16px;
              padding: 12px 14px;
              border-radius: 14px;
              background: rgba(15, 23, 42, 0.82);
              color: #ffffff;
              font-size: 12px;
            }
            .card {
              max-width: 760px;
              margin: 48px auto;
              padding: 28px;
              background: #ffffff;
              border: 1px solid #dce3f0;
              border-radius: 20px;
              box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
            }
            .badge {
              display: inline-flex;
              padding: 6px 10px;
              border-radius: 999px;
              background: #eaf1ff;
              color: #35579b;
              font-size: 12px;
              font-weight: 700;
            }
            h1 {
              margin: 14px 0 12px;
              font-size: 28px;
            }
            p {
              margin: 0 0 12px;
              color: #475467;
              line-height: 1.7;
            }
            .path {
              font-family: Consolas, monospace;
              word-break: break-all;
            }
          </style>
        </head>
        <body>$embedMarkup</body>
        </html>
    """.trimIndent()
}

private fun ensureReplicaWorkspaceRoot(
    context: Context,
    conversationId: String,
    title: String,
    preview: String,
    messageCount: Int,
): File {
    val sanitizedConversationId = conversationId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val root = File(context.filesDir, "operit-replica-workspaces/$sanitizedConversationId")
    val notesDir = File(root, "notes")
    val promptsDir = File(root, "prompts")
    val previewDir = File(root, "preview")
    val scratchDir = File(root, "scratch")

    listOf(root, notesDir, promptsDir, previewDir, scratchDir).forEach { directory ->
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    writeReplicaWorkspaceSeedFile(
        File(root, "README.md"),
        """
        # $title

        当前会话的工作区已准备就绪。

        - 会话 ID：$conversationId
        - 上下文消息数：$messageCount

        ## 摘要

        $preview

        ## 目录

        - `notes/`：上下文记录
        - `prompts/`：任务与提示
        - `preview/`：预览文件
        - `scratch/`：临时草稿
        """.trimIndent(),
        legacyMarkers =
            listOf(
                "继续把 Kiyori 正一屏向 Operit 的 WorkspaceScreen 靠拢",
                "这是当前正一屏会话对应的本地 Workspace 根目录。",
            ),
    )

    writeReplicaWorkspaceSeedFile(
        File(notesDir, "session-context.md"),
        """
        # 会话上下文

        会话标题：$title

        最近摘要：

        $preview
        """.trimIndent(),
        legacyMarkers =
            listOf(
                "# Session Context",
                "operitreplica",
                "不等于 Operit 完整工作区",
            ),
    )

    writeReplicaWorkspaceSeedFile(
        File(promptsDir, "next-task.md"),
        """
        # 当前任务

        - 对齐当前需求
        - 更新相关文件
        - 完成后重新验证

        ## 备注

        在此记录下一步待办或补充说明。
        """.trimIndent(),
        legacyMarkers =
            listOf(
                "# Next Task",
                "WebView 宿主首版",
                "更接近 Operit 的真实工作区",
            ),
    )

    writeReplicaWorkspaceSeedFile(
        File(scratchDir, "ideas.txt"),
        """
        临时记录：
        - 在这里记下零散想法
        - 未整理内容先放在这里
        - 确认后的内容再移入 notes 或 prompts
        """.trimIndent(),
        legacyMarkers =
            listOf(
                "Workspace alignment notes:",
                "preserve host when hidden",
                "file manager + preview split layout",
            ),
    )

    writeReplicaWorkspaceSeedFile(
        File(previewDir, "index.html"),
        buildReplicaWorkspacePreviewHtml(
            title = title,
            preview = preview,
            messageCount = messageCount,
            workspaceRoot = root,
        ),
        legacyMarkers =
            listOf(
                "OPERIT WORKSPACE PREVIEW",
                "WebView Host",
                "File Manager",
            ),
    )

    return root
}

private fun writeReplicaWorkspaceSeedFile(
    file: File,
    content: String,
    legacyMarkers: List<String> = emptyList(),
) {
    if (!file.exists()) {
        file.writeText(content)
        return
    }
    val existingContent = runCatching { file.readText() }.getOrDefault("")
    if (legacyMarkers.any { marker -> existingContent.contains(marker) }) {
        file.writeText(content)
    }
}

private fun buildReplicaWorkspacePreviewHtml(
    title: String,
    preview: String,
    messageCount: Int,
    workspaceRoot: File,
): String {
    val safeTitle = TextUtils.htmlEncode(title)
    val safePreview = TextUtils.htmlEncode(preview)
    val folderCount = workspaceRoot.listFiles()?.count { it.isDirectory } ?: 0
    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Workspace</title>
          <style>
            body {
              margin: 0;
              font-family: "Segoe UI", "Microsoft YaHei", sans-serif;
              background: linear-gradient(180deg, #f6f8fd 0%, #edf2fb 100%);
              color: #111111;
              padding: 28px;
            }
            .frame {
              max-width: 1080px;
              margin: 0 auto;
              display: grid;
              gap: 16px;
            }
            .hero, .card {
              background: #ffffff;
              border: 1px solid #dce3f0;
              border-radius: 22px;
              padding: 22px;
              box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
            }
            .badge {
              display: inline-flex;
              border-radius: 999px;
              padding: 6px 10px;
              background: rgba(69, 104, 178, 0.1);
              color: #4568b2;
              font-size: 12px;
              font-weight: 700;
            }
            h1 {
              margin: 12px 0 10px;
              font-size: 30px;
            }
            p {
              margin: 0;
              color: #667085;
              line-height: 1.7;
            }
            .grid {
              display: grid;
              grid-template-columns: repeat(3, minmax(0, 1fr));
              gap: 16px;
            }
            .label {
              font-size: 12px;
              color: #667085;
              text-transform: uppercase;
              letter-spacing: 0.06em;
            }
            .value {
              margin-top: 10px;
              font-size: 20px;
              font-weight: 700;
            }
            .path {
              margin-top: 14px;
              padding: 14px;
              border-radius: 16px;
              background: #f7f9fd;
              color: #475467;
              font-family: Consolas, monospace;
              font-size: 13px;
              word-break: break-all;
            }
            .list {
              margin: 14px 0 0;
              padding-left: 18px;
              color: #475467;
              line-height: 1.8;
            }
            @media (max-width: 900px) {
              .grid { grid-template-columns: 1fr; }
              body { padding: 16px; }
            }
          </style>
        </head>
        <body>
          <div class="frame">
            <section class="hero">
              <div class="badge">WORKSPACE</div>
              <h1>$safeTitle</h1>
              <p>$safePreview</p>
              <div class="path">preview/index.html</div>
              <ul class="list">
                <li>从左侧文件列表选择文件可继续预览或编辑</li>
                <li>默认目录包含 notes、prompts、preview、scratch</li>
              </ul>
            </section>
            <section class="grid">
              <article class="card">
                <div class="label">消息</div>
                <div class="value">$messageCount 条</div>
              </article>
              <article class="card">
                <div class="label">目录</div>
                <div class="value">$folderCount</div>
              </article>
              <article class="card">
                <div class="label">状态</div>
                <div class="value">就绪</div>
              </article>
            </section>
          </div>
        </body>
        </html>
    """.trimIndent()
}

private fun buildWorkspaceLandingHtml(
    title: String,
    preview: String,
    messageCount: Int,
): String {
    val safeTitle = TextUtils.htmlEncode(title)
    val safePreview = TextUtils.htmlEncode(preview)
    return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Workspace</title>
          <style>
            :root {
              color-scheme: light;
              --bg: #f3f6fb;
              --card: #ffffff;
              --line: #dce3f0;
              --text: #111111;
              --muted: #667085;
              --accent: #4568b2;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              font-family: "Segoe UI", "Microsoft YaHei", sans-serif;
              background:
                radial-gradient(circle at top left, rgba(97,119,178,0.14), transparent 32%),
                linear-gradient(180deg, #f9fbff 0%, var(--bg) 100%);
              color: var(--text);
              min-height: 100vh;
              padding: 28px;
            }
            .shell {
              max-width: 1120px;
              margin: 0 auto;
              display: grid;
              gap: 18px;
            }
            .hero {
              background: var(--card);
              border: 1px solid var(--line);
              border-radius: 24px;
              padding: 24px;
              box-shadow: 0 14px 40px rgba(17, 24, 39, 0.06);
            }
            .eyebrow {
              display: inline-flex;
              padding: 6px 10px;
              border-radius: 999px;
              background: rgba(69,104,178,0.08);
              color: var(--accent);
              font-size: 12px;
              font-weight: 700;
              letter-spacing: 0.04em;
            }
            h1 {
              margin: 14px 0 10px;
              font-size: 32px;
              line-height: 1.15;
            }
            p {
              margin: 0;
              color: var(--muted);
              line-height: 1.7;
              font-size: 15px;
            }
            .note {
              margin-top: 18px;
              padding: 16px 18px;
              background: #f6f8fc;
              color: #475467;
              border-radius: 18px;
              border: 1px solid var(--line);
            }
            .note strong {
              color: var(--text);
            }
            @media (max-width: 900px) {
              body { padding: 16px; }
              h1 { font-size: 26px; }
            }
          </style>
        </head>
        <body>
          <div class="shell">
            <section class="hero">
              <div class="eyebrow">WORKSPACE</div>
              <h1>$safeTitle</h1>
              <p>$safePreview</p>
              <div class="note">
                <strong>当前未打开文件。</strong>
                从左侧文件列表选择文件以开始预览或编辑。
              </div>
              <div class="note" style="margin-top: 12px;">
                <strong>消息：</strong>
                $messageCount 条
              </div>
            </section>
          </div>
        </body>
        </html>
    """.trimIndent()
}

private fun resolveReplicaWorkspaceFileIcon(extension: String): ImageVector {
    return when (extension) {
        "html",
        "htm",
        "css",
        "js",
        "ts",
        "tsx",
        "jsx",
        "kt",
        "kts",
        "java",
        "json",
        "xml",
        "sh",
        "py",
        "sql" -> Icons.Default.Code
        "md",
        "markdown",
        "txt",
        "csv",
        "log",
        "ini",
        "properties",
        "yml",
        "yaml" -> Icons.Default.Description
        "png",
        "jpg",
        "jpeg",
        "webp",
        "gif",
        "bmp",
        "svg" -> Icons.Default.Image
        "mp3",
        "wav",
        "ogg",
        "m4a",
        "aac",
        "flac" -> Icons.Default.MusicNote
        "mp4",
        "webm",
        "mkv",
        "mov",
        "m4v" -> Icons.Default.Movie
        "pdf" -> Icons.Default.PictureAsPdf
        "xls",
        "xlsx" -> Icons.Default.TableChart
        "ppt",
        "pptx" -> Icons.Default.Slideshow
        else -> Icons.Default.Description
    }
}
