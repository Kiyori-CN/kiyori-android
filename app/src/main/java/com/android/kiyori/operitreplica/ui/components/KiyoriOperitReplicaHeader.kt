package com.android.kiyori.operitreplica.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.ui.features.chat.components.ChatHeader

@Composable
internal fun KiyoriOperitReplicaHeader(
    showHistoryPanel: Boolean,
    immersiveMode: Boolean,
    currentCharacterLabel: String,
    usageProgress: Float,
    contextUsagePercentage: Float,
    currentWindowSize: Int,
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChatHeader(
            showChatHistorySelector = showHistoryPanel,
            onToggleChatHistorySelector = onHistoryClick,
            modifier = Modifier.weight(1f),
            onLaunchFloatingWindow = onPipClick,
            isFloatingMode = false && immersiveMode,
            runningTaskCount = 0,
            activeCharacterName = currentCharacterLabel,
            activeCharacterAvatarUri = activeCharacterAvatarUri,
            onCharacterClick = onCharacterClick,
        )

        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clickable(onClick = onStatsMenuToggle)
                    .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = usageProgress,
                modifier = Modifier.fillMaxSize(),
                color =
                    when {
                        contextUsagePercentage > 90f -> Color(0xFFD92D20)
                        contextUsagePercentage > 75f -> Color(0xFFF79009)
                        else -> Color(0xFF6177B2)
                    },
                trackColor = Color(0x14000000),
                strokeWidth = 3.dp,
            )
            Text(
                text = contextUsagePercentage.toInt().toString(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6177B2),
            )
            DropdownMenu(
                expanded = showStatsMenu,
                onDismissRequest = onStatsMenuDismiss,
                modifier = Modifier.background(Color.White),
            ) {
                DropdownMenuItem(text = { Text("Context Window: $currentWindowSize") }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text("Input Tokens: $inputTokenCount") }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text("Output Tokens: $outputTokenCount") }, onClick = {}, enabled = false)
                DropdownMenuItem(
                    text = { Text("Total Tokens: ${inputTokenCount + outputTokenCount}") },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
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
