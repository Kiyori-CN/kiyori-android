package com.ai.assistance.operit.ui.features.chat.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.kiyori.R

@Composable
fun ChatMessageActionDialogsBridge(
    message: ChatActionSurfaceMessage,
    showMessageInfoDialog: Boolean,
    showHiddenUserMessageDialog: Boolean,
    copyPreviewText: String?,
    onDismissMessageInfo: () -> Unit,
    onDismissHiddenUserMessage: () -> Unit,
    onDismissCopyPreview: () -> Unit,
) {
    if (showMessageInfoDialog) {
        ChatMessageInfoDialogBridge(
            message = message,
            onDismiss = onDismissMessageInfo,
        )
    }

    if (showHiddenUserMessageDialog) {
        HiddenUserMessageDialogBridge(
            message = message,
            onDismiss = onDismissHiddenUserMessage,
        )
    }

    copyPreviewText?.let { previewText ->
        MessageCopyPreviewBottomSheetBridge(
            text = previewText,
            onDismiss = onDismissCopyPreview,
        )
    }
}

@Composable
private fun HiddenUserMessageDialogBridge(
    message: ChatActionSurfaceMessage,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.chat_hidden_user_message_badge)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
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
