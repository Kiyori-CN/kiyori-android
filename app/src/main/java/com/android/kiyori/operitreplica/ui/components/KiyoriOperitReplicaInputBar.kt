package com.android.kiyori.operitreplica.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ai.assistance.operit.data.model.AttachmentInfo
import com.ai.assistance.operit.ui.features.chat.components.style.input.agent.AgentChatInputWorkbenchBridge
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingQueueMessageItem

@Composable
internal fun KiyoriOperitReplicaInputBar(
    modifier: Modifier = Modifier,
    inputText: String,
    currentModelLabel: String,
    availableModelLabels: List<String>,
    currentMemoryProfileLabel: String,
    attachments: List<String>,
    showModelSelectorPopup: Boolean,
    showFeaturePanel: Boolean,
    showAttachmentPanel: Boolean,
    isInputProcessing: Boolean,
    inputProcessingLabel: String,
    inputProcessingProgress: Float,
    enableTools: Boolean,
    enableThinking: Boolean,
    thinkingQualityLevel: Int,
    enableMaxContextMode: Boolean,
    baseContextLengthInK: Float,
    maxContextLengthInK: Float,
    enableMemoryAutoUpdate: Boolean,
    isAutoReadEnabled: Boolean,
    isAutoApproveEnabled: Boolean,
    disableStreamOutput: Boolean,
    disableUserPreferenceDescription: Boolean,
    disableStatusTags: Boolean,
    pendingQueueMessages: List<PendingQueueMessageItem>,
    isPendingQueueExpanded: Boolean,
    onInputTextChange: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onToggleModelSelectorPopup: () -> Unit,
    onModelSelectorVisibilityChange: (Boolean) -> Unit,
    onSelectModelLabel: (String) -> Unit,
    onToggleFeaturePanel: () -> Unit,
    onFeaturePanelVisibilityChange: (Boolean) -> Unit,
    onToggleAttachmentPanel: () -> Unit,
    onAttachmentPanelVisibilityChange: (Boolean) -> Unit,
    onAddAttachment: (String) -> Unit,
    onOpenFullscreenEditor: () -> Unit,
    onSubmitPrompt: () -> Unit,
    onEnqueuePendingPrompt: () -> Boolean,
    onPendingQueueExpandedChange: (Boolean) -> Unit,
    onDeletePendingQueueMessage: (Long) -> Unit,
    onEditPendingQueueMessage: (Long) -> Unit,
    onSendPendingQueueMessage: (Long) -> Unit,
    onCancelProcessing: () -> Unit,
    onManageMemory: () -> Unit,
    onManualMemoryUpdate: () -> Unit,
    onManageModels: () -> Unit,
    onToggleThinkingMode: () -> Unit,
    onThinkingQualityLevelChange: (Int) -> Unit,
    onToggleMaxContextMode: () -> Unit,
    onToggleMemoryAutoUpdate: () -> Unit,
    onToggleAutoRead: () -> Unit,
    onToggleAutoApprove: () -> Unit,
    onToggleTools: () -> Unit,
    onToggleDisableStreamOutput: () -> Unit,
    onToggleDisableUserPreferenceDescription: () -> Unit,
    onToggleDisableStatusTags: () -> Unit,
    onManageTools: () -> Unit,
    onAuxiliaryActionClick: (() -> Unit)?,
) {
    val canSendMessage = inputText.isNotBlank() || attachments.isNotEmpty()
    val showQueueAction = isInputProcessing && inputText.isNotBlank()
    val bridgedAttachments =
        attachments.map { attachment ->
            AttachmentInfo(
                filePath = attachment,
                fileName = attachment,
                mimeType = "text/plain",
                fileSize = 0L,
            )
        }
    val actionIcon: ImageVector =
        when {
            showQueueAction -> Icons.Default.Add
            isInputProcessing -> Icons.Default.Close
            canSendMessage -> Icons.Default.Send
            else -> Icons.Default.Mic
        }
    val actionButtonBackground =
        when {
            showQueueAction -> Color(0xFFF79009)
            isInputProcessing -> Color(0xFFD92D20)
            else -> Color(0xFF6177B2)
        }

    AgentChatInputWorkbenchBridge(
        modifier = modifier,
        inputText = inputText,
        onInputTextChange = onInputTextChange,
        placeholderText = "\u8BF7\u8F93\u5165\u60A8\u7684\u95EE\u9898...",
        attachments = bridgedAttachments,
        onRemoveAttachment = { onRemoveAttachment(it.filePath) },
        onInsertAttachment = { attachment ->
            val token = "@${attachment.fileName}"
            val nextText =
                when {
                    inputText.isBlank() -> "$token "
                    inputText.contains(token) -> inputText
                    inputText.endsWith(" ") -> inputText + token + " "
                    else -> "$inputText $token "
                }
            onInputTextChange(nextText)
        },
        currentModelLabel = currentModelLabel,
        availableModelLabels = availableModelLabels,
        modelSelectorExpanded = showModelSelectorPopup,
        onModelSelectorClick = onToggleModelSelectorPopup,
        onModelSelectorVisibilityChange = onModelSelectorVisibilityChange,
        onSelectModelLabel = onSelectModelLabel,
        showFeaturePanel = showFeaturePanel,
        onToggleFeaturePanel = onToggleFeaturePanel,
        onFeaturePanelVisibilityChange = onFeaturePanelVisibilityChange,
        showAttachmentPanel = showAttachmentPanel,
        onToggleAttachmentPanel = onToggleAttachmentPanel,
        onAttachmentPanelVisibilityChange = onAttachmentPanelVisibilityChange,
        onAddAttachment = onAddAttachment,
        onOpenFullscreenEditor = onOpenFullscreenEditor,
        enableThinkingMode = enableThinking,
        thinkingQualityLevel = thinkingQualityLevel,
        enableMaxContextMode = enableMaxContextMode,
        baseContextLengthInK = baseContextLengthInK,
        maxContextLengthInK = maxContextLengthInK,
        currentMemoryProfileLabel = currentMemoryProfileLabel,
        enableMemoryAutoUpdate = enableMemoryAutoUpdate,
        isAutoReadEnabled = isAutoReadEnabled,
        isAutoApproveEnabled = isAutoApproveEnabled,
        disableTools = !enableTools,
        disableStreamOutput = disableStreamOutput,
        disableUserPreferenceDescription = disableUserPreferenceDescription,
        disableStatusTags = disableStatusTags,
        onManageMemory = onManageMemory,
        onManualMemoryUpdate = onManualMemoryUpdate,
        onManageModels = onManageModels,
        onToggleThinkingMode = onToggleThinkingMode,
        onThinkingQualityLevelChange = onThinkingQualityLevelChange,
        onToggleEnableMaxContextMode = onToggleMaxContextMode,
        onToggleMemoryAutoUpdate = onToggleMemoryAutoUpdate,
        onToggleAutoRead = onToggleAutoRead,
        onToggleAutoApprove = onToggleAutoApprove,
        onToggleTools = onToggleTools,
        onToggleDisableStreamOutput = onToggleDisableStreamOutput,
        onToggleDisableUserPreferenceDescription = onToggleDisableUserPreferenceDescription,
        onToggleDisableStatusTags = onToggleDisableStatusTags,
        onManageTools = onManageTools,
        canSendMessage = canSendMessage,
        actionButtonBackground = actionButtonBackground,
        actionButtonIconTint = Color.White,
        actionIcon = actionIcon,
        actionContentDescription =
            when {
                isInputProcessing -> "Cancel"
                canSendMessage -> "Send"
                else -> "Voice input"
            },
        showProcessingStatus = isInputProcessing,
        processingLabel = inputProcessingLabel,
        processingProgressValue = inputProcessingProgress,
        pendingQueueMessages = pendingQueueMessages,
        isPendingQueueExpanded = isPendingQueueExpanded,
        onPendingQueueExpandedChange = onPendingQueueExpandedChange,
        onDeletePendingQueueMessage = onDeletePendingQueueMessage,
        onEditPendingQueueMessage = onEditPendingQueueMessage,
        onSendPendingQueueMessage = onSendPendingQueueMessage,
        onActionClick = {
            if (showQueueAction) {
                onEnqueuePendingPrompt()
            } else if (isInputProcessing) {
                onCancelProcessing()
            } else if (canSendMessage) {
                onSubmitPrompt()
            } else {
                onAuxiliaryActionClick?.invoke()
            }
        },
    )
}
