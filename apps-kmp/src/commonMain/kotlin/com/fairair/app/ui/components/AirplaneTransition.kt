package com.fairair.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fairair.app.ui.theme.VelocityColors
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Airplane transition that reveals the next screen from left to right.
 * The airplane flies across with contrails, and behind it the new screen is revealed.
 */
@Composable
fun AirplaneTransition(
    isAnimating: Boolean,
    onAnimationComplete: () -> Unit,
    previousScreen: @Composable () -> Unit,
    nextScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    var showAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            println("[AirplaneTransition] Starting animation")
            showAnimation = true
            animationProgress.snapTo(0f)
            delay(50)
            
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            )
            
            println("[AirplaneTransition] Animation complete")
            showAnimation = false
            onAnimationComplete()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Next screen (the destination) - always rendered underneath
        nextScreen()
        
        if (showAnimation) {
            val progress = animationProgress.value
            
            // Layer 2: Canvas overlay that covers unrevealed portion and draws airplane
            Canvas(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                val width = size.width
                val height = size.height
                
                // The reveal line moves from left to right
                val revealX = width * progress
                
                // Draw the "curtain" covering the unrevealed part (right side)
                // This covers the next screen on the right side
                val curtainGradientWidth = 60.dp.toPx()
                
                // Gradient edge (soft transition)
                if (revealX < width) {
                    // Soft gradient edge
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                VelocityColors.GradientStart
                            ),
                            startX = revealX,
                            endX = revealX + curtainGradientWidth
                        ),
                        topLeft = Offset(revealX, 0f),
                        size = Size(curtainGradientWidth.coerceAtMost(width - revealX), height)
                    )
                    
                    // Solid curtain covering the rest
                    val solidStartX = revealX + curtainGradientWidth
                    if (solidStartX < width) {
                        drawRect(
                            color = VelocityColors.GradientStart,
                            topLeft = Offset(solidStartX, 0f),
                            size = Size(width - solidStartX, height)
                        )
                    }
                }
                
                // Airplane position - at the reveal edge with slight wave motion
                val airplaneX = revealX + 30.dp.toPx()
                val waveAmplitude = 25.dp.toPx()
                val airplaneY = height * 0.45f + (sin(progress * kotlin.math.PI * 2) * waveAmplitude).toFloat()
                
                // Draw contrails (vapor trails) behind airplane
                val trailLength = 200.dp.toPx()
                val trailStartX = (airplaneX - trailLength).coerceAtLeast(0f)
                
                // Multiple contrail lines
                for (i in -1..1) {
                    val yOffset = i * 10.dp.toPx()
                    val alpha = 0.6f - kotlin.math.abs(i) * 0.2f
                    
                    // Create curved trail path following the wave
                    val trailPath = Path().apply {
                        val steps = 15
                        var started = false
                        for (step in 0..steps) {
                            val t = step.toFloat() / steps
                            val x = trailStartX + (airplaneX - 25.dp.toPx() - trailStartX) * t
                            // Calculate Y based on where the plane was at that point
                            val pastProgress = (progress - (1 - t) * 0.15f).coerceIn(0f, 1f)
                            val y = height * 0.45f + (sin(pastProgress * kotlin.math.PI * 2) * waveAmplitude).toFloat() + yOffset
                            
                            if (!started) {
                                moveTo(x, y)
                                started = true
                            } else {
                                lineTo(x, y)
                            }
                        }
                    }
                    
                    drawPath(
                        path = trailPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = alpha * 0.5f),
                                Color.White.copy(alpha = alpha)
                            ),
                            startX = trailStartX,
                            endX = airplaneX - 25.dp.toPx()
                        ),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                
                // Draw the airplane
                val planeSize = 50.dp.toPx()
                translate(left = airplaneX - planeSize / 2, top = airplaneY - planeSize / 2) {
                    rotate(degrees = -10f, pivot = Offset(planeSize / 2, planeSize / 2)) {
                        // Airplane body
                        val bodyPath = Path().apply {
                            moveTo(planeSize * 0.95f, planeSize * 0.5f)  // Nose
                            quadraticTo(planeSize * 0.8f, planeSize * 0.4f, planeSize * 0.2f, planeSize * 0.45f)
                            lineTo(planeSize * 0.05f, planeSize * 0.5f)
                            lineTo(planeSize * 0.2f, planeSize * 0.55f)
                            quadraticTo(planeSize * 0.8f, planeSize * 0.6f, planeSize * 0.95f, planeSize * 0.5f)
                            close()
                        }
                        drawPath(bodyPath, Color.White)
                        
                        // Wings
                        val wingPath = Path().apply {
                            moveTo(planeSize * 0.55f, planeSize * 0.48f)
                            lineTo(planeSize * 0.4f, planeSize * 0.15f)
                            lineTo(planeSize * 0.3f, planeSize * 0.2f)
                            lineTo(planeSize * 0.45f, planeSize * 0.46f)
                            close()
                            
                            moveTo(planeSize * 0.55f, planeSize * 0.52f)
                            lineTo(planeSize * 0.4f, planeSize * 0.85f)
                            lineTo(planeSize * 0.3f, planeSize * 0.8f)
                            lineTo(planeSize * 0.45f, planeSize * 0.54f)
                            close()
                        }
                        drawPath(wingPath, Color.White)
                        
                        // Tail fin
                        val tailPath = Path().apply {
                            moveTo(planeSize * 0.12f, planeSize * 0.45f)
                            lineTo(planeSize * 0.05f, planeSize * 0.2f)
                            lineTo(planeSize * 0.18f, planeSize * 0.35f)
                            lineTo(planeSize * 0.18f, planeSize * 0.45f)
                            close()
                        }
                        drawPath(tailPath, Color.White)
                        
                        // Horizontal stabilizers
                        val stabPath = Path().apply {
                            moveTo(planeSize * 0.1f, planeSize * 0.48f)
                            lineTo(planeSize * 0.02f, planeSize * 0.38f)
                            lineTo(planeSize * 0.08f, planeSize * 0.44f)
                            close()
                            
                            moveTo(planeSize * 0.1f, planeSize * 0.52f)
                            lineTo(planeSize * 0.02f, planeSize * 0.62f)
                            lineTo(planeSize * 0.08f, planeSize * 0.56f)
                            close()
                        }
                        drawPath(stabPath, Color.White)
                    }
                }
            }
        }
    }
}
