package com.ai.assistance.operit.ui.features.chat.components.style.input.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.data.model.AttachmentInfo
import com.ai.assistance.operit.ui.features.chat.components.AttachmentChip
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingMessageQueuePanel
import com.ai.assistance.operit.ui.features.chat.components.style.input.common.PendingQueueMessageItem

@Composable
fun AgentChatInputWorkbenchBridge(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    placeholderText: String,
    attachments: List<AttachmentInfo>,
    onRemoveAttachment: (AttachmentInfo) -> Unit,
    onInsertAttachment: (AttachmentInfo) -> Unit,
    currentModelLabel: String,
    availableModelLabels: List<String>,
    modelSelectorExpanded: Boolean,
    onModelSelectorClick: () -> Unit,
    onModelSelectorVisibilityChange: (Boolean) -> Unit,
    onSelectModelLabel: (String) -> Unit,
    showFeaturePanel: Boolean,
    onToggleFeaturePanel: () -> Unit,
    onFeaturePanelVisibilityChange: (Boolean) -> Unit,
    showAttachmentPanel: Boolean,
    onToggleAttachmentPanel: () -> Unit,
    onAttachmentPanelVisibilityChange: (Boolean) -> Unit,
    onAddAttachment: (String) -> Unit,
    onOpenFullscreenEditor: () -> Unit,
    enableThinkingMode: Boolean,
    thinkingQualityLevel: Int,
    enableMaxContextMode: Boolean,
    baseContextLengthInK: Float,
    maxContextLengthInK: Float,
    currentMemoryProfileLabel: String,
    enableMemoryAutoUpdate: Boolean,
    isAutoReadEnabled: Boolean,
    isAutoApproveEnabled: Boolean,
    disableTools: Boolean,
    disableStreamOutput: Boolean,
    disableUserPreferenceDescription: Boolean,
    disableStatusTags: Boolean,
    onManageMemory: () -> Unit,
    onManualMemoryUpdate: () -> Unit,
    onManageModels: () -> Unit,
    onToggleThinkingMode: () -> Unit,
    onThinkingQualityLevelChange: (Int) -> Unit,
    onToggleEnableMaxContextMode: () -> Unit,
    onToggleMemoryAutoUpdate: () -> Unit,
    onToggleAutoRead: () -> Unit,
    onToggleAutoApprove: () -> Unit,
    onToggleTools: () -> Unit,
    onToggleDisableStreamOutput: () -> Unit,
    onToggleDisableUserPreferenceDescription: () -> Unit,
    onToggleDisableStatusTags: () -> Unit,
    onManageTools: () -> Unit,
    canSendMessage: Boolean,
    actionButtonBackground: Color,
    actionButtonIconTint: Color,
    actionIcon: ImageVector,
    actionContentDescription: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProcessingStatus: Boolean = false,
    processingLabel: String = "",
    processingProgressValue: Float = 0f,
    processingProgressColor: Color = Color(0xFF6177B2),
    pendingQueueMessages: List<PendingQueueMessageItem> = emptyList(),
    isPendingQueueExpanded: Boolean = true,
    onPendingQueueExpandedChange: (Boolean) -> Unit = {},
    onDeletePendingQueueMessage: (Long) -> Unit = {},
    onEditPendingQueueMessage: (Long) -> Unit = {},
    onSendPendingQueueMessage: (Long) -> Unit = {},
    enableEnterToSend: Boolean = true,
) {
    val modelLabel =
        if (currentModelLabel.length > 26) {
            currentModelLabel.take(26) + "..."
        } else {
            currentModelLabel
        }

    Column(modifier = modifier) {
        PendingMessageQueuePanel(
            queuedMessages = pendingQueueMessages,
            expanded = isPendingQueueExpanded,
            onExpandedChange = onPendingQueueExpandedChange,
            onDeleteMessage = onDeletePendingQueueMessage,
            onEditMessage = onEditPendingQueueMessage,
            onSendMessage = onSendPendingQueueMessage,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            itemColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )

        if (attachments.isNotEmpty()) {
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(attachments) { attachment ->
                    AttachmentChip(
                        attachmentInfo = attachment,
                        onRemove = { onRemoveAttachment(attachment) },
                        onInsert = { onInsertAttachment(attachment) },
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (showProcessingStatus && processingLabel.isNotBlank()) {
                    Text(
                        text = processingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            text = placeholderText,
                            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 132.dp),
                    textStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                    maxLines = 6,
                    minLines = 1,
                    singleLine = false,
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = if (enableEnterToSend) ImeAction.Send else ImeAction.Default,
                        ),
                    keyboardActions =
                        if (enableEnterToSend) {
                            KeyboardActions(onSend = { if (canSendMessage) onActionClick() })
                        } else {
                            KeyboardActions()
                        },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = onOpenFullscreenEditor) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen input",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier =
                                Modifier
                                    .widthIn(min = 0.dp, max = 220.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onModelSelectorClick),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = modelLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector =
                                        if (modelSelectorExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .padding(start = 6.dp)
                                .size(34.dp)
                                .clickable(onClick = onToggleFeaturePanel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Settings options",
                            tint =
                                if (showFeaturePanel) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .clickable(onClick = onToggleAttachmentPanel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint =
                                if (showAttachmentPanel) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showProcessingStatus) {
                            CircularProgressIndicator(
                                progress = processingProgressValue.coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxSize(),
                                color = processingProgressColor,
                                trackColor = processingProgressColor.copy(alpha = 0.2f),
                                strokeWidth = 2.dp,
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(actionButtonBackground, CircleShape)
                                    .clickable(onClick = onActionClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = actionContentDescription,
                                tint = actionButtonIconTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        AgentExtraSettingsPopupBridge(
            visible = showFeaturePanel,
            currentMemoryProfileLabel = currentMemoryProfileLabel,
            enableMemoryAutoUpdate = enableMemoryAutoUpdate,
            isAutoReadEnabled = isAutoReadEnabled,
            isAutoApproveEnabled = isAutoApproveEnabled,
            disableTools = disableTools,
            disableStreamOutput = disableStreamOutput,
            disableUserPreferenceDescription = disableUserPreferenceDescription,
            disableStatusTags = disableStatusTags,
            onManageMemory = onManageMemory,
            onManualMemoryUpdate = onManualMemoryUpdate,
            onToggleMemoryAutoUpdate = onToggleMemoryAutoUpdate,
            onToggleAutoRead = onToggleAutoRead,
            onToggleAutoApprove = onToggleAutoApprove,
            onToggleTools = onToggleTools,
            onToggleDisableStreamOutput = onToggleDisableStreamOutput,
            onToggleDisableUserPreferenceDescription = onToggleDisableUserPreferenceDescription,
            onToggleDisableStatusTags = onToggleDisableStatusTags,
            onManageTools = onManageTools,
            onDismiss = { onFeaturePanelVisibilityChange(false) },
        )

        AgentModelSelectorPopupBridge(
            visible = modelSelectorExpanded,
            currentModelLabel = currentModelLabel,
            availableModelLabels = availableModelLabels,
            enableThinkingMode = enableThinkingMode,
            thinkingQualityLevel = thinkingQualityLevel,
            enableMaxContextMode = enableMaxContextMode,
            baseContextLengthInK = baseContextLengthInK,
            maxContextLengthInK = maxContextLengthInK,
            onToggleThinkingMode = onToggleThinkingMode,
            onThinkingQualityLevelChange = onThinkingQualityLevelChange,
            onToggleEnableMaxContextMode = onToggleEnableMaxContextMode,
            onSelectModelLabel = onSelectModelLabel,
            onManageModels = onManageModels,
            onDismiss = { onModelSelectorVisibilityChange(false) },
        )

        AttachmentSelectorPopupPanelBridge(
            visible = showAttachmentPanel,
            onAddAttachment = onAddAttachment,
            onDismiss = { onAttachmentPanelVisibilityChange(false) },
        )
    }
}
