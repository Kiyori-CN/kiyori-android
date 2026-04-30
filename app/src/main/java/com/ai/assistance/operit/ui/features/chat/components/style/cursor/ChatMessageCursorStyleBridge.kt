package com.ai.assistance.operit.ui.features.chat.components.style.cursor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ai.assistance.operit.ui.features.chat.components.ChatAreaDisplayPreferencesBridge
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceMessage
import com.ai.assistance.operit.ui.features.chat.components.ChatActionSurfaceSender

@Composable
fun ChatMessageCursorStyleBridge(
    message: ChatActionSurfaceMessage,
    modifier: Modifier = Modifier,
    userMessageColor: Color = MaterialTheme.colorScheme.primaryContainer,
    aiMessageColor: Color = Color.Transparent,
    userTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    aiTextColor: Color = MaterialTheme.colorScheme.onSurface,
    displayPreferences: ChatAreaDisplayPreferencesBridge = ChatAreaDisplayPreferencesBridge(),
) {
    when (message.sender) {
        ChatActionSurfaceSender.USER ->
            UserMessageComposableBridge(
                message = message,
                backgroundColor = userMessageColor,
                textColor = userTextColor,
                modifier = modifier,
            )

        ChatActionSurfaceSender.AI ->
            AiMessageComposableBridge(
                message = message,
                backgroundColor = aiMessageColor,
                textColor = aiTextColor,
                modifier = modifier,
                showModelProvider = displayPreferences.showModelProvider,
                showModelName = displayPreferences.showModelName,
                showRoleName = displayPreferences.showRoleName,
                displayPreferences = displayPreferences,
            )

        ChatActionSurfaceSender.SYSTEM -> Unit
    }
}
