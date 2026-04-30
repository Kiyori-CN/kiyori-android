package com.ai.assistance.operit.ui.features.chat.components.style.cursor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.chat.components.ChatAreaDisplayPreferencesBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.part.ChatMarkupRendererBridge

@Composable
fun AiMessageComposableBridge(
    message: ChatActionSurfaceMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    showModelProvider: Boolean = false,
    showModelName: Boolean = false,
    showRoleName: Boolean = false,
    displayPreferences: ChatAreaDisplayPreferencesBridge = ChatAreaDisplayPreferencesBridge(),
) {
    val detailText =
        remember(
            showRoleName,
            showModelName,
            showModelProvider,
            message.roleName,
            message.modelName,
            message.provider,
        ) {
            buildAiMessageDetailTextBridge(
                message = message,
                showModelProvider = showModelProvider,
                showModelName = showModelName,
                showRoleName = showRoleName,
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Response",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.70f),
            )

            if (detailText.isNotEmpty()) {
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        key(message.timestamp, message.content) {
            ChatMarkupRendererBridge(
                content = message.content,
                textColor = textColor,
                backgroundColor = backgroundColor,
                showThinkingProcess = displayPreferences.showThinkingProcess,
                showStatusTags = displayPreferences.showStatusTags,
                toolCollapseMode = displayPreferences.toolCollapseMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )
        }
    }
}

private fun buildAiMessageDetailTextBridge(
    message: ChatActionSurfaceMessage,
    showModelProvider: Boolean,
    showModelName: Boolean,
    showRoleName: Boolean,
): String {
    val sourceDetailText =
        buildString {
            if (showRoleName && message.roleName.isNotEmpty()) {
                append(message.roleName)
            }

            val showModel = showModelName && message.modelName.isNotEmpty()
            val showProvider = showModelProvider && message.provider.isNotEmpty()

            when {
                showModel && showProvider -> {
                    if (isNotEmpty()) append(" | ")
                    append("${message.modelName} by ${message.provider}")
                }

                showModel -> {
                    if (isNotEmpty()) append(" | ")
                    append(message.modelName)
                }

                showProvider -> {
                    if (isNotEmpty()) append(" | ")
                    append(message.provider)
                }
            }
        }

    return sourceDetailText
}
