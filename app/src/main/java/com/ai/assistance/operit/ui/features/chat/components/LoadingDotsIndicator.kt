package com.ai.assistance.operit.ui.features.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDotsIndicator(textColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_loading_dots")

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val jumpHeight = -5f
        val animationDelay = 160

        (0..2).forEach { index ->
            val offsetY by
                infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = jumpHeight,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                keyframes {
                                    durationMillis = 600
                                    0f at 0
                                    jumpHeight * 0.4f at 100
                                    jumpHeight * 0.8f at 200
                                    jumpHeight at 300
                                    jumpHeight * 0.8f at 400
                                    jumpHeight * 0.4f at 500
                                    0f at 600
                                },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(index * animationDelay),
                        ),
                    label = "chat_loading_dot_$index",
                )

            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .offset(y = offsetY.dp)
                        .background(
                            color = textColor.copy(alpha = 0.6f),
                            shape = CircleShape,
                        ),
            )
        }
    }
}
