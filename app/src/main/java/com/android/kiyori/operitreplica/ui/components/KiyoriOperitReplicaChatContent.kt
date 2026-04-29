package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.kiyori.operitreplica.model.OperitReplicaMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessageRole
import com.android.kiyori.operitreplica.ui.components.message.KiyoriOperitReplicaMessageBubble
import com.android.kiyori.operitreplica.ui.components.message.KiyoriOperitReplicaSystemMessage

@Composable
internal fun KiyoriOperitReplicaChatContent(
    modifier: Modifier = Modifier,
    bottomDockPadding: Dp,
    immersiveMode: Boolean,
    showHistoryPanel: Boolean,
    currentCharacterLabel: String,
    usageProgress: Float,
    contextUsagePercentage: Float,
    currentWindowSize: Int,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showStatsMenu: Boolean,
    activeMessages: List<OperitReplicaMessage>,
    onHistoryClick: () -> Unit,
    onPipClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onStatsMenuDismiss: () -> Unit,
    onStatsMenuToggle: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp),
    ) {
        KiyoriOperitReplicaHeader(
            showHistoryPanel = showHistoryPanel,
            immersiveMode = immersiveMode,
            currentCharacterLabel = currentCharacterLabel,
            usageProgress = usageProgress,
            contextUsagePercentage = contextUsagePercentage,
            currentWindowSize = currentWindowSize,
            inputTokenCount = inputTokenCount,
            outputTokenCount = outputTokenCount,
            showStatsMenu = showStatsMenu,
            onHistoryClick = onHistoryClick,
            onPipClick = onPipClick,
            onCharacterClick = onCharacterClick,
            onStatsMenuDismiss = onStatsMenuDismiss,
            onStatsMenuToggle = onStatsMenuToggle,
        )

        activeMessages.forEach { message ->
            if (message.role == OperitReplicaMessageRole.System) {
                KiyoriOperitReplicaSystemMessage(message = message)
            } else {
                KiyoriOperitReplicaMessageBubble(message = message)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(160.dp + bottomDockPadding))
    }
}
