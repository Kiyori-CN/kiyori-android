package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreenContentWorkbenchBridge(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    bottomInset: Dp = 0.dp,
    backgroundColor: Color,
    chatHeaderOverlayMode: Boolean = true,
    headerContent: @Composable () -> Unit,
    content: @Composable (topPadding: Dp, bottomPadding: Dp) -> Unit,
) {
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues),
    ) {
        if (chatHeaderOverlayMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                content(
                    headerHeight,
                    bottomInset,
                )
                Box(
                    modifier =
                        Modifier.onGloballyPositioned { coordinates ->
                            headerHeight = with(density) { coordinates.size.height.toDp() }
                        },
                ) {
                    headerContent()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                headerContent()
                Box(modifier = Modifier.fillMaxSize()) {
                    content(0.dp, bottomInset)
                }
            }
        }
    }
}
