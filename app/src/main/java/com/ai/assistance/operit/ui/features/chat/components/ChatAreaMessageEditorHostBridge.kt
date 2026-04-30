package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun ChatAreaMessageEditorHostBridge(
    editingMessageIndex: MutableState<Int?>,
    editingMessageContent: MutableState<String>,
    editingMessageType: String?,
    onEditingMessageTypeChange: (String?) -> Unit,
    onUpdateMessage: (Int, String) -> Unit,
    onRewindAndResendMessage: (Int, String) -> Unit,
) {
    if (editingMessageIndex.value == null) {
        return
    }

    fun clearEditingState() {
        editingMessageIndex.value = null
        editingMessageContent.value = ""
        onEditingMessageTypeChange(null)
    }

    MessageEditor(
        editingMessageContent = editingMessageContent,
        onCancel = ::clearEditingState,
        onSave = {
            editingMessageIndex.value?.let { index ->
                onUpdateMessage(index, editingMessageContent.value)
            }
            clearEditingState()
        },
        onResend = {
            editingMessageIndex.value?.let { index ->
                onRewindAndResendMessage(index, editingMessageContent.value)
            }
            clearEditingState()
        },
        showResendButton = editingMessageType == "user",
    )
}
