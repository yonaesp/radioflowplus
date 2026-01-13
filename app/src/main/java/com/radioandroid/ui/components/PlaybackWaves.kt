package com.radioandroid.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * High-performance Animated Playback Waves using Canvas.
 * 
 * Uses a mathematical sine-wave approach driven by a continuous phase:
 * - Playing: Bars oscillate with a spatial phase offset (Wave effect).
 * - Loading: Bars oscillate synchronously (Breathing effect).
 * - Paused: Amplitude collapses to 0 (Flat line).
 * 
 * This ensures smooth transitions and prevents "stuck" animations.
 */
@Composable
fun PlaybackWaves(
    isPlaying: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 20.dp,
    spacing: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    // 1. The Motor: Continuous Phase Animation (0 -> 2PI)
    // Runs constantly to ensure we never "lose" the animation loop.
    val infiniteTransition = rememberInfiniteTransition(label = "wave_motor")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // 2. The Logic: State-driven Properties
    // Amplitude: 0f (Paused) -> 1f (Playing/Loading)
    val targetAmplitude = if (isPlaying || isLoading) 1f else 0f
    val amplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(500, easing = LinearOutSlowInEasing), // Smooth entry/exit
        label = "amplitude"
    )

    // Spatial Frequency: Determines if bars act independently (Wave) or together (Breathing)
    // 1f = Staggered (Wave), 0f = Synced (Breathing)
    val targetSpatialFreq = if (isLoading) 0f else 1f
    val spatialFreq by animateFloatAsState(
        targetValue = targetSpatialFreq,
        animationSpec = tween(500),
        label = "spatial_freq"
    )
    
    // Speed Multiplier: Faster when playing, slower when loading? 
    // For now, constant speed is fine, but we could modulate phase speed if needed.

    // 3. The Renderer: Canvas
    Canvas(modifier = modifier.height(maxHeight).width((barWidth + spacing) * barCount)) {
        val barW = barWidth.toPx()
        val space = spacing.toPx()
        val maxH = size.height
        val minH = maxH * 0.2f // Minimum height when paused (flat line thickness)
        
        val totalWidth = (barCount * barW) + ((barCount - 1) * space)
        val startX = (size.width - totalWidth) / 2f // Center horizontally if needed, though modifier usually handles it
        
        repeat(barCount) { index ->
            // Math: Height = Min + (Range * Amplitude * WaveFunction)
            // WaveFunction = (sin(Phase + IndexOffset) + 1) / 2  <- Maps -1..1 to 0..1
            
            // Offset calculation:
            // If spatialFreq is 1 (Playing): each bar has different phase (Order: 0, 1.2, 2.4...)
            // If spatialFreq is 0 (Loading): all bars have phase 0 (Synced pattern)
            val currentPhaseOffset = index * 1.2f * spatialFreq
            
            val sineValue = (sin(phase + currentPhaseOffset) + 1f) / 2f
            
            // Current dynamic height
            // If amplitude is 0 (Paused), this collapses to minH
            val currentBarHeight = minH + (maxH - minH) * amplitude * sineValue
            
            // Draw
            // Align to BottomCenter
            val x = startX + (index * (barW + space))
            val y = size.height - currentBarHeight
            
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, currentBarHeight),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
