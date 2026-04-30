package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.assistance.operit.ui.features.chat.components.style.cursor.ChatMessageCursorStyleBridge
import com.android.kiyori.R

@Composable
fun ChatAreaMessageItemBridge(
    index: Int,
    message: ChatActionSurfaceMessage,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    displayPreferences: ChatAreaDisplayPreferencesBridge,
    callbacks: ChatActionSurfaceCallbacks,
    modifier: Modifier = Modifier,
) {
    when (message.sender) {
        ChatActionSurfaceSender.SYSTEM ->
            ChatAreaSystemMessageBridge(
                message = message,
                modifier = modifier,
            )

        ChatActionSurfaceSender.USER,
        ChatActionSurfaceSender.AI -> {
            val visibleMessage = message.withHiddenPlaceholderContent()
            ChatMessageActionSurfaceBridge(
                index = index,
                message = message,
                isMultiSelectMode = isMultiSelectMode,
                isSelected = isSelected,
                showMessageTokenStats = displayPreferences.showMessageTokenStats,
                showMessageTimingStats = displayPreferences.showMessageTimingStats,
                callbacks = callbacks,
                modifier = modifier,
            ) {
                ChatMessageCursorStyleBridge(
                    message = visibleMessage,
                    displayPreferences = displayPreferences,
                )
            }
        }
    }
}

@Composable
private fun ChatAreaSystemMessageBridge(
    message: ChatActionSurfaceMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF4F6FA)) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message.content,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF667085),
                    textAlign = TextAlign.Center,
                )
                if (!message.meta.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.meta.orEmpty(),
                        fontSize = 10.sp,
                        color = Color(0xFF98A2B3),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatActionSurfaceMessage.withHiddenPlaceholderContent(): ChatActionSurfaceMessage =
    if (sender == ChatActionSurfaceSender.USER && displayMode == ChatActionSurfaceDisplayMode.HIDDEN_PLACEHOLDER) {
        copy(content = stringResource(R.string.chat_hidden_user_message_placeholder))
    } else {
        this
    }
