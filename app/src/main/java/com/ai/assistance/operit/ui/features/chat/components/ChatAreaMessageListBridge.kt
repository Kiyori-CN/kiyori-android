package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.kiyori.R

@Composable
fun ChatAreaMessageListBridge(
    firstMessageIndex: Int,
    visibleMessages: List<ChatActionSurfaceMessage>,
    scrollState: androidx.compose.foundation.ScrollState,
    hasOlderPages: Boolean,
    hasNewerPages: Boolean,
    showLoadingIndicator: Boolean,
    showChatFloatingDotsAnimation: Boolean,
    chatStyle: ChatAreaStyleBridge,
    loadingIndicatorTextColor: Color,
    topContentPadding: Dp,
    horizontalPadding: Dp = 16.dp,
    bottomSpacerHeight: Dp,
    isMultiSelectMode: Boolean,
    selectedMessageIndices: Set<Int>,
    displayPreferences: ChatAreaDisplayPreferencesBridge,
    callbacks: ChatActionSurfaceCallbacks,
    onLoadOlderPages: () -> Unit,
    onLoadNewerPages: () -> Unit,
    onMessagePositioned: (index: Int, absoluteTopPx: Float, heightPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = topContentPadding,
                )
                .verticalScroll(scrollState),
    ) {
        if (hasOlderPages) {
            ChatAreaPaginationRowBridge(
                text = stringResource(R.string.load_more_history),
                onClick = onLoadOlderPages,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        visibleMessages.forEachIndexed { relativeIndex, message ->
            val actualIndex = firstMessageIndex + relativeIndex
            key(actualIndex, message.sender, message.content, message.meta) {
                Box(
                    modifier =
                        Modifier.onGloballyPositioned { coordinates ->
                            onMessagePositioned(
                                actualIndex,
                                coordinates.positionInParent().y,
                                coordinates.size.height,
                            )
                        },
                ) {
                    ChatAreaMessageItemBridge(
                        index = actualIndex,
                        message = message,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = selectedMessageIndices.contains(actualIndex),
                        displayPreferences = displayPreferences,
                        callbacks = callbacks,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (hasNewerPages) {
            ChatAreaPaginationRowBridge(
                text = stringResource(R.string.load_newer_history),
                onClick = onLoadNewerPages,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (showLoadingIndicator) {
            ChatAreaLoadingIndicatorBridge(
                chatStyle = chatStyle,
                showChatFloatingDotsAnimation = showChatFloatingDotsAnimation,
                textColor = loadingIndicatorTextColor,
            )
        }

        Spacer(modifier = Modifier.height(bottomSpacerHeight))
    }
}

@Composable
private fun ChatAreaPaginationRowBridge(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center,
    )
}
