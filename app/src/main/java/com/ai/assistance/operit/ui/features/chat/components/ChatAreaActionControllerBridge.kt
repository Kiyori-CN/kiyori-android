package com.ai.assistance.operit.ui.features.chat.components

data class ChatAreaEditRequest(
    val index: Int,
    val content: String,
    val senderType: String,
)

fun createChatAreaActionSurfaceCallbacksBridge(
    onStartEditing: (ChatAreaEditRequest) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onRollbackToMessage: (Int) -> Unit,
    onRegenerateMessage: (Int) -> Unit,
    onSwitchMessageVariant: (Int, Int) -> Unit,
    onDeleteCurrentMessageVariant: (Int) -> Unit,
    onReplyToMessage: (ChatActionSurfaceMessage) -> Unit,
    onInsertSummary: (Int, ChatActionSurfaceMessage) -> Unit,
    onCreateBranch: (Int) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onToggleMultiSelectMode: (Int?) -> Unit,
    onToggleMessageSelection: (Int) -> Unit,
): ChatActionSurfaceCallbacks =
    ChatActionSurfaceCallbacks(
        onEditMessage = { index, targetMessage, senderType ->
            onStartEditing(
                ChatAreaEditRequest(
                    index = index,
                    content = targetMessage.content,
                    senderType = senderType,
                ),
            )
        },
        onDeleteMessage = onDeleteMessage,
        onRollbackToMessage = onRollbackToMessage,
        onRegenerateMessage = onRegenerateMessage,
        onSwitchMessageVariant = onSwitchMessageVariant,
        onDeleteCurrentMessageVariant = onDeleteCurrentMessageVariant,
        onReplyToMessage = onReplyToMessage,
        onInsertSummary = onInsertSummary,
        onCreateBranch = onCreateBranch,
        onSpeakMessage = onSpeakMessage,
        onToggleMultiSelectMode = onToggleMultiSelectMode,
        onToggleMessageSelection = onToggleMessageSelection,
    )
