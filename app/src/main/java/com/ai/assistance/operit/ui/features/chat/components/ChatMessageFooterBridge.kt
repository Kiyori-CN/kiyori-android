package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.kiyori.R
import java.util.Locale

@Composable
fun ChatMessageFooterBridge(
    index: Int,
    message: ChatActionSurfaceMessage,
    showMessageTokenStats: Boolean,
    showMessageTimingStats: Boolean,
    onSwitchMessageVariant: ((Int, Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!shouldShowChatMessageFooterBridge(message, showMessageTokenStats, showMessageTimingStats)) {
        return
    }

    MessageFooterBarBridge(
        message = message,
        showMessageTokenStats = showMessageTokenStats,
        showMessageTimingStats = showMessageTimingStats,
        onSelectVariant = { targetVariantIndex ->
            onSwitchMessageVariant?.invoke(index, targetVariantIndex)
        },
        modifier = modifier,
    )
}

internal fun shouldShowChatMessageFooterBridge(
    message: ChatActionSurfaceMessage,
    showMessageTokenStats: Boolean,
    showMessageTimingStats: Boolean,
): Boolean {
    return message.sender == ChatActionSurfaceSender.AI &&
        (
            message.variantCount > 1 ||
                (showMessageTokenStats && hasDisplayableTokenStats(message)) ||
                (showMessageTimingStats && hasDisplayableTimingStats(message))
            )
}

@Composable
private fun MessageFooterBarBridge(
    message: ChatActionSurfaceMessage,
    showMessageTokenStats: Boolean,
    showMessageTimingStats: Boolean,
    onSelectVariant: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPrevious = message.selectedVariantIndex > 0
    val hasNext = message.selectedVariantIndex < message.variantCount - 1
    val context = LocalContext.current
    val tokenSummary =
        remember(message.inputTokens, message.cachedInputTokens, message.outputTokens) {
            val totalTokens = message.inputTokens + message.outputTokens
            context.getString(
                R.string.chat_message_token_stats_compact,
                totalTokens,
                message.cachedInputTokens,
                message.inputTokens,
                message.outputTokens,
            )
        }
    val timeSummary =
        remember(message.waitDurationMs, message.outputDurationMs) {
            val totalDuration = (message.waitDurationMs + message.outputDurationMs).coerceAtLeast(0L)
            context.getString(
                R.string.chat_message_timing_stats_compact,
                formatCompactDuration(totalDuration),
                formatCompactDuration(message.waitDurationMs),
                formatCompactDuration(message.outputDurationMs),
            )
        }
    val statsTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)

    Column(
        modifier = modifier.padding(start = 16.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (message.variantCount > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.chat_previous_variant),
                    tint =
                        if (hasPrevious) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clickable(enabled = hasPrevious) {
                                onSelectVariant(message.selectedVariantIndex - 1)
                            },
                )
                Text(
                    text =
                        stringResource(
                            R.string.chat_message_variant_counter,
                            message.selectedVariantIndex + 1,
                            message.variantCount,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.chat_next_variant),
                    tint =
                        if (hasNext) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                    modifier =
                        Modifier
                            .size(16.dp)
                            .clickable(enabled = hasNext) {
                                onSelectVariant(message.selectedVariantIndex + 1)
                            },
                )
            }
        }

        if (showMessageTokenStats && hasDisplayableTokenStats(message)) {
            Text(
                text = tokenSummary,
                style = MaterialTheme.typography.labelSmall,
                color = statsTextColor,
            )
        }

        if (showMessageTimingStats && hasDisplayableTimingStats(message)) {
            Text(
                text = timeSummary,
                style = MaterialTheme.typography.labelSmall,
                color = statsTextColor,
            )
        }
    }
}

private fun hasDisplayableTokenStats(message: ChatActionSurfaceMessage): Boolean {
    return message.inputTokens > 0 || message.cachedInputTokens > 0 || message.outputTokens > 0
}

private fun hasDisplayableTimingStats(message: ChatActionSurfaceMessage): Boolean {
    return message.waitDurationMs > 0L || message.outputDurationMs > 0L
}

private fun formatCompactDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0ms"
    return if (durationMs >= 1000L) {
        if (durationMs >= 10_000L) {
            String.format(Locale.getDefault(), "%.0fs", durationMs / 1000f)
        } else {
            String.format(Locale.getDefault(), "%.1fs", durationMs / 1000f)
        }
    } else {
        "${durationMs}ms"
    }
}
