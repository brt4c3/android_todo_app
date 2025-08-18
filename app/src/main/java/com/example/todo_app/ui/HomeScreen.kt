package com.example.todo_app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Home screen:
 * - Full-screen BackgroundAnimation
 * - User draws a circle; if detected -> onEnter()
 * - Lightweight detection & drawing for wide device support
 */
@Composable
fun HomeScreen(
    bg: BackgroundThemeSelection,
    modifier: Modifier = Modifier,
    onEnter: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Accumulated stroke points (decimated for perf)
    var points by remember { mutableStateOf(emptyList<Offset>()) }

    // Visual pulse on the hint text
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Min distance between recorded points (in px)
    val minPointGapPx = with(density) { 6.dp.toPx() }

    // Capture theme colors in a composable context (not inside Canvas)
    val scheme = MaterialTheme.colorScheme
    val onBackgroundColor = scheme.onBackground
    val surfaceColor = scheme.surface
    val onSurfaceVariantColor = scheme.onSurfaceVariant

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background
        BackgroundAnimation(
            modifier = Modifier.fillMaxSize(),
            quality = bg.quality,
            threshold = bg.threshold,
            radiantUnit = bg.radiantUnit,
            colorUnit = bg.colorUnit,
            opacity = bg.opacity,
            colorTheme = bg.theme,
            customPalette = if (bg.theme == PaletteTheme.CUSTOM) bg.customPalette else null
        )


        // Gesture capture + ink overlay
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            points = listOf(start)
                        },
                        onDrag = { change, _ ->
                            val p = change.position
                            val last = points.lastOrNull()
                            if (last == null || distance(last, p) >= minPointGapPx) {
                                points = points + p
                            }
                        },
                        onDragEnd = {
                            // Evaluate the drawn path
                            val ok = detectCircle(points)
                            if (ok) {
                                // Use an available haptic type
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            } else {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            // Clear stroke regardless
                            points = emptyList()
                        },
                        onDragCancel = {
                            points = emptyList()
                        }
                    )
                }
        ) {
            // Ink stroke preview (cheap Path)
            Canvas(Modifier.fillMaxSize()) {
                val pts = points
                if (pts.size >= 2) {
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            lineTo(pts[i].x, pts[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = onBackgroundColor.copy(alpha = 0.55f),
                        style = Stroke(
                            width = 8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Hint UI
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = surfaceColor.copy(alpha = 0.85f)
                ) {
                //    Column(
                //        Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                //        horizontalAlignment = Alignment.CenterHorizontally
                //    ) {
                //        Text(
                //            "Welcome",
                //            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                //        )
                //        Spacer(Modifier.height(6.dp))
                //        Text(
                //            "Draw a circle anywhere to enter",
                //            style = MaterialTheme.typography.bodyMedium,
                //            color = onSurfaceVariantColor
                //        )
                //    }
                }
                //Spacer(Modifier.height(24.dp))
                // A pulsing ring hint
                Canvas(Modifier.size((160 * pulseScale).dp)) {
                    drawCircle(
                        color = onBackgroundColor.copy(alpha = 0.25f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}

/* -------------------- Gesture / Shape detection -------------------- */

private fun detectCircle(points: List<Offset>): Boolean {
    if (points.size < 24) return false

    // Closedness
    val start = points.first()
    val end = points.last()
    val bbox = boundingBox(points)
    val diag = hypot(bbox.width, bbox.height).coerceAtLeast(1f)
    if (distance(start, end) > diag * 0.22f) return false

    // Bounding box near-square
    val ratio = if (bbox.width > bbox.height) bbox.width / bbox.height else bbox.height / bbox.width
    if (!ratio.isFinite() || ratio > 1.5f) return false

    // Centroid
    val cx = points.sumOf { it.x.toDouble() }.toFloat() / points.size
    val cy = points.sumOf { it.y.toDouble() }.toFloat() / points.size

    // Radii statistics
    var sum = 0.0
    var sumSq = 0.0
    for (p in points) {
        val r = distance(Offset(cx, cy), p).toDouble()
        sum += r
        sumSq += r * r
    }
    val n = points.size.toDouble()
    val mean = sum / n
    val variance = (sumSq / n) - (mean * mean)
    val std = kotlin.math.sqrt(max(0.0, variance))
    val relStd = if (mean > 1e-3) std / mean else 1.0
    if (relStd > 0.30) return false // too wobbly

    // Optional: polyline length vs circumference (loose tolerance)
    val length = polylineLength(points)
    val circumference = (2.0 * Math.PI * mean)
    val ratioLen = length / circumference
    if (ratioLen < 0.55 || ratioLen > 1.45) return false

    return true
}

private data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = max(0f, right - left)
    val height: Float get() = max(0f, bottom - top)
}

private fun boundingBox(points: List<Offset>): Box {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (p in points) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    return Box(minX, minY, maxX, maxY)
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private fun polylineLength(points: List<Offset>): Double {
    var len = 0.0
    for (i in 1 until points.size) {
        len += distance(points[i - 1], points[i]).toDouble()
    }
    return len
}
