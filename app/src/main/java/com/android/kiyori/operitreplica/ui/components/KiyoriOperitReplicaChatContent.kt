package com.android.kiyori.operitreplica.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceCallbacks
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceSender
import com.ai.assistance.operit.ui.features.chat.components.ChatMessageActionSurfaceBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatMultiSelectToolbarBridge
import com.ai.assistance.operit.ui.features.chat.components.MAX_VISIBLE_CHAT_PAGES
import com.ai.assistance.operit.ui.features.chat.components.buildChatAreaPaginationState
import com.ai.assistance.operit.ui.features.chat.components.ChatScrollMessageAnchor
import com.ai.assistance.operit.ui.features.chat.components.ChatScrollNavigatorBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatScrollNavigatorDisplayMode
import com.ai.assistance.operit.ui.features.chat.components.ChatScrollNavigatorMessage
import com.ai.assistance.operit.ui.features.chat.components.ChatScreenContentWorkbenchBridge
import com.ai.assistance.operit.ui.features.chat.components.findChatAreaPaginationDepthForIndex
import com.ai.assistance.operit.ui.features.chat.components.LoadingDotsIndicator
import com.ai.assistance.operit.ui.features.chat.components.MessageEditor
import com.ai.assistance.operit.ui.features.chat.components.resolveChatAreaPaginationWindow
import com.ai.assistance.operit.ui.features.chat.components.resolveChatAreaWindowDepthsForTargetPage
import com.ai.assistance.operit.ui.features.chat.components.ScrollToBottomButton
import com.ai.assistance.operit.ui.features.chat.components.style.cursor.ChatMessageCursorStyleBridge
import com.android.kiyori.operitreplica.model.OperitReplicaMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessageRole
import com.android.kiyori.operitreplica.ui.components.message.KiyoriOperitReplicaSystemMessage
import kotlin.math.roundToInt

private fun OperitReplicaMessage.toChatScrollNavigatorMessage(): ChatScrollNavigatorMessage =
    ChatScrollNavigatorMessage(
        sender =
            when (role) {
                OperitReplicaMessageRole.User -> "user"
                OperitReplicaMessageRole.Assistant -> "ai"
                OperitReplicaMessageRole.System -> "system"
            },
        content = text,
        displayMode = ChatScrollNavigatorDisplayMode.NORMAL,
    )

@Composable
internal fun KiyoriOperitReplicaChatContent(
    modifier: Modifier = Modifier,
    activeConversationId: String,
    bottomDockPadding: Dp,
    immersiveMode: Boolean,
    showHistoryPanel: Boolean,
    currentCharacterLabel: String,
    usageProgress: Float,
    contextUsagePercentage: Float,
    currentWindowSize: Int,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showStatsMenu: Boolean,
    isInputProcessing: Boolean,
    activeMessages: List<OperitReplicaMessage>,
    onHistoryClick: () -> Unit,
    onPipClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onStatsMenuDismiss: () -> Unit,
    onStatsMenuToggle: () -> Unit,
    onUpdateMessage: (Int, String) -> Unit,
    onRewindAndResendMessage: (Int, String) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onDeleteMessages: (Set<Int>) -> Unit,
    onRollbackToMessage: (Int) -> Unit,
    onRegenerateMessage: (Int) -> Unit,
    onInsertSummary: (Int) -> Unit,
    onCreateBranch: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var autoScrollToBottom by remember { mutableStateOf(true) }
    var newestVisibleDepthState by remember(activeConversationId) { mutableStateOf(1) }
    var oldestVisibleDepthState by remember(activeConversationId) { mutableStateOf(1) }
    var viewportHeightPx by remember { mutableStateOf(0) }
    val messageAnchors = remember(activeConversationId) { mutableStateMapOf<Int, ChatScrollMessageAnchor>() }
    var pendingJumpToMessageIndex by remember(activeConversationId) { mutableStateOf<Int?>(null) }
    var isMultiSelectMode by remember(activeConversationId) { mutableStateOf(false) }
    var selectedMessageIndices by remember(activeConversationId) { mutableStateOf(emptySet<Int>()) }
    val editingMessageIndex = remember(activeConversationId) { mutableStateOf<Int?>(null) }
    val editingMessageContent = remember(activeConversationId) { mutableStateOf("") }
    var editingMessageType by remember(activeConversationId) { mutableStateOf<String?>(null) }
    var showDeleteSelectedConfirmDialog by remember(activeConversationId) { mutableStateOf(false) }
    val selectableMessageIndices =
        remember(activeMessages) {
            activeMessages.mapIndexedNotNull { index, message ->
                if (message.role == OperitReplicaMessageRole.User || message.role == OperitReplicaMessageRole.Assistant) {
                    index
                } else {
                    null
                }
            }.toSet()
        }
    val paginationState =
        remember(activeMessages) {
            buildChatAreaPaginationState(
                itemCount = activeMessages.size,
                messagesPerPage = 10,
                isPaginationTrigger = { index ->
                    val role = activeMessages[index].role
                    role == OperitReplicaMessageRole.User || role == OperitReplicaMessageRole.Assistant
                },
            )
        }
    val navigatorMessages = remember(activeMessages) { activeMessages.map { it.toChatScrollNavigatorMessage() } }
    val paginationWindow =
        resolveChatAreaPaginationWindow(
            paginationState = paginationState,
            itemCount = activeMessages.size,
            newestVisibleDepth = newestVisibleDepthState,
            oldestVisibleDepth = oldestVisibleDepthState,
        )
    val visiblePageCount = paginationWindow.oldestVisibleDepth - paginationWindow.newestVisibleDepth + 1
    val visibleRange = paginationWindow.minVisibleIndex until paginationWindow.maxVisibleIndexExclusive
    val lastMessage = activeMessages.lastOrNull()
    val isLatestMessageVisible = activeMessages.isNotEmpty() && activeMessages.lastIndex in visibleRange
    val showLoadingIndicator =
        isLatestMessageVisible &&
            isInputProcessing &&
            lastMessage?.role == OperitReplicaMessageRole.User
    val visibleMessages =
        if (activeMessages.isEmpty()) {
            emptyList()
        } else {
            activeMessages.subList(
                paginationWindow.minVisibleIndex,
                paginationWindow.maxVisibleIndexExclusive,
            )
        }
    val pendingTargetAnchor = pendingJumpToMessageIndex?.let { messageAnchors[it] }

    LaunchedEffect(activeConversationId, activeMessages.isEmpty()) {
        if (activeMessages.isEmpty()) {
            newestVisibleDepthState = 1
            oldestVisibleDepthState = 1
            pendingJumpToMessageIndex = null
            isMultiSelectMode = false
            selectedMessageIndices = emptySet()
            editingMessageIndex.value = null
            editingMessageContent.value = ""
            editingMessageType = null
        }
    }

    LaunchedEffect(activeMessages.size) {
        selectedMessageIndices = selectedMessageIndices.filter { it in activeMessages.indices }.toSet()
        if (selectedMessageIndices.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    LaunchedEffect(autoScrollToBottom, activeMessages.size) {
        if (autoScrollToBottom && activeMessages.isNotEmpty()) {
            newestVisibleDepthState = 1
            oldestVisibleDepthState = 1
            pendingJumpToMessageIndex = activeMessages.lastIndex
        }
    }

    LaunchedEffect(paginationWindow.minVisibleIndex, activeMessages.size) {
        messageAnchors.keys
            .toList()
            .filterNot { it in visibleRange }
            .forEach(messageAnchors::remove)
    }

    LaunchedEffect(
        pendingJumpToMessageIndex,
        paginationWindow.minVisibleIndex,
        paginationWindow.maxVisibleIndexExclusive,
        pendingTargetAnchor,
        scrollState.maxValue,
    ) {
        val targetIndex = pendingJumpToMessageIndex ?: return@LaunchedEffect
        if (targetIndex !in activeMessages.indices) {
            pendingJumpToMessageIndex = null
            return@LaunchedEffect
        }

        if (targetIndex !in visibleRange) {
            val targetPageDepth = findChatAreaPaginationDepthForIndex(paginationState, targetIndex)
            val (newestDepth, oldestDepth) =
                resolveChatAreaWindowDepthsForTargetPage(
                    paginationState = paginationState,
                    targetPageDepth = targetPageDepth,
                )
            if (
                newestDepth != paginationWindow.newestVisibleDepth ||
                    oldestDepth != paginationWindow.oldestVisibleDepth
            ) {
                newestVisibleDepthState = newestDepth
                oldestVisibleDepthState = oldestDepth
            }
            return@LaunchedEffect
        }

        val targetAnchor = pendingTargetAnchor ?: return@LaunchedEffect
        val targetOffset = targetAnchor.absoluteTopPx.roundToInt().coerceIn(0, scrollState.maxValue)
        if (targetIndex == activeMessages.lastIndex) {
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            scrollState.animateScrollTo(targetOffset)
        }
        pendingJumpToMessageIndex = null
    }

    ChatScreenContentWorkbenchBridge(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
        backgroundColor = Color.White,
        chatHeaderOverlayMode = true,
        headerContent = {
            KiyoriOperitReplicaHeader(
                showHistoryPanel = showHistoryPanel,
                immersiveMode = immersiveMode,
                currentCharacterLabel = currentCharacterLabel,
                usageProgress = usageProgress,
                contextUsagePercentage = contextUsagePercentage,
                currentWindowSize = currentWindowSize,
                inputTokenCount = inputTokenCount,
                outputTokenCount = outputTokenCount,
                showStatsMenu = showStatsMenu,
                onHistoryClick = onHistoryClick,
                onPipClick = onPipClick,
                onCharacterClick = onCharacterClick,
                onStatsMenuDismiss = onStatsMenuDismiss,
                onStatsMenuToggle = onStatsMenuToggle,
            )
        },
    ) { topPadding, bottomPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        viewportHeightPx = coordinates.size.height
                    },
        ) {
            if (visibleMessages.isNotEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = topPadding + 8.dp,
                            ),
                ) {
                    if (paginationWindow.hasOlderPages) {
                        Text(
                            text = stringResource(R.string.load_more_history),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        autoScrollToBottom = false
                                        if (visiblePageCount < MAX_VISIBLE_CHAT_PAGES) {
                                            oldestVisibleDepthState = paginationWindow.oldestVisibleDepth + 1
                                        } else {
                                            newestVisibleDepthState = paginationWindow.newestVisibleDepth + 1
                                            oldestVisibleDepthState = paginationWindow.oldestVisibleDepth + 1
                                        }
                                    }
                                    .padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    visibleMessages.forEachIndexed { relativeIndex, message ->
                        val actualIndex = paginationWindow.minVisibleIndex + relativeIndex
                        key(actualIndex, message.role, message.text, message.meta) {
                            val actionSurfaceMessage = message.toChatActionSurfaceMessage()
                            Box(
                                modifier =
                                    Modifier.onGloballyPositioned { coordinates ->
                                        messageAnchors[actualIndex] =
                                            ChatScrollMessageAnchor(
                                                absoluteTopPx = coordinates.positionInParent().y,
                                                heightPx = coordinates.size.height,
                                            )
                                    },
                            ) {
                                if (message.role == OperitReplicaMessageRole.System) {
                                    KiyoriOperitReplicaSystemMessage(message = message)
                                } else {
                                    ChatMessageActionSurfaceBridge(
                                        index = actualIndex,
                                        message = actionSurfaceMessage,
                                        isMultiSelectMode = isMultiSelectMode,
                                        isSelected = selectedMessageIndices.contains(actualIndex),
                                        callbacks =
                                            ChatActionSurfaceCallbacks(
                                                onEditMessage = { index, targetMessage, senderType ->
                                                    editingMessageIndex.value = index
                                                    editingMessageContent.value = targetMessage.content
                                                    editingMessageType = senderType
                                                },
                                                onDeleteMessage = onDeleteMessage,
                                                onRollbackToMessage = onRollbackToMessage,
                                                onRegenerateMessage = onRegenerateMessage,
                                                onReplyToMessage = {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.reply_message),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                },
                                                onInsertSummary = { index, _ -> onInsertSummary(index) },
                                                onCreateBranch = onCreateBranch,
                                                onSpeakMessage = {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.read_message),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                },
                                                onToggleMultiSelectMode = { initialIndex ->
                                                    isMultiSelectMode = !isMultiSelectMode
                                                    selectedMessageIndices =
                                                        if (isMultiSelectMode && initialIndex != null) {
                                                            setOf(initialIndex)
                                                        } else {
                                                            emptySet()
                                                        }
                                                },
                                                onToggleMessageSelection = { index ->
                                                    selectedMessageIndices =
                                                        if (selectedMessageIndices.contains(index)) {
                                                            selectedMessageIndices - index
                                                        } else {
                                                            selectedMessageIndices + index
                                                        }
                                                },
                                            ),
                                    ) {
                                        ChatMessageCursorStyleBridge(message = actionSurfaceMessage)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (paginationWindow.hasNewerPages) {
                        Text(
                            text = stringResource(R.string.load_newer_history),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val nextNewestVisibleDepth =
                                            (paginationWindow.newestVisibleDepth - 1).coerceAtLeast(1)
                                        val nextOldestVisibleDepth =
                                            (paginationWindow.oldestVisibleDepth - 1)
                                                .coerceAtLeast(nextNewestVisibleDepth)
                                        newestVisibleDepthState = nextNewestVisibleDepth
                                        oldestVisibleDepthState = nextOldestVisibleDepth
                                        autoScrollToBottom = nextNewestVisibleDepth == 1
                                    }
                                    .padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (showLoadingIndicator) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 0.dp),
                        ) {
                            Box(modifier = Modifier.padding(start = 16.dp)) {
                                LoadingDotsIndicator(textColor = Color(0xFF111827))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(140.dp + bottomDockPadding + bottomPadding))
                }
            } else {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(topPadding + 12.dp),
                )
            }

            ScrollToBottomButton(
                scrollState = scrollState,
                coroutineScope = coroutineScope,
                autoScrollToBottom = autoScrollToBottom,
                hasHiddenNewerMessages = paginationWindow.hasNewerPages,
                onAutoScrollToBottomChange = { autoScrollToBottom = it },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 92.dp + bottomDockPadding + bottomPadding),
            )

            ChatScrollNavigatorBridge(
                chatHistory = navigatorMessages,
                scrollState = scrollState,
                messageAnchors = messageAnchors,
                viewportHeightPx = viewportHeightPx,
                onJumpToMessage = { targetIndex ->
                    autoScrollToBottom = targetIndex == activeMessages.lastIndex
                    pendingJumpToMessageIndex = targetIndex
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = (-56).dp)
                        .padding(end = 10.dp),
            )

            if (isMultiSelectMode) {
                ChatMultiSelectToolbarBridge(
                    selectedMessageIndices = selectedMessageIndices,
                    selectableMessageIndices = selectableMessageIndices,
                    onExit = {
                        isMultiSelectMode = false
                        selectedMessageIndices = emptySet()
                    },
                    onSelectAllChange = { selectedMessageIndices = it },
                    onShareSelected = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.share_selected),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onDeleteSelected = {
                        if (selectedMessageIndices.isNotEmpty()) {
                            showDeleteSelectedConfirmDialog = true
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topPadding + 4.dp),
                )
            }

            if (editingMessageIndex.value != null) {
                MessageEditor(
                    editingMessageContent = editingMessageContent,
                    onCancel = {
                        editingMessageIndex.value = null
                        editingMessageContent.value = ""
                        editingMessageType = null
                    },
                    onSave = {
                        editingMessageIndex.value?.let { index ->
                            onUpdateMessage(index, editingMessageContent.value)
                        }
                        editingMessageIndex.value = null
                        editingMessageContent.value = ""
                        editingMessageType = null
                    },
                    onResend = {
                        editingMessageIndex.value?.let { index ->
                            onRewindAndResendMessage(index, editingMessageContent.value)
                        }
                        editingMessageIndex.value = null
                        editingMessageContent.value = ""
                        editingMessageType = null
                    },
                    showResendButton = editingMessageType == "user",
                )
            }

            if (showDeleteSelectedConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteSelectedConfirmDialog = false },
                    title = { Text(stringResource(R.string.confirm_delete)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.confirm_delete_selected_messages,
                                selectedMessageIndices.size,
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteMessages(selectedMessageIndices)
                                selectedMessageIndices = emptySet()
                                isMultiSelectMode = false
                                showDeleteSelectedConfirmDialog = false
                            },
                        ) {
                            Text(stringResource(R.string.confirm_delete_action))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteSelectedConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

private fun OperitReplicaMessage.toChatActionSurfaceMessage(): ChatActionSurfaceMessage =
    ChatActionSurfaceMessage(
        sender =
            when (role) {
                OperitReplicaMessageRole.User -> ChatActionSurfaceSender.USER
                OperitReplicaMessageRole.Assistant -> ChatActionSurfaceSender.AI
                OperitReplicaMessageRole.System -> ChatActionSurfaceSender.SYSTEM
            },
        content = text,
        meta = meta,
    )
