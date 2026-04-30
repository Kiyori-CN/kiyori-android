package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R

@Composable
fun ChatMessageActionMenuBridge(
    expanded: Boolean,
    index: Int,
    message: ChatActionSurfaceMessage,
    callbacks: ChatActionSurfaceCallbacks,
    onDismiss: () -> Unit,
    onCopyPreview: (String) -> Unit,
    onShowInfo: () -> Unit,
) {
    val isHiddenUserMessage =
        message.sender == ChatActionSurfaceSender.USER &&
            message.displayMode == ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER

    OperitDropdownMenuBridge(
        expanded = expanded,
        onDismissRequest = onDismiss,
        width = 180.dp,
    ) {
        if (!isHiddenUserMessage) {
            ChatActionMenuItemBridge(
                label = stringResource(R.string.copy_message),
                icon = Icons.Default.ContentCopy,
                onClick = {
                    onCopyPreview(cleanOperitMessageCopyPreviewBridge(message.content))
                    onDismiss()
                },
            )
            ChatActionMenuItemBridge(
                label = stringResource(R.string.read_message),
                icon = Icons.Rounded.VolumeUp,
                onClick = {
                    callbacks.onSpeakMessage?.invoke(message.content)
                    onDismiss()
                },
            )
        }

        when (message.sender) {
            ChatActionSurfaceSender.USER -> {
                if (!isHiddenUserMessage) {
                    ChatActionMenuItemBridge(
                        label = stringResource(R.string.edit_and_resend),
                        icon = Icons.Default.Edit,
                        onClick = {
                            callbacks.onEditMessage?.invoke(index, message, "user")
                            onDismiss()
                        },
                    )
                }
                ChatActionMenuItemBridge(
                    label = stringResource(R.string.rollback_to_here),
                    icon = Icons.Default.DeleteSweep,
                    onClick = {
                        callbacks.onRollbackToMessage?.invoke(index)
                        onDismiss()
                    },
                )
            }

            ChatActionSurfaceSender.AI -> {
                ChatActionMenuItemBridge(
                    label = stringResource(R.string.chat_regenerate_single),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        callbacks.onRegenerateMessage?.invoke(index)
                        onDismiss()
                    },
                )
                ChatActionMenuItemBridge(
                    label = stringResource(R.string.modify_memory),
                    icon = Icons.Default.AutoFixHigh,
                    onClick = {
                        callbacks.onEditMessage?.invoke(index, message, "ai")
                        onDismiss()
                    },
                )
                ChatActionMenuItemBridge(
                    label = stringResource(R.string.reply_message),
                    icon = Icons.Default.Reply,
                    onClick = {
                        callbacks.onReplyToMessage?.invoke(message)
                        onDismiss()
                    },
                )
            }

            ChatActionSurfaceSender.SYSTEM -> Unit
        }

        if (message.sender == ChatActionSurfaceSender.AI && message.variantCount > 1) {
            ChatActionMenuItemBridge(
                label = stringResource(R.string.chat_delete_single_variant),
                icon = Icons.Default.Delete,
                onClick = {
                    callbacks.onDeleteCurrentMessageVariant?.invoke(index)
                    onDismiss()
                },
            )
        }

        ChatActionMenuItemBridge(
            label = stringResource(R.string.delete),
            icon = Icons.Default.Delete,
            onClick = {
                callbacks.onDeleteMessage?.invoke(index)
                onDismiss()
            },
        )
        ChatActionMenuItemBridge(
            label = stringResource(R.string.insert_summary),
            icon = Icons.Default.Summarize,
            onClick = {
                callbacks.onInsertSummary?.invoke(index, message)
                onDismiss()
            },
        )
        ChatActionMenuItemBridge(
            label = stringResource(R.string.create_branch),
            icon = Icons.Default.AccountTree,
            onClick = {
                callbacks.onCreateBranch?.invoke(index)
                onDismiss()
            },
        )
        ChatActionMenuItemBridge(
            label = stringResource(R.string.info),
            icon = Icons.Default.Info,
            onClick = {
                onDismiss()
                onShowInfo()
            },
        )
        ChatActionMenuItemBridge(
            label = stringResource(R.string.multi_select),
            icon = Icons.Default.CheckCircle,
            onClick = {
                callbacks.onToggleMultiSelectMode?.invoke(index)
                onDismiss()
            },
        )
    }
}

@Composable
private fun ChatActionMenuItemBridge(
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

private fun cleanOperitMessageCopyPreviewBridge(content: String): String {
    val tagsToRemove =
        listOf(
            "status",
            "think",
            "thinking",
            "search",
            "tool",
            "tool_result",
            "emotion",
            "workspace",
            "attachment",
        )
    var result = content
    tagsToRemove.forEach { tag ->
        result =
            result
                .replace(
                    Regex(
                        pattern = "<$tag\\b[^>]*>[\\s\\S]*?</$tag>",
                        options = setOf(RegexOption.IGNORE_CASE),
                    ),
                    "",
                )
                .replace(
                    Regex(
                        pattern = "<$tag\\b[^>]*/>",
                        options = setOf(RegexOption.IGNORE_CASE),
                    ),
                    "",
                )
    }

    return result
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[[^]]*]\\((?:data:|file:|content:)[^)]*\\)"), "")
        .trim()
}
