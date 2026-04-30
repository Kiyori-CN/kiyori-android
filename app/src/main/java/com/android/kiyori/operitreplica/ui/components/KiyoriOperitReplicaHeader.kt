package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.chat.components.ChatScreenHeaderBridge

@Composable
internal fun KiyoriOperitReplicaHeader(
    showHistoryPanel: Boolean,
    immersiveMode: Boolean,
    isInputProcessing: Boolean,
    chatHeaderTransparent: Boolean,
    chatHeaderHistoryIconColor: Int?,
    chatHeaderPipIconColor: Int?,
    currentCharacterLabel: String,
    currentWindowSize: Int,
    maxContextLengthInK: Float,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showStatsMenu: Boolean,
    onHistoryClick: () -> Unit,
    onPipClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onStatsMenuDismiss: () -> Unit,
    onStatsMenuToggle: () -> Unit,
) {
    val context = LocalContext.current
    val activeCharacterAvatarUri =
        "android.resource://${context.packageName}/${com.android.kiyori.R.drawable.ic_kiyori_operit_avatar}"

    ChatScreenHeaderBridge(
        showChatHistorySelector = showHistoryPanel,
        chatHeaderTransparent = chatHeaderTransparent,
        chatHeaderHistoryIconColor = chatHeaderHistoryIconColor,
        chatHeaderPipIconColor = chatHeaderPipIconColor,
        isFloatingMode = false && immersiveMode,
        runningTaskCount = if (isInputProcessing) 1 else 0,
        activeCharacterName = currentCharacterLabel,
        activeCharacterAvatarUri = activeCharacterAvatarUri,
        currentWindowSize = currentWindowSize,
        maxWindowSizeInK = maxContextLengthInK,
        inputTokenCount = inputTokenCount,
        outputTokenCount = outputTokenCount,
        showDetailedStats = showStatsMenu,
        applyHostStatusBarPadding = true,
        onToggleChatHistorySelector = onHistoryClick,
        onLaunchFloatingWindow = onPipClick,
        onCharacterSwitcherClick = onCharacterClick,
        onStatsToggle = onStatsMenuToggle,
        onStatsDismiss = onStatsMenuDismiss,
    )
}

@Composable
internal fun OperitHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(
                    color = if (selected) Color(0xFF6177B2).copy(alpha = 0.15f) else Color.Transparent,
                    shape = CircleShape,
                ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint =
                    if (selected) {
                        Color(0xFF6177B2)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun OperitSecondaryIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    selectedTint: Color = Color(0xFF6177B2),
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .background(
                    color = if (selected) selectedTint.copy(alpha = 0.15f) else Color.Transparent,
                    shape = CircleShape,
                ),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint =
                    if (selected) {
                        selectedTint
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
