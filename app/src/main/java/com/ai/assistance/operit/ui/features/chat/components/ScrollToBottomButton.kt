package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ScrollToBottomButton(
    scrollState: ScrollState,
    coroutineScope: CoroutineScope,
    autoScrollToBottom: Boolean,
    hasHiddenNewerMessages: Boolean = false,
    onAutoScrollToBottomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showScrollButton by remember { mutableStateOf(false) }
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(scrollState) {
        var lastPosition = scrollState.value
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { currentPosition ->
                if (scrollState.isScrollInProgress) {
                    val scrolledUp = currentPosition < lastPosition
                    if (scrolledUp) {
                        if (autoScrollToBottom && isDragged) {
                            onAutoScrollToBottomChange(false)
                            showScrollButton = true
                        }
                    } else {
                        val isAtBottom =
                            scrollState.value >= scrollState.maxValue &&
                                !hasHiddenNewerMessages
                        if (isAtBottom && !autoScrollToBottom) {
                            onAutoScrollToBottomChange(true)
                            showScrollButton = false
                        }
                    }
                }
                lastPosition = currentPosition
            }
    }

    ScrollToBottomButtonContent(
        visible = showScrollButton,
        modifier = modifier,
        onClick = {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            onAutoScrollToBottomChange(true)
            showScrollButton = false
        },
    )
}

@Composable
private fun ScrollToBottomButtonContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(50),
                ),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
