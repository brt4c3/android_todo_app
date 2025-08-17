package com.example.todo_app.ui

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real–world Perlin-grid background (LFSR-hashed gradients, no permutation table)
 * - Adaptive quality (AUTO/LOW/MEDIUM/HIGH)
 * - Lifecycle-aware (pauses in background)
 * - Reduce-motion aware (uses system animator scale)
 * - Theme-aware defaults (override colors if you want)
 *
 * Your math:
 *   depth = perlin3Lfsr(i*refinement, j*refinement, t)
 *   radiant = (depth - threshold) * radiant_unit
 *   colorFactor = (depth - threshold) * color_unit
 */
@Composable
fun BackgroundAnimation(
    modifier: Modifier = Modifier,
    opacity: Float = 1f,                 // final alpha (0..1)
    grainAlpha: Float = 0f,              // kept for API compat (unused here)
    enableAgsl: Boolean = false,         // kept for API compat (unused here)

    // Real-world knobs
    quality: Quality = Quality.AUTO,
    threshold: Float = 0.0f,
    radiantUnit: Dp = 18.dp,
    colorUnit: Float = 0.9f,
    baseA: Color? = null,                // null -> themed defaults
    baseB: Color? = null,
    accent: Color? = null,

    // LFSR parameters (tweak if you like)
    lfsrSeed: Int = 1337,                // mixed into coordinate hash
    lfsrTapMask: Int = 0x71              // 0b0111_0001 same as your Python sample
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Pause when not visible
    var inForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            inForeground = e == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Battery saver
    val power = remember { context.getSystemService(PowerManager::class.java) }
    val powerSave = power?.isPowerSaveMode == true

    // System animator scale (reduce motion)
    val animatorScale: Float = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )
        } catch (_: Throwable) { 1f }
    }.coerceAtLeast(0f)

    // Adaptive quality profile
    val profile = remember(quality, powerSave) { quality.resolve(powerSave) }

    // Animated time (slowed if reduce motion; stopped if bg)
    val baseSpeed = profile.timeSpeed * when {
        animatorScale <= 0.01f -> 0.05f   // very slow
        animatorScale <  0.5f  -> 0.5f    // moderately slow
        else                   -> 1f
    }


// Continuously accumulating time in seconds (no reset)
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(inForeground, baseSpeed) {
        var lastNanos = 0L
        while (true) {
            val now = withFrameNanos { it } // suspend until next frame
            if (lastNanos != 0L) {
                // delta time in seconds, with a safety cap to avoid big jumps
                val dt = ((now - lastNanos).coerceAtMost(50_000_000L)).toFloat() / 1_000_000_000f
                if (inForeground) {
                    timeSec += dt * baseSpeed
                }
            }
            lastNanos = now
        }
    }

    // Theme-ish defaults if colors not provided
    val a = baseA ?: Color(0xFF5A5DF0) // indigo
    val b = baseB ?: Color(0xFFEC6EA7) // pink
    val c = accent ?: Color(0xFF4ED0C9) // teal

    val radiusUnitPx = with(LocalDensity.current) { radiantUnit.toPx() }

    // Precompute nothing large — LFSR hash is stateless per coordinate
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val autoCellSize = profile.autoCellSizePx(w, h)
        val cell = max(8f, autoCellSize)
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1

        // Scale and very-large wrap to avoid float precision drift, but no visible “loop”
        val t = (timeSec * 10f) % 1_000_000f

        for (j in 0 until rows) {
            for (i in 0 until cols) {
                val cx = i * cell + cell * 0.5f
                val cy = j * cell + cell * 0.5f

                // LFSR-hashed Perlin (no permutation table)
                val depth = perlin3Lfsr(
                    x = i * profile.refinement,
                    y = j * profile.refinement,
                    z = t,
                    baseSeed = lfsrSeed,
                    tapMask = lfsrTapMask
                ) // ~[-1,1]

                val delta = depth - threshold
                val radius = (delta * radiusUnitPx).coerceAtLeast(0f)
                if (radius <= 0.6f) continue

                val colorFactor = (delta * colorUnit).coerceIn(0f, 1.2f)
                val col = triMix(a, b, c, colorFactor)
                val alpha = (opacity * min(1f, 0.15f + colorFactor * 0.85f)).coerceIn(0f, 1f)

                drawCircle(color = col.copy(alpha = alpha), radius = radius, center = Offset(cx, cy))
            }
        }

        // Optional: ultra-light contours to “seat” the blobs visually
        if (profile.contours) {
            val step = max(16f, cell * 1.2f)
            val stroke = max(1f, cell * 0.035f)
            val contourColor = Color.Black.copy(alpha = 0.03f * opacity)
            var x = 0f
            while (x < w) {
                drawLine(contourColor, Offset(x, 0f), Offset(x, h), strokeWidth = stroke)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(contourColor, Offset(0f, y), Offset(w, y), strokeWidth = stroke)
                y += step
            }
        }
    }
}

/* --------------------------- Quality profiles --------------------------- */

enum class Quality {
    AUTO, LOW, MEDIUM, HIGH;

    fun resolve(powerSave: Boolean): Profile = when {
        powerSave || this == LOW -> Profile(
            refinement = 0.030f,
            cellTargetPx = 36f,
            timeSpeed   = 0.10f,
            contours    = false
        )
        this == MEDIUM -> Profile(
            refinement = 0.036f,
            cellTargetPx = 28f,
            timeSpeed   = 0.12f,
            contours    = false
        )
        this == HIGH -> Profile(
            refinement = 0.042f,
            cellTargetPx = 22f,
            timeSpeed   = 0.14f,
            contours    = true
        )
        else -> Profile(
            refinement = 0.038f,
            cellTargetPx = 0f,
            timeSpeed   = 0.12f,
            contours    = true
        )
    }
}


data class Profile(
    val refinement: Float,
    val cellTargetPx: Float, // if 0 -> compute from area
    val timeSpeed: Float,
    val contours: Boolean
) {
    fun autoCellSizePx(w: Float, h: Float): Float {
        if (cellTargetPx > 0f) return cellTargetPx
        val area = w * h
        val targetCells = 1050f  // ~900–1200 blobs typical
        val approxCell = sqrt(area / targetCells)
        return approxCell.coerceIn(18f, 42f)
    }
}

/* --------------------- LFSR-based Perlin (no perm table) --------------------- */

/** Galois 8-bit LFSR: emits one 8-bit value by clocking 8 times (your Python lfsr8_galois). */
private fun lfsr8Byte(seed: Int, tapMask: Int = 0x71): Int {
    require(seed in 1..0xFF) { "LFSR seed must be 1..255 (non-zero)" }
    var s = seed and 0xFF
    var b = 0
    repeat(8) {
        val lsb = s and 1
        s = s ushr 1
        if (lsb != 0) s = s xor tapMask
        b = (b shl 1) or lsb
    }
    return b and 0xFF
}

/** Mix integer coords into a non-zero 8-bit seed, then step the LFSR to get a hash byte. */
private fun hash8Lfsr(xi: Int, yi: Int, zi: Int, baseSeed: Int, tapMask: Int): Int {
    // Simple, fast integer mixing (kept mod 256)
    var s = (xi * 73) xor (yi * 199) xor (zi * 229) xor baseSeed
    s = s and 0xFF
    if (s == 0) s = 1
    return lfsr8Byte(s, tapMask)
}

/**
 * Perlin noise with LFSR-hashed gradients. Returns ~[-1,1].
 * Drop-in replacement for perm-table Perlin.
 */
private fun perlin3Lfsr(
    x: Float,
    y: Float,
    z: Float,
    baseSeed: Int,
    tapMask: Int
): Float {
    // Lattice coords
    val X = fastFloor(x)
    val Y = fastFloor(y)
    val Z = fastFloor(z)

    // Relative position inside cube
    val xf = x - floor(x)
    val yf = y - floor(y)
    val zf = z - floor(z)

    // Fade curves
    val u = fade(xf)
    val v = fade(yf)
    val w = fade(zf)

    // 8 corner hashes via LFSR
    val hAA  = hash8Lfsr(X,     Y,     Z,     baseSeed, tapMask)
    val hBA  = hash8Lfsr(X + 1, Y,     Z,     baseSeed, tapMask)
    val hAB  = hash8Lfsr(X,     Y + 1, Z,     baseSeed, tapMask)
    val hBB  = hash8Lfsr(X + 1, Y + 1, Z,     baseSeed, tapMask)

    val hAA1 = hash8Lfsr(X,     Y,     Z + 1, baseSeed, tapMask)
    val hBA1 = hash8Lfsr(X + 1, Y,     Z + 1, baseSeed, tapMask)
    val hAB1 = hash8Lfsr(X,     Y + 1, Z + 1, baseSeed, tapMask)
    val hBB1 = hash8Lfsr(X + 1, Y + 1, Z + 1, baseSeed, tapMask)

    // Dot products at corners
    val x1 = lerp(
        grad(hAA,  xf,     yf,     zf),
        grad(hBA,  xf - 1, yf,     zf), u
    )
    val x2 = lerp(
        grad(hAB,  xf,     yf - 1, zf),
        grad(hBB,  xf - 1, yf - 1, zf), u
    )
    val y1 = lerp(x1, x2, v)

    val x3 = lerp(
        grad(hAA1, xf,     yf,     zf - 1),
        grad(hBA1, xf - 1, yf,     zf - 1), u
    )
    val x4 = lerp(
        grad(hAB1, xf,     yf - 1, zf - 1),
        grad(hBB1, xf - 1, yf - 1, zf - 1), u
    )
    val y2 = lerp(x3, x4, v)

    return lerp(y1, y2, w) // ~[-1,1]
}

/* --------------------------- Perlin helpers --------------------------- */

private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

/** Classic 3D Perlin gradient selector using low 4 bits of the hash. */
private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
    val h = hash and 15
    val u = if (h < 8) x else y
    val v = if (h < 4) y else if (h == 12 || h == 14) x else z
    val r1 = if ((h and 1) == 0) u else -u
    val r2 = if ((h and 2) == 0) v else -v
    return r1 + r2
}

private fun fastFloor(x: Float): Int = if (x >= 0f) x.toInt() else x.toInt() - 1

/* --------------------------- Color helpers --------------------------- */

private fun triMix(a: Color, b: Color, c: Color, tIn: Float): Color {
    val t = tIn.coerceIn(0f, 1f)
    return if (t < 0.66f) lerpColor(a, b, t / 0.66f) else lerpColor(b, c, (t - 0.66f) / 0.34f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red   = a.red   + (b.red   - a.red)   * k,
        green = a.green + (b.green - a.green) * k,
        blue  = a.blue  + (b.blue  - a.blue)  * k,
        alpha = a.alpha + (b.alpha - a.alpha) * k
    )
}
