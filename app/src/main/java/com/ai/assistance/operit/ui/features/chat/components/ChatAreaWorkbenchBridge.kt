package com.ai.assistance.operit.ui.features.chat.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import kotlin.math.roundToInt

@Composable
fun ChatAreaWorkbenchBridge(
    activeConversationId: String,
    messages: List<ChatActionSurfaceMessage>,
    isInputProcessing: Boolean,
    topPadding: Dp,
    bottomPadding: Dp,
    bottomDockPadding: Dp,
    messagesPerPage: Int = 10,
    horizontalPadding: Dp = 16.dp,
    chatStyle: ChatAreaStyleBridge = ChatAreaStyleBridge.CURSOR,
    showChatFloatingDotsAnimation: Boolean = true,
    displayPreferences: ChatAreaDisplayPreferencesBridge = ChatAreaDisplayPreferencesBridge(),
    loadingIndicatorTextColor: Color = Color(0xFF111827),
    onHasHiddenNewerMessagesChange: ((Boolean) -> Unit)? = null,
    onUpdateMessage: (Int, String) -> Unit,
    onRewindAndResendMessage: (Int, String) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onDeleteMessages: (Set<Int>) -> Unit,
    onRollbackToMessage: (Int) -> Unit,
    onRegenerateMessage: (Int) -> Unit,
    onSwitchMessageVariant: (Int, Int) -> Unit,
    onDeleteCurrentMessageVariant: (Int) -> Unit,
    onInsertSummary: (Int, ChatActionSurfaceMessage) -> Unit,
    onCreateBranch: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
        remember(messages) {
            messages.mapIndexedNotNull { index, message ->
                if (message.sender == ChatActionSurfaceSender.USER || message.sender == ChatActionSurfaceSender.AI) {
                    index
                } else {
                    null
                }
            }.toSet()
        }
    val paginationState =
        remember(messages) {
            buildChatAreaPaginationState(
                itemCount = messages.size,
                messagesPerPage = messagesPerPage,
                isPaginationTrigger = { index ->
                    val sender = messages[index].sender
                    sender == ChatActionSurfaceSender.USER || sender == ChatActionSurfaceSender.AI
                },
            )
        }
    val navigatorMessages = remember(messages) { messages.map { it.toChatScrollNavigatorMessage() } }
    val paginationWindow =
        resolveChatAreaPaginationWindow(
            paginationState = paginationState,
            itemCount = messages.size,
            newestVisibleDepth = newestVisibleDepthState,
            oldestVisibleDepth = oldestVisibleDepthState,
        )
    val visiblePageCount = paginationWindow.oldestVisibleDepth - paginationWindow.newestVisibleDepth + 1
    val visibleRange = paginationWindow.minVisibleIndex until paginationWindow.maxVisibleIndexExclusive
    val lastMessage = messages.lastOrNull()
    val isLatestMessageVisible = messages.isNotEmpty() && messages.lastIndex in visibleRange
    val showLoadingIndicator =
        isLatestMessageVisible &&
            isInputProcessing &&
            lastMessage?.sender == ChatActionSurfaceSender.USER
    val visibleMessages =
        if (messages.isEmpty()) {
            emptyList()
        } else {
            messages.subList(
                paginationWindow.minVisibleIndex,
                paginationWindow.maxVisibleIndexExclusive,
            )
        }
    val pendingTargetAnchor = pendingJumpToMessageIndex?.let { messageAnchors[it] }

    LaunchedEffect(activeConversationId, messages.isEmpty()) {
        if (messages.isEmpty()) {
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

    LaunchedEffect(messages.size) {
        selectedMessageIndices =
            pruneChatAreaSelectedMessageIndices(
                selectedMessageIndices = selectedMessageIndices,
                itemCount = messages.size,
            )
        if (selectedMessageIndices.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    LaunchedEffect(paginationWindow.hasNewerPages) {
        onHasHiddenNewerMessagesChange?.invoke(paginationWindow.hasNewerPages)
    }

    LaunchedEffect(autoScrollToBottom, messages.size) {
        if (autoScrollToBottom && messages.isNotEmpty()) {
            newestVisibleDepthState = 1
            oldestVisibleDepthState = 1
            pendingJumpToMessageIndex = messages.lastIndex
        }
    }

    LaunchedEffect(paginationWindow.minVisibleIndex, messages.size) {
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
        if (targetIndex !in messages.indices) {
            pendingJumpToMessageIndex = null
            return@LaunchedEffect
        }

        val pendingWindowDepths =
            resolveChatAreaPendingJumpWindowDepths(
                paginationState = paginationState,
                paginationWindow = paginationWindow,
                targetIndex = targetIndex,
                visibleRange = visibleRange,
            )
        if (pendingWindowDepths != null) {
            newestVisibleDepthState = pendingWindowDepths.newestVisibleDepth
            oldestVisibleDepthState = pendingWindowDepths.oldestVisibleDepth
            return@LaunchedEffect
        }

        val targetAnchor = pendingTargetAnchor ?: return@LaunchedEffect
        val targetOffset = targetAnchor.absoluteTopPx.roundToInt().coerceIn(0, scrollState.maxValue)
        if (targetIndex == messages.lastIndex) {
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            scrollState.animateScrollTo(targetOffset)
        }
        pendingJumpToMessageIndex = null
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    viewportHeightPx = coordinates.size.height
                },
    ) {
        ChatAreaMessageListBridge(
            firstMessageIndex = paginationWindow.minVisibleIndex,
            visibleMessages = visibleMessages,
            scrollState = scrollState,
            hasOlderPages = paginationWindow.hasOlderPages,
            hasNewerPages = paginationWindow.hasNewerPages,
            showLoadingIndicator = showLoadingIndicator,
            showChatFloatingDotsAnimation = showChatFloatingDotsAnimation,
            chatStyle = chatStyle,
            loadingIndicatorTextColor = loadingIndicatorTextColor,
            topContentPadding = topPadding + 8.dp,
            horizontalPadding = horizontalPadding,
            bottomSpacerHeight = 140.dp + bottomDockPadding + bottomPadding,
            isMultiSelectMode = isMultiSelectMode,
            selectedMessageIndices = selectedMessageIndices,
            displayPreferences = displayPreferences,
            callbacks =
                createChatAreaActionSurfaceCallbacksBridge(
                    onStartEditing = { request ->
                        editingMessageIndex.value = request.index
                        editingMessageContent.value = request.content
                        editingMessageType = request.senderType
                    },
                    onDeleteMessage = onDeleteMessage,
                    onRollbackToMessage = onRollbackToMessage,
                    onRegenerateMessage = onRegenerateMessage,
                    onSwitchMessageVariant = onSwitchMessageVariant,
                    onDeleteCurrentMessageVariant = onDeleteCurrentMessageVariant,
                    onReplyToMessage = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.reply_message),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    onInsertSummary = onInsertSummary,
                    onCreateBranch = onCreateBranch,
                    onSpeakMessage = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.read_message),
                            Toast.LENGTH_SHORT,
                            ).show()
                    },
                    onToggleMultiSelectMode = { initialIndex ->
                        val (nextMultiSelectMode, nextSelectedMessageIndices) =
                            resolveChatAreaMultiSelectToggle(
                                isMultiSelectMode = isMultiSelectMode,
                                initialIndex = initialIndex,
                            )
                        isMultiSelectMode = nextMultiSelectMode
                        selectedMessageIndices = nextSelectedMessageIndices
                    },
                    onToggleMessageSelection = { index ->
                        selectedMessageIndices =
                            toggleChatAreaMessageSelection(
                                selectedMessageIndices = selectedMessageIndices,
                                index = index,
                            )
                    },
                ),
            onLoadOlderPages = {
                autoScrollToBottom = false
                val nextWindowDepths =
                    resolveChatAreaLoadOlderWindowDepths(
                        paginationWindow = paginationWindow,
                        visiblePageCount = visiblePageCount,
                    )
                newestVisibleDepthState = nextWindowDepths.newestVisibleDepth
                oldestVisibleDepthState = nextWindowDepths.oldestVisibleDepth
            },
            onLoadNewerPages = {
                val nextWindowDepths =
                    resolveChatAreaLoadNewerWindowDepths(
                        paginationWindow = paginationWindow,
                    )
                newestVisibleDepthState = nextWindowDepths.newestVisibleDepth
                oldestVisibleDepthState = nextWindowDepths.oldestVisibleDepth
                autoScrollToBottom = nextWindowDepths.newestVisibleDepth == 1
            },
            onMessagePositioned = { index, absoluteTopPx, heightPx ->
                messageAnchors[index] =
                    ChatScrollMessageAnchor(
                        absoluteTopPx = absoluteTopPx,
                        heightPx = heightPx,
                    )
            },
        )

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
                autoScrollToBottom = targetIndex == messages.lastIndex
                pendingJumpToMessageIndex = targetIndex
            },
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-56).dp)
                    .padding(end = 10.dp),
        )

        if (isMultiSelectMode) {
            ChatAreaSelectionOverlayBridge(
                selectedMessageIndices = selectedMessageIndices,
                selectableMessageIndices = selectableMessageIndices,
                showDeleteConfirmDialog = showDeleteSelectedConfirmDialog,
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
                onDeleteSelectedClick = {
                    if (selectedMessageIndices.isNotEmpty()) {
                        showDeleteSelectedConfirmDialog = true
                    }
                },
                onConfirmDeleteSelected = {
                    onDeleteMessages(selectedMessageIndices)
                    selectedMessageIndices = emptySet()
                    isMultiSelectMode = false
                    showDeleteSelectedConfirmDialog = false
                },
                onDismissDeleteConfirm = { showDeleteSelectedConfirmDialog = false },
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding + 4.dp),
            )
        }

        ChatAreaMessageEditorHostBridge(
            editingMessageIndex = editingMessageIndex,
            editingMessageContent = editingMessageContent,
            editingMessageType = editingMessageType,
            onEditingMessageTypeChange = { editingMessageType = it },
            onUpdateMessage = onUpdateMessage,
            onRewindAndResendMessage = onRewindAndResendMessage,
        )
    }
}

private fun ChatActionSurfaceMessage.toChatScrollNavigatorMessage(): ChatScrollNavigatorMessage =
    ChatScrollNavigatorMessage(
        sender =
            when (sender) {
                ChatActionSurfaceSender.USER -> "user"
                ChatActionSurfaceSender.AI -> "ai"
                ChatActionSurfaceSender.SYSTEM -> "system"
            },
        content = content,
        displayMode =
            when (displayMode) {
                ChatActionSurfaceDisplayMode.NORMAL -> ChatScrollNavigatorDisplayMode.NORMAL
                ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER ->
                    ChatScrollNavigatorDisplayMode.HIDDEN_PLACEHOLDER
            },
    )
