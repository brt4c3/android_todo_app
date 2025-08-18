package com.example.todo_app.ui

import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Perlin-grid background (single octave, no permutation table)
 * Keeps your math EXACTLY:
 *   depth = perlin3Lfsr(i*refinement, j*refinement, t)
 *   radiant = (depth - threshold) * radiant_unit
 *   colorFactor = (depth - threshold) * color_unit
 *
 * Themes: AQI, BIO_NEON, TERMINAL_LOG, CUSTOM — all smoothly interpolated.
 */
@Composable
fun BackgroundAnimation(
    modifier: Modifier = Modifier,
    opacity: Float = 1f,
    grainAlpha: Float = 0f,              // kept for API compat (unused)
    enableAgsl: Boolean = false,         // kept for API compat (unused)

    // Real-world knobs
    quality: Quality = Quality.AUTO,
    threshold: Float = 0.0f,
    radiantUnit: Dp = 18.dp,
    colorUnit: Float = 0.9f,

    // Fallback tri-mix colors (only used if CUSTOM with no palette)
    baseA: Color? = null,
    baseB: Color? = null,
    accent: Color? = null,

    // LFSR parameters
    lfsrSeed: Int = 1337,
    lfsrTapMask: Int = 0x71,

    // THEME controls
    colorTheme: PaletteTheme = PaletteTheme.AQI,
    customPalette: List<Color>? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Time accumulator in seconds
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(inForeground, baseSpeed) {
        var lastNanos = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (lastNanos != 0L) {
                val dt = ((now - lastNanos).coerceAtMost(50_000_000L)).toFloat() / 1_000_000_000f
                if (inForeground) timeSec += dt * baseSpeed
            }
            lastNanos = now
        }
    }

    // Fallback tri-mix (only used for CUSTOM with no palette provided)
    val fallbackTri = listOf(
        baseA ?: Color(0xFF5A5DF0), // indigo
        baseB ?: Color(0xFFEC6EA7), // pink
        accent ?: Color(0xFF4ED0C9) // teal
    )

    val radiusUnitPx = with(LocalDensity.current) { radiantUnit.toPx() }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cell = max(8f, profile.autoCellSizePx(w, h))
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1

        // Your global time (no jitter/warp/fBm)
        val t = (timeSec * 10f) % 1_000_000f

        for (j in 0 until rows) {
            for (i in 0 until cols) {
                val cx = i * cell + cell * 0.5f
                val cy = j * cell + cell * 0.5f

                // ---- YOUR MATH (unchanged) ----
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
                // --------------------------------

                val colorT = (colorFactor / 1.2f).coerceIn(0f, 1f)
                val col = themedColorSmooth(
                    t = colorT,
                    theme = colorTheme,
                    custom = customPalette,
                    fallbackTri = fallbackTri
                )

                val alpha = (opacity * min(1f, 0.15f + colorFactor.coerceAtMost(1f) * 0.85f))
                    .coerceIn(0f, 1f)

                drawCircle(color = col.copy(alpha = alpha), radius = radius, center = Offset(cx, cy))
            }
        }

        // Optional subtle contours
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
            timeSpeed   = 0.05f,
            contours    = false
        )
        this == MEDIUM -> Profile(
            refinement = 0.036f,
            cellTargetPx = 28f,
            timeSpeed   = 0.05f,
            contours    = false
        )
        this == HIGH -> Profile(
            refinement = 0.042f,
            cellTargetPx = 22f,
            timeSpeed   = 0.02f,
            contours    = true
        )
        else -> Profile( // AUTO
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
        val targetCells = 1050f
        val approxCell = sqrt(area / targetCells)
        return approxCell.coerceIn(18f, 42f)
    }
}

/* --------------------- LFSR-based Perlin (no perm table) --------------------- */

/** Galois 8-bit LFSR: emits one 8-bit value by clocking 8 times. */
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

/* -------- Strong integer hashing (non-linear avalanche) feeding LFSR -------- */

private fun avalanche32(x0: Int): Int {
    var x = x0
    x = x xor (x ushr 16); x *= 0x7feb352d
    x = x xor (x ushr 15); x *= 0x846ca68b.toInt()
    x = x xor (x ushr 16)
    return x
}

private fun hash32(xi: Int, yi: Int, zi: Int, seed: Int): Int {
    var h = xi * 0x27d4eb2d
    h = h xor (yi * 0x165667b1)
    h = h xor (zi * 0x9e3779b1.toInt())
    h = h xor seed
    return avalanche32(h)
}

private fun hash8Lfsr(xi: Int, yi: Int, zi: Int, baseSeed: Int, tapMask: Int): Int {
    val s = (hash32(xi, yi, zi, baseSeed) ushr 8) and 0xFF
    val seed8 = if (s != 0) s else 1
    return lfsr8Byte(seed8, tapMask)
}

/**
 * Single-octave Perlin with LFSR-hashed gradients. Returns ~[-1,1].
 * (Interface unchanged; improved internal hashing only.)
 */
private fun perlin3Lfsr(
    x: Float,
    y: Float,
    z: Float,
    baseSeed: Int,
    tapMask: Int
): Float {
    val X = fastFloor(x)
    val Y = fastFloor(y)
    val Z = fastFloor(z)

    val xf = x - floor(x)
    val yf = y - floor(y)
    val zf = z - floor(z)

    val u = fade(xf)
    val v = fade(yf)
    val w = fade(zf)

    val hAA  = hash8Lfsr(X,     Y,     Z,     baseSeed, tapMask)
    val hBA  = hash8Lfsr(X + 1, Y,     Z,     baseSeed, tapMask)
    val hAB  = hash8Lfsr(X,     Y + 1, Z,     baseSeed, tapMask)
    val hBB  = hash8Lfsr(X + 1, Y + 1, Z,     baseSeed, tapMask)

    val hAA1 = hash8Lfsr(X,     Y,     Z + 1, baseSeed, tapMask)
    val hBA1 = hash8Lfsr(X + 1, Y,     Z + 1, baseSeed, tapMask)
    val hAB1 = hash8Lfsr(X,     Y + 1, Z + 1, baseSeed, tapMask)
    val hBB1 = hash8Lfsr(X + 1, Y + 1, Z + 1, baseSeed, tapMask)

    val x1 = lerp(grad(hAA,  xf,     yf,     zf),
        grad(hBA,  xf - 1, yf,     zf), u)
    val x2 = lerp(grad(hAB,  xf,     yf - 1, zf),
        grad(hBB,  xf - 1, yf - 1, zf), u)
    val y1 = lerp(x1, x2, v)

    val x3 = lerp(grad(hAA1, xf,     yf,     zf - 1),
        grad(hBA1, xf - 1, yf,     zf - 1), u)
    val x4 = lerp(grad(hAB1, xf,     yf - 1, zf - 1),
        grad(hBB1, xf - 1, yf - 1, zf - 1), u)
    val y2 = lerp(x3, x4, v)

    return lerp(y1, y2, w)
}

/* --------------------------- Perlin helpers --------------------------- */

private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)
private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
    val h = hash and 15
    val u = if (h < 8) x else y
    val v = if (h < 4) y else if (h == 12 || h == 14) x else z
    val r1 = if ((h and 1) == 0) u else -u
    val r2 = if ((h and 2) == 0) v else -v
    return r1 + r2
}

private fun fastFloor(x: Float): Int = if (x >= 0f) x.toInt() else x.toInt() - 1

/* ------------------------------ Themes (smooth) ------------------------------ */

/** All themes below are sampled as smooth gradients. */
private fun themedColorSmooth(
    t: Float,
    theme: PaletteTheme,
    custom: List<Color>?,
    fallbackTri: List<Color>
): Color = when (theme) {
    PaletteTheme.CUSTOM ->
        paletteSample(custom?.takeIf { it.size >= 2 } ?: fallbackTri, t, smooth = true)
    PaletteTheme.AQI ->
        paletteSample(aqiPalette, t, smooth = true)
    PaletteTheme.BIO_NEON ->
        paletteSample(bioNeonPalette, t, smooth = true)
    PaletteTheme.TERMINAL_LOG ->
        paletteSample(terminalLogPalette, t, smooth = true)
}

/* AQI gradient (Excellent → Dangerous), now smooth across the 6 anchors */
private val aqiPalette = listOf(
    Color(0xFF00E400), // Excellent (Green)
    Color(0xFFFFFF00), // Fair (Yellow)
    Color(0xFFFF7E00), // Poor (Orange)
    Color(0xFFFF0000), // Unhealthy (Red)
    Color(0xFF8F3F97), // Very Unhealthy (Purple)
    Color(0xFF7E0023)  // Dangerous (Maroon)
)

/* Bio-neon gradient: aqua glow < light cyan < hot magenta < golden highlight */
private val bioNeonPalette = listOf(
    Color(0xFF00D4FF), // aqua glow
    Color(0xFF7FE0FF), // light cyan
    Color(0xFFFF00A5), // hot magenta
    Color(0xFFFFD54F)  // golden highlight
)

/* Terminal log severity (TRACE→DEBUG→INFO→WARN→ERROR→FATAL) as a smooth ramp */
private val terminalLogPalette = listOf(
    Color(0xFF808080), // TRACE (gray)
    Color(0xFF00FFFF), // DEBUG (cyan)
    Color(0xFF00FF00), // INFO (green)
    Color(0xFFFFFF00), // WARN (yellow)
    Color(0xFFFF5555), // ERROR (bright red)
    Color(0xFFFF00FF)  // FATAL (magenta)
)

/* --------------------------- Palette sampling --------------------------- */

private fun paletteSample(p: List<Color>, tIn: Float, smooth: Boolean): Color {
    val t = tIn.coerceIn(0f, 1f)
    if (p.size == 1) return p.first()
    val n = p.size - 1
    return if (smooth) {
        val pos = t * n
        val i = pos.toInt().coerceIn(0, n - 1)
        val f = pos - i
        lerpColor(p[i], p[i + 1], f)
    } else {
        val i = (t * p.size).toInt().coerceIn(0, p.size - 1)
        p[i]
    }
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
