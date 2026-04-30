package com.ai.assistance.operit.ui.features.chat.components.style.cursor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceSender
import com.ai.assistance.operit.ui.features.chat.components.part.ChatMarkupRendererBridge

@Composable
fun ChatMessageCursorStyleBridge(
    message: ChatActionSurfaceMessage,
    modifier: Modifier = Modifier,
    userMessageColor: Color = MaterialTheme.colorScheme.primaryContainer,
    aiMessageColor: Color = Color.Transparent,
    userTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    aiTextColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    when (message.sender) {
        ChatActionSurfaceSender.USER ->
            CursorUserMessageBridge(
                message = message,
                backgroundColor = userMessageColor,
                textColor = userTextColor,
                modifier = modifier,
            )

        ChatActionSurfaceSender.AI ->
            CursorAiMessageBridge(
                message = message,
                backgroundColor = aiMessageColor,
                textColor = aiTextColor,
                modifier = modifier,
            )

        ChatActionSurfaceSender.SYSTEM -> Unit
    }
}

@Composable
private fun CursorUserMessageBridge(
    message: ChatActionSurfaceMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = "Prompt",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.70f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ChatMarkupRendererBridge(
                    content = message.content,
                    textColor = textColor,
                    renderInlineTextOnly = true,
                )
            }
        }
    }
}

@Composable
private fun CursorAiMessageBridge(
    message: ChatActionSurfaceMessage,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
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
                fontWeight = FontWeight.Medium,
            )
            if (!message.meta.isNullOrBlank()) {
                Text(
                    text = message.meta.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.50f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            ChatMarkupRendererBridge(
                content = message.content,
                textColor = textColor,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
