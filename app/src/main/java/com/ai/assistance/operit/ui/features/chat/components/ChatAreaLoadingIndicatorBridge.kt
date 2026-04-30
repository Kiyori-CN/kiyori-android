package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ChatAreaStyleBridge {
    CURSOR,
    BUBBLE,
}

@Composable
fun ChatAreaLoadingIndicatorBridge(
    chatStyle: ChatAreaStyleBridge,
    showChatFloatingDotsAnimation: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (!showChatFloatingDotsAnimation) {
        return
    }

    val styleModifier =
        when (chatStyle) {
            ChatAreaStyleBridge.BUBBLE -> Modifier.offset(y = (-24).dp)
            ChatAreaStyleBridge.CURSOR -> Modifier
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .then(styleModifier),
    ) {
        Box(modifier = Modifier.padding(start = 16.dp)) {
            LoadingDotsIndicator(textColor = textColor)
        }
    }
}
