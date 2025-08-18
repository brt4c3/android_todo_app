@file:Suppress("FunctionName", "unused")

package com.example.todo_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/* ───────────────────────────── Public API ───────────────────────────── */

data class BackgroundThemeSelection(
    val theme: PaletteTheme,
    val customPalette: List<Color>,
    val threshold: Float,
    val radiantUnit: Dp,
    val colorUnit: Float,
    val opacity: Float,
    val quality: Quality
)

/**
 * BackgroundThemePickerPage
 *
 * Integrates directly with BackgroundAnimation.kt (same ui package types).
 * - Color theme from list (AQI / Bio-Neon / Terminal Log / Custom)
 * - Custom theme uses horizontal color faders (Hue & Brightness)
 * - Settings (threshold, radiant size, color intensity, opacity) via horizontal sliders
 * - Quality via chips (discrete profiles)
 */
@Composable
fun BackgroundThemePickerPage(
    initial: BackgroundThemeSelection = BackgroundThemeSelection(
        theme = PaletteTheme.BIO_NEON,
        customPalette = listOf(Color(0xFF00D4FF), Color(0xFF7FE0FF), Color(0xFFFF00A5), Color(0xFFFFD54F)),
        threshold = -0.10f,
        radiantUnit = 20.dp,
        colorUnit = 1.0f,
        opacity = 1f,
        quality = Quality.HIGH
    ),
    onApply: (BackgroundThemeSelection) -> Unit = {}
) {
    // Local state (use rememberSaveable/DataStore in your app as needed)
    var theme by remember { mutableStateOf(initial.theme) }
    var customPalette by remember { mutableStateOf(initial.customPalette.ensureAtLeastTwo()) }
    var threshold by remember { mutableStateOf(initial.threshold.coerceIn(-0.3f, 0.3f)) }
    var radiant by remember { mutableStateOf(initial.radiantUnit.coerceIn(8.dp, 36.dp)) }
    var colorUnit by remember { mutableStateOf(initial.colorUnit.coerceIn(0.5f, 1.5f)) }
    var opacity by remember { mutableStateOf(initial.opacity.coerceIn(0.2f, 1f)) }
    var quality by remember { mutableStateOf(initial.quality) }

    // Palette previews (smooth anchors — match BackgroundAnimation’s sets)
    val aqiPalette = remember {
        listOf(
            Color(0xFF00E400), Color(0xFFFFFF00), Color(0xFFFF7E00),
            Color(0xFFFF0000), Color(0xFF8F3F97), Color(0xFF7E0023)
        )
    }
    val bioNeonPalette = remember {
        listOf(
            Color(0xFF00D4FF), Color(0xFF7FE0FF),
            Color(0xFFFF00A5), Color(0xFFFFD54F)
        )
    }
    val terminalLogPalette = remember {
        listOf(
            Color(0xFF808080), Color(0xFF00FFFF), Color(0xFF00FF00),
            Color(0xFFFFFF00), Color(0xFFFF5555), Color(0xFFFF00FF)
        )
    }

    val previewPalette = when (theme) {
        PaletteTheme.AQI -> aqiPalette
        PaletteTheme.BIO_NEON -> bioNeonPalette
        PaletteTheme.TERMINAL_LOG -> terminalLogPalette
        PaletteTheme.CUSTOM -> customPalette.ensureAtLeastTwo()
    }

    Box(Modifier.fillMaxSize()) {
        // ----- Live Animated Background (uses current controls) -----
        BackgroundAnimation(
            modifier = Modifier.fillMaxSize(),
            quality = quality,
            threshold = threshold,
            radiantUnit = radiant,
            colorUnit = colorUnit,
            opacity = opacity,
            colorTheme = theme,
            customPalette = if (theme == PaletteTheme.CUSTOM) customPalette else null
        )

        // ----- Frosted Control Panel -----
        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Background Theme", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            onApply(
                                BackgroundThemeSelection(
                                    theme = theme,
                                    customPalette = if (theme == PaletteTheme.CUSTOM) customPalette else emptyList(),
                                    threshold = threshold,
                                    radiantUnit = radiant,
                                    colorUnit = colorUnit,
                                    opacity = opacity,
                                    quality = quality
                                )
                            )
                        }
                    ) { Text("Apply") }
                }

                Spacer(Modifier.height(12.dp))

                // Color Theme (chips list — not sliders)
                Text("Color theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                ThemeChipsRow(
                    selected = theme,
                    onSelected = { theme = it }
                )

                Spacer(Modifier.height(12.dp))

                // Smooth gradient preview strip
                GradientPreviewBar(colors = previewPalette)

                // Custom theme: horizontal faders to build palette
                if (theme == PaletteTheme.CUSTOM) {
                    Spacer(Modifier.height(12.dp))
                    CustomPaletteFader(
                        palette = customPalette.ensureAtLeastTwo(),
                        onPaletteChange = { customPalette = it.ensureAtLeastTwo() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Settings — all horizontal sliders (faders)
                SettingsFaders(
                    threshold = threshold,
                    onThreshold = { threshold = it },
                    radiant = radiant,
                    onRadiant = { radiant = it },
                    colorUnit = colorUnit,
                    onColorUnit = { colorUnit = it },
                    opacity = opacity,
                    onOpacity = { opacity = it }
                )

                Spacer(Modifier.height(12.dp))

                // Quality (discrete profiles) — use chips
                Text("Quality", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                QualityChipsRow(
                    selected = quality,
                    onSelected = { quality = it }
                )
            }
        }
    }
}

/* ───────────────────────────── UI Pieces ───────────────────────────── */

@Composable
private fun ThemeChipsRow(
    selected: PaletteTheme,
    onSelected: (PaletteTheme) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = selected == PaletteTheme.AQI,
            onClick = { onSelected(PaletteTheme.AQI) },
            label = { Text("AQI") }
        )
        FilterChip(
            selected = selected == PaletteTheme.BIO_NEON,
            onClick = { onSelected(PaletteTheme.BIO_NEON) },
            label = { Text("Bio-Neon") }
        )
        FilterChip(
            selected = selected == PaletteTheme.TERMINAL_LOG,
            onClick = { onSelected(PaletteTheme.TERMINAL_LOG) },
            label = { Text("Terminal Log") }
        )
        FilterChip(
            selected = selected == PaletteTheme.CUSTOM,
            onClick = { onSelected(PaletteTheme.CUSTOM) },
            label = { Text("Custom") }
        )
    }
}

@Composable
private fun QualityChipsRow(
    selected: Quality,
    onSelected: (Quality) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(selected == Quality.LOW, onClick = { onSelected(Quality.LOW) }, label = { Text("Low") })
        FilterChip(selected == Quality.MEDIUM, onClick = { onSelected(Quality.MEDIUM) }, label = { Text("Medium") })
        FilterChip(selected == Quality.HIGH, onClick = { onSelected(Quality.HIGH) }, label = { Text("High") })
        FilterChip(selected == Quality.AUTO, onClick = { onSelected(Quality.AUTO) }, label = { Text("Auto") })
    }
}

@Composable
private fun GradientPreviewBar(colors: List<Color>) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(colors))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    )
}

/**
 * CustomPaletteFader
 * - Hue fader and Brightness (value) fader (both horizontal)
 * - Add/Remove to build a 2..8 color custom palette
 */
@Composable
private fun CustomPaletteFader(
    palette: List<Color>,
    onPaletteChange: (List<Color>) -> Unit
) {
    var hue by remember { mutableStateOf(200f) }      // 0..360
    var value by remember { mutableStateOf(1.0f) }    // 0..1

    val hueStrip = remember {
        listOf(
            Color(0xFFFF0000), // R
            Color(0xFFFFFF00), // Y
            Color(0xFF00FF00), // G
            Color(0xFF00FFFF), // C
            Color(0xFF0000FF), // B
            Color(0xFFFF00FF), // M
            Color(0xFFFF0000)  // R wrap
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Custom palette", style = MaterialTheme.typography.titleSmall)

        // Hue bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.horizontalGradient(hueStrip))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
        )
        Slider(
            value = hue / 360f,
            onValueChange = { hue = (it * 360f).coerceIn(0f, 360f) },
            modifier = Modifier.fillMaxWidth()
        )

        // Brightness bar
        val pure = hsvToColor(hue, 1f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.horizontalGradient(listOf(Color.Black, pure)))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
        )
        Slider(
            value = value,
            onValueChange = { value = it.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        // Current pick + swatches + controls
        val pick = hsvToColor(hue, 1f, value)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Current pick swatch
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(pick)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )

            // Existing palette swatches
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                palette.forEach { col ->
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }

            Button(
                onClick = { if (palette.size < 8) onPaletteChange(palette + pick) },
                enabled = palette.size < 8
            ) { Text("Add") }

            Button(
                onClick = { if (palette.size > 2) onPaletteChange(palette.dropLast(1)) },
                enabled = palette.size > 2
            ) { Text("Remove") }
        }
    }
}

/**
 * SettingsFaders — all sliders (horizontal)
 */
@Composable
private fun SettingsFaders(
    threshold: Float,
    onThreshold: (Float) -> Unit,
    radiant: Dp,
    onRadiant: (Dp) -> Unit,
    colorUnit: Float,
    onColorUnit: (Float) -> Unit,
    opacity: Float,
    onOpacity: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabeledSlider(
            label = "Threshold",
            value = threshold,
            onChange = onThreshold,
            valueRange = -0.30f..0.30f,
            format = { "%.2f".format(it) }
        )
        LabeledSlider(
            label = "Radiant size (dp)",
            value = (radiant.value - 8f) / (36f - 8f),   // normalize 8..36 → 0..1
            onChange = { onRadiant((8f + it * (36f - 8f)).dp) },
            valueRange = 0f..1f,
            format = { "${(8f + it * (36f - 8f)).roundToInt()}dp" }
        )
        LabeledSlider(
            label = "Color intensity",
            value = (colorUnit - 0.5f) / (1.5f - 0.5f), // normalize 0.5..1.5 → 0..1
            onChange = { onColorUnit(0.5f + it * (1.5f - 0.5f)) },
            valueRange = 0f..1f,
            format = { "%.2f".format(0.5f + it * 1.0f) }
        )
        LabeledSlider(
            label = "Opacity",
            value = (opacity - 0.2f) / (1f - 0.2f),     // normalize 0.2..1.0 → 0..1
            onChange = { onOpacity(0.2f + it * (1f - 0.2f)) },
            valueRange = 0f..1f,
            format = { "%.2f".format(0.2f + it * 0.8f) }
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ───────────────────────────── Utilities ───────────────────────────── */

private fun List<Color>.ensureAtLeastTwo(): List<Color> =
    if (size >= 2) this else this + List(2 - size) { Color.White }

/** Simple HSV→RGB for faders */
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val C = v * s
    val X = C * (1f - abs((h / 60f % 2f) - 1f))
    val m = v - C
    val (rp, gp, bp) = when {
        h < 60f  -> Triple(C, X, 0f)
        h < 120f -> Triple(X, C, 0f)
        h < 180f -> Triple(0f, C, X)
        h < 240f -> Triple(0f, X, C)
        h < 300f -> Triple(X, 0f, C)
        else     -> Triple(C, 0f, X)
    }
    return Color(rp + m, gp + m, bp + m)
}
