package com.mileszhang.spindle.ui.album

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import kotlinx.coroutines.isActive

/**
 * A record with the album cover as its label. It spins while this album is
 * playing and stops where it is when paused, like a real turntable. The course
 * used an infinite transition that snapped back to 0 degrees on pause.
 */
@Composable
fun Vinyl(coverUrl: String, spinning: Boolean, size: Dp, contentDescription: String, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (isActive) {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(rotation.value + 360f, tween(durationMillis = 8_000, easing = LinearEasing))
            }
        }
    }
    Box(modifier.size(size).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = this.size.minDimension / 2
                drawCircle(Color(0xFF0B0B0B), radius = r)
                // Grooves.
                var g = r * 0.97f
                while (g > r * 0.42f) {
                    drawCircle(Color(0xFF242424), radius = g, style = Stroke(width = 1.2f))
                    g -= r * 0.035f
                }
                drawCircle(Color(0x22FFFFFF), radius = r * 0.97f, style = Stroke(width = 2f))
            }
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(0.44f).aspectRatio(1f).clip(CircleShape),
            )
            // Spindle hole.
            Canvas(Modifier.size(size * 0.04f)) { drawCircle(Color(0xFF0B0B0B)) }
        }
    }
}
