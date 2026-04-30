package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R

@Composable
fun ChatScreenHeaderBridge(
    modifier: Modifier = Modifier,
    showChatHistorySelector: Boolean,
    chatHeaderTransparent: Boolean,
    chatHeaderHistoryIconColor: Int? = null,
    chatHeaderPipIconColor: Int? = null,
    isFloatingMode: Boolean,
    runningTaskCount: Int,
    activeCharacterName: String,
    activeCharacterAvatarUri: String?,
    currentWindowSize: Int,
    maxWindowSizeInK: Float,
    inputTokenCount: Int,
    outputTokenCount: Int,
    showDetailedStats: Boolean,
    applyHostStatusBarPadding: Boolean = false,
    onToggleChatHistorySelector: () -> Unit,
    onLaunchFloatingWindow: () -> Unit,
    onCharacterSwitcherClick: () -> Unit,
    onStatsToggle: () -> Unit,
    onStatsDismiss: () -> Unit,
) {
    val hostPaddingModifier =
        if (applyHostStatusBarPadding) {
            Modifier.statusBarsPadding()
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(hostPaddingModifier)
                .background(
                    if (chatHeaderTransparent) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    },
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChatHeader(
            showChatHistorySelector = showChatHistorySelector,
            onToggleChatHistorySelector = onToggleChatHistorySelector,
            modifier = Modifier.weight(1f),
            isFloatingMode = isFloatingMode,
            onLaunchFloatingWindow = onLaunchFloatingWindow,
            historyIconColor = chatHeaderHistoryIconColor,
            pipIconColor = chatHeaderPipIconColor,
            runningTaskCount = runningTaskCount,
            activeCharacterName = activeCharacterName,
            activeCharacterAvatarUri = activeCharacterAvatarUri,
            onCharacterClick = onCharacterSwitcherClick,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val maxWindowSize = (maxWindowSizeInK * 1024).toInt()
            val totalTokenCount = inputTokenCount + outputTokenCount
            val contextUsagePercentage =
                if (maxWindowSize > 0) {
                    (currentWindowSize.toFloat() / maxWindowSize) * 100
                } else {
                    0f
                }

            Box {
                val progress = (contextUsagePercentage / 100f).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    label = "TokenProgressAnimation",
                )
                val progressColor =
                    when {
                        contextUsagePercentage > 90f -> MaterialTheme.colorScheme.error
                        contextUsagePercentage > 75f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                Box(
                    modifier =
                        Modifier
                            .clickable(onClick = onStatsToggle)
                            .size(32.dp)
                            .padding(3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = progressColor,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                    Text(
                        text = contextUsagePercentage.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor,
                    )
                }

                DropdownMenu(
                    expanded = showDetailedStats,
                    onDismissRequest = onStatsDismiss,
                    modifier =
                        Modifier
                            .width(IntrinsicSize.Min)
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.context_window, currentWindowSize.toString())) },
                        onClick = {},
                        enabled = false,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.input_tokens, inputTokenCount.toString())) },
                        onClick = {},
                        enabled = false,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.output_tokens, outputTokenCount.toString())) },
                        onClick = {},
                        enabled = false,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.total_tokens, totalTokenCount.toString()),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        onClick = {},
                        enabled = false,
                    )
                }
            }
        }
    }
}
