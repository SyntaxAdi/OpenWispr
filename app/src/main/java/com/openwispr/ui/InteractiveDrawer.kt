package com.openwispr.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Hardware-accelerated full-screen swipeable side navigation drawer.
 * Tracks horizontal swipes anywhere on screen with 1:1 RenderThread translation
 * and velocity-matched physics spring animation.
 */
@Composable
fun InteractiveDrawer(
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    drawerWidth: Dp = 280.dp,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val animOffset = remember { Animatable(if (isOpen) drawerWidthPx else 0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    // Sync external open/close triggers (e.g. top hamburger button click)
    LaunchedEffect(isOpen, drawerWidthPx) {
        val target = if (isOpen) drawerWidthPx else 0f
        if (animOffset.value != target) {
            animOffset.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(drawerWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPointerInputChange(change)
                        scope.launch {
                            val newOffset = (animOffset.value + dragAmount).coerceIn(0f, drawerWidthPx)
                            animOffset.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        scope.launch {
                            if (velocity > 750f) {
                                onOpen()
                                animOffset.animateTo(
                                    targetValue = drawerWidthPx,
                                    initialVelocity = velocity,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            } else if (velocity < -750f) {
                                onClose()
                                animOffset.animateTo(
                                    targetValue = 0f,
                                    initialVelocity = velocity,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            } else {
                                if (animOffset.value >= drawerWidthPx * 0.35f) {
                                    onOpen()
                                    animOffset.animateTo(
                                        targetValue = drawerWidthPx,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                } else {
                                    onClose()
                                    animOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            val target = if (isOpen) drawerWidthPx else 0f
                            animOffset.animateTo(target)
                        }
                    }
                )
            }
    ) {
        // Main Screen Content
        content()

        // Scrim Dimmer
        val progress = if (drawerWidthPx > 0f) (animOffset.value / drawerWidthPx).coerceIn(0f, 1f) else 0f
        if (progress > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress * 0.65f }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                        scope.launch {
                            animOffset.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
                        }
                    }
            )
        }

        // Drawer Sheet with RenderThread hardware translation
        Box(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = animOffset.value - drawerWidthPx
                }
        ) {
            drawerContent()
        }
    }
}
