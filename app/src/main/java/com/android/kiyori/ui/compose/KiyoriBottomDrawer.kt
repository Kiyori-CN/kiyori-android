package com.android.kiyori.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.interaction.MutableInteractionSource

private enum class KiyoriDrawerState {
    Partial,
    Expanded,
    Hidden
}

private fun resolveDrawerOffsetForState(
    state: KiyoriDrawerState,
    drawerHeightPx: Float,
    partialOffsetPx: Float
): Float {
    return when (state) {
        KiyoriDrawerState.Expanded -> 0f
        KiyoriDrawerState.Partial -> partialOffsetPx
        KiyoriDrawerState.Hidden -> drawerHeightPx
    }
}

@Composable
fun KiyoriBottomDrawer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    partialHeightFraction: Float = 0.4f,
    shape: RoundedCornerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    containerColor: Color = Color.White,
    scrimColor: Color = Color(0x73000000),
    content: @Composable ColumnScope.() -> Unit
) {
    val safePartialFraction = partialHeightFraction.coerceIn(0.2f, 0.95f)
    val coroutineScope = rememberCoroutineScope()
    var drawerState by rememberSaveable { mutableStateOf(KiyoriDrawerState.Partial) }
    var partialOffsetPx by remember { mutableFloatStateOf(Float.NaN) }
    var drawerHeightPx by remember { mutableFloatStateOf(Float.NaN) }
    var dragStartOffsetPx by remember { mutableFloatStateOf(Float.NaN) }
    var dragDeltaPx by remember { mutableFloatStateOf(0f) }
    val offsetPx = remember { Animatable(Float.NaN) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val resolvedDrawerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val resolvedPartialOffsetPx = resolvedDrawerHeightPx * (1f - safePartialFraction)
        val currentOffsetPx = offsetPx.value.let { if (it.isFinite()) it else resolvedPartialOffsetPx }
        val visibleFraction = 1f - (currentOffsetPx / resolvedDrawerHeightPx).coerceIn(0f, 1f)

        LaunchedEffect(resolvedDrawerHeightPx, resolvedPartialOffsetPx) {
            drawerHeightPx = resolvedDrawerHeightPx
            partialOffsetPx = resolvedPartialOffsetPx
            val targetOffset = resolveDrawerOffsetForState(
                state = drawerState,
                drawerHeightPx = resolvedDrawerHeightPx,
                partialOffsetPx = resolvedPartialOffsetPx
            )
            if (!offsetPx.value.isFinite()) {
                offsetPx.snapTo(targetOffset)
            } else if (
                kotlin.math.abs(offsetPx.targetValue - targetOffset) > 0.5f ||
                kotlin.math.abs(offsetPx.value - targetOffset) > 0.5f
            ) {
                offsetPx.animateTo(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        LaunchedEffect(drawerState) {
            if (!drawerHeightPx.isFinite() || !partialOffsetPx.isFinite()) {
                return@LaunchedEffect
            }
            val targetOffset = resolveDrawerOffsetForState(
                state = drawerState,
                drawerHeightPx = drawerHeightPx,
                partialOffsetPx = partialOffsetPx
            )
            if (kotlin.math.abs(offsetPx.value - targetOffset) > 0.5f) {
                offsetPx.animateTo(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            if (drawerState == KiyoriDrawerState.Hidden) {
                onDismissRequest()
            }
        }

        BackHandler {
            drawerState = KiyoriDrawerState.Hidden
        }

        val draggableState = rememberDraggableState { delta ->
            dragDeltaPx += delta
            coroutineScope.launch {
                if (offsetPx.isRunning) {
                    offsetPx.stop()
                }
                val safeHeight = if (drawerHeightPx.isFinite()) drawerHeightPx else resolvedDrawerHeightPx
                val nextOffset = (offsetPx.value + delta).coerceIn(0f, safeHeight)
                offsetPx.snapTo(nextOffset)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = scrimColor.alpha * visibleFraction))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    drawerState = KiyoriDrawerState.Hidden
                }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .offset { IntOffset(0, currentOffsetPx.roundToInt()) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        dragStartOffsetPx = offsetPx.value
                        dragDeltaPx = 0f
                        coroutineScope.launch {
                            if (offsetPx.isRunning) {
                                offsetPx.stop()
                            }
                        }
                    },
                    onDragStopped = { velocity ->
                        val safeStartOffset = dragStartOffsetPx.takeIf { it.isFinite() } ?: currentOffsetPx
                        val velocityThreshold = 900f
                        val distanceThreshold = resolvedDrawerHeightPx * 0.08f
                        val movedEnough = kotlin.math.abs(dragDeltaPx) >= distanceThreshold
                        val flungEnough = kotlin.math.abs(velocity) >= velocityThreshold
                        val movingUp = dragDeltaPx < 0f || velocity < -velocityThreshold
                        val movingDown = dragDeltaPx > 0f || velocity > velocityThreshold

                        drawerState = when {
                            !movedEnough && !flungEnough -> {
                                when (safeStartOffset.roundToInt()) {
                                    resolveDrawerOffsetForState(
                                        state = KiyoriDrawerState.Expanded,
                                        drawerHeightPx = resolvedDrawerHeightPx,
                                        partialOffsetPx = resolvedPartialOffsetPx
                                    ).roundToInt() -> KiyoriDrawerState.Expanded

                                    resolveDrawerOffsetForState(
                                        state = KiyoriDrawerState.Hidden,
                                        drawerHeightPx = resolvedDrawerHeightPx,
                                        partialOffsetPx = resolvedPartialOffsetPx
                                    ).roundToInt() -> KiyoriDrawerState.Hidden

                                    else -> KiyoriDrawerState.Partial
                                }
                            }

                            movingUp && safeStartOffset <= resolvedPartialOffsetPx * 0.5f -> KiyoriDrawerState.Expanded
                            movingUp -> KiyoriDrawerState.Expanded
                            movingDown -> KiyoriDrawerState.Hidden
                            else -> KiyoriDrawerState.Partial
                        }
                        dragStartOffsetPx = Float.NaN
                        dragDeltaPx = 0f
                    }
                ),
            shape = shape,
            color = containerColor,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .background(Color(0xFFD6D6D6), RoundedCornerShape(999.dp))
                    )
                }

                content()
            }
        }
    }
}
