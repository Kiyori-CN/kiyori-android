package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.kiyori.R

@Composable
fun ChatAreaSelectionOverlayBridge(
    selectedMessageIndices: Set<Int>,
    selectableMessageIndices: Set<Int>,
    showDeleteConfirmDialog: Boolean,
    onExit: () -> Unit,
    onSelectAllChange: (Set<Int>) -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onConfirmDeleteSelected: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChatMultiSelectToolbarBridge(
        selectedMessageIndices = selectedMessageIndices,
        selectableMessageIndices = selectableMessageIndices,
        onExit = onExit,
        onSelectAllChange = onSelectAllChange,
        onShareSelected = onShareSelected,
        onDeleteSelected = onDeleteSelectedClick,
        modifier = modifier,
    )

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
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
                TextButton(onClick = onConfirmDeleteSelected) {
                    Text(stringResource(R.string.confirm_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
