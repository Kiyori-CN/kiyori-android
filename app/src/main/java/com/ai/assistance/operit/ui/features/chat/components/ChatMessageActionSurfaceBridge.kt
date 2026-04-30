package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.kiyori.R

enum class ChatActionSurfaceSender {
    USER,
    AI,
    SYSTEM,
}

enum class ChatActionSurfaceDisplayMode {
    NORMAL,
    HIDDEN_PLACEHOLDER,
}

data class ChatActionSurfaceMessage(
    val sender: ChatActionSurfaceSender,
    val content: String,
    val meta: String? = null,
    val roleName: String = "",
    val modelName: String = "",
    val provider: String = "",
    val timestamp: Long = 0L,
    val displayMode: ChatActionSurfaceDisplayMode = ChatActionSurfaceDisplayMode.NORMAL,
    val selectedVariantIndex: Int = 0,
    val variantCount: Int = 1,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cachedInputTokens: Int = 0,
    val waitDurationMs: Long = 0L,
    val outputDurationMs: Long = 0L,
)

data class ChatActionSurfaceCallbacks(
    val onEditMessage: ((Int, ChatActionSurfaceMessage, String) -> Unit)? = null,
    val onDeleteMessage: ((Int) -> Unit)? = null,
    val onRollbackToMessage: ((Int) -> Unit)? = null,
    val onRegenerateMessage: ((Int) -> Unit)? = null,
    val onSwitchMessageVariant: ((Int, Int) -> Unit)? = null,
    val onDeleteCurrentMessageVariant: ((Int) -> Unit)? = null,
    val onReplyToMessage: ((ChatActionSurfaceMessage) -> Unit)? = null,
    val onInsertSummary: ((Int, ChatActionSurfaceMessage) -> Unit)? = null,
    val onCreateBranch: ((Int) -> Unit)? = null,
    val onSpeakMessage: ((String) -> Unit)? = null,
    val onToggleMultiSelectMode: ((Int?) -> Unit)? = null,
    val onToggleMessageSelection: ((Int) -> Unit)? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageActionSurfaceBridge(
    index: Int,
    message: ChatActionSurfaceMessage,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    showMessageTokenStats: Boolean,
    showMessageTimingStats: Boolean,
    callbacks: ChatActionSurfaceCallbacks,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showMessageInfoDialog by remember { mutableStateOf(false) }
    var showHiddenUserMessageDialog by remember { mutableStateOf(false) }
    var copyPreviewText by remember { mutableStateOf<String?>(null) }
    val isActionable = message.sender == ChatActionSurfaceSender.USER || message.sender == ChatActionSurfaceSender.AI
    val isHiddenUserMessage =
        message.sender == ChatActionSurfaceSender.USER &&
            message.displayMode == ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER

    Box(
        modifier =
            modifier
                .alpha(1f)
                .then(
                    if (isSelected) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .combinedClickable(
                    onClick = {
                        if (isMultiSelectMode && isActionable) {
                            callbacks.onToggleMessageSelection?.invoke(index)
                        } else if (!isMultiSelectMode && isHiddenUserMessage) {
                            showHiddenUserMessageDialog = true
                        }
                    },
                    onLongClick = {
                        if (!isMultiSelectMode && isActionable) {
                            showContextMenu = true
                        }
                    },
                ),
    ) {
        Column {
            content()

            ChatMessageFooterBridge(
                index = index,
                message = message,
                showMessageTokenStats = showMessageTokenStats,
                showMessageTimingStats = showMessageTimingStats,
                onSwitchMessageVariant = callbacks.onSwitchMessageVariant,
            )
        }

        ChatMessageActionMenuBridge(
            expanded = showContextMenu,
            index = index,
            message = message,
            callbacks = callbacks,
            onDismiss = { showContextMenu = false },
            onCopyPreview = { copyPreviewText = it },
            onShowInfo = { showMessageInfoDialog = true },
        )

        ChatMessageActionDialogsBridge(
            message = message,
            showMessageInfoDialog = showMessageInfoDialog,
            showHiddenUserMessageDialog = showHiddenUserMessageDialog,
            copyPreviewText = copyPreviewText,
            onDismissMessageInfo = { showMessageInfoDialog = false },
            onDismissHiddenUserMessage = { showHiddenUserMessageDialog = false },
            onDismissCopyPreview = { copyPreviewText = null },
        )
    }
}

@Composable
fun ChatMultiSelectToolbarBridge(
    selectedMessageIndices: Set<Int>,
    selectableMessageIndices: Set<Int>,
    onExit: () -> Unit,
    onSelectAllChange: (Set<Int>) -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.exit_multi_select),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text =
                    if (selectedMessageIndices.isEmpty()) {
                        stringResource(R.string.multi_select)
                    } else {
                        stringResource(R.string.selected_count, selectedMessageIndices.size)
                    },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val allSelectableSelected =
                selectableMessageIndices.isNotEmpty() &&
                    selectableMessageIndices.all { selectedMessageIndices.contains(it) }
            TextButton(
                onClick = {
                    onSelectAllChange(
                        if (allSelectableSelected) {
                            emptySet()
                        } else {
                            selectableMessageIndices
                        },
                    )
                },
                enabled = selectableMessageIndices.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription =
                        stringResource(
                            if (allSelectableSelected) {
                                R.string.clear_selection
                            } else {
                                R.string.select_all_messages
                            },
                        ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            FilledIconButton(
                onClick = onShareSelected,
                enabled = selectedMessageIndices.isNotEmpty(),
                modifier = Modifier.size(32.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.share_selected),
                    modifier = Modifier.size(16.dp),
                )
            }
            FilledIconButton(
                onClick = onDeleteSelected,
                enabled = selectedMessageIndices.isNotEmpty(),
                modifier = Modifier.size(32.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_selected),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
