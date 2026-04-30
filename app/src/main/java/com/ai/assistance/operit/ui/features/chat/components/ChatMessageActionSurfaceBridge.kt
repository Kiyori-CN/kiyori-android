package com.ai.assistance.operit.ui.features.chat.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R

enum class ChatActionSurfaceSender {
    USER,
    AI,
    SYSTEM,
}

data class ChatActionSurfaceMessage(
    val sender: ChatActionSurfaceSender,
    val content: String,
    val meta: String? = null,
)

data class ChatActionSurfaceCallbacks(
    val onEditMessage: ((Int, ChatActionSurfaceMessage, String) -> Unit)? = null,
    val onDeleteMessage: ((Int) -> Unit)? = null,
    val onRollbackToMessage: ((Int) -> Unit)? = null,
    val onRegenerateMessage: ((Int) -> Unit)? = null,
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
    callbacks: ChatActionSurfaceCallbacks,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showMessageInfoDialog by remember { mutableStateOf(false) }
    var copyPreviewText by remember { mutableStateOf<String?>(null) }
    val isActionable = message.sender == ChatActionSurfaceSender.USER || message.sender == ChatActionSurfaceSender.AI

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
                        }
                    },
                    onLongClick = {
                        if (!isMultiSelectMode && isActionable) {
                            showContextMenu = true
                        }
                    },
                ),
    ) {
        content()

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.width(180.dp),
        ) {
            ChatActionMenuItem(
                label = stringResource(R.string.copy_message),
                icon = Icons.Default.ContentCopy,
                onClick = {
                    copyPreviewText = message.content
                    showContextMenu = false
                },
            )
            ChatActionMenuItem(
                label = stringResource(R.string.read_message),
                icon = Icons.Rounded.VolumeUp,
                onClick = {
                    callbacks.onSpeakMessage?.invoke(message.content)
                    showContextMenu = false
                },
            )

            when (message.sender) {
                ChatActionSurfaceSender.USER -> {
                    ChatActionMenuItem(
                        label = stringResource(R.string.edit_and_resend),
                        icon = Icons.Default.Edit,
                        onClick = {
                            callbacks.onEditMessage?.invoke(index, message, "user")
                            showContextMenu = false
                        },
                    )
                    ChatActionMenuItem(
                        label = stringResource(R.string.rollback_to_here),
                        icon = Icons.Default.DeleteSweep,
                        onClick = {
                            callbacks.onRollbackToMessage?.invoke(index)
                            showContextMenu = false
                        },
                    )
                }
                ChatActionSurfaceSender.AI -> {
                    ChatActionMenuItem(
                        label = stringResource(R.string.chat_regenerate_single),
                        icon = Icons.Default.Refresh,
                        onClick = {
                            callbacks.onRegenerateMessage?.invoke(index)
                            showContextMenu = false
                        },
                    )
                    ChatActionMenuItem(
                        label = stringResource(R.string.modify_memory),
                        icon = Icons.Default.AutoFixHigh,
                        onClick = {
                            callbacks.onEditMessage?.invoke(index, message, "ai")
                            showContextMenu = false
                        },
                    )
                    ChatActionMenuItem(
                        label = stringResource(R.string.reply_message),
                        icon = Icons.Default.Reply,
                        onClick = {
                            callbacks.onReplyToMessage?.invoke(message)
                            showContextMenu = false
                        },
                    )
                }
                ChatActionSurfaceSender.SYSTEM -> Unit
            }

            ChatActionMenuItem(
                label = stringResource(R.string.delete),
                icon = Icons.Default.Delete,
                onClick = {
                    callbacks.onDeleteMessage?.invoke(index)
                    showContextMenu = false
                },
            )
            ChatActionMenuItem(
                label = stringResource(R.string.insert_summary),
                icon = Icons.Default.Summarize,
                onClick = {
                    callbacks.onInsertSummary?.invoke(index, message)
                    showContextMenu = false
                },
            )
            ChatActionMenuItem(
                label = stringResource(R.string.create_branch),
                icon = Icons.Default.AccountTree,
                onClick = {
                    callbacks.onCreateBranch?.invoke(index)
                    showContextMenu = false
                },
            )
            ChatActionMenuItem(
                label = stringResource(R.string.info),
                icon = Icons.Default.Info,
                onClick = {
                    showContextMenu = false
                    showMessageInfoDialog = true
                },
            )
            ChatActionMenuItem(
                label = stringResource(R.string.multi_select),
                icon = Icons.Default.CheckCircle,
                onClick = {
                    callbacks.onToggleMultiSelectMode?.invoke(index)
                    showContextMenu = false
                },
            )
        }

        if (showMessageInfoDialog) {
            ChatMessageInfoDialogBridge(
                message = message,
                onDismiss = { showMessageInfoDialog = false },
            )
        }

        copyPreviewText?.let { previewText ->
            MessageCopyPreviewBottomSheetBridge(
                text = previewText,
                onDismiss = { copyPreviewText = null },
            )
        }
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

@Composable
private fun ChatActionMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = Modifier.height(36.dp),
    )
}

@Composable
private fun ChatMessageInfoDialogBridge(
    message: ChatActionSurfaceMessage,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.message_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${stringResource(R.string.message_info_sender)}: ${message.sender.name.lowercase()}")
                Text("${stringResource(R.string.message_info_message_time)}: ${message.meta ?: stringResource(R.string.message_info_unavailable)}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageCopyPreviewBottomSheetBridge(
    text: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.copy_message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            BasicTextField(
                value = text,
                onValueChange = {},
                readOnly = true,
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .widthIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(
                            context,
                            context.getString(R.string.message_copied_to_clipboard),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                ) {
                    Text(text = stringResource(R.string.copy_all_content))
                }
            }
        }
    }
}
