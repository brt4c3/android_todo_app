// data/themeData/ThemeData.kt
package com.example.todo_app.data.themeData

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo_app.ui.PaletteTheme

@Immutable
data class ThemeData(
    val theme: PaletteTheme = PaletteTheme.AQI,
    val threshold: Float = -0.10f,
    val radiantUnit: Dp = 20.dp,
    val colorUnit: Float = 1.0f,
    val opacity: Float = 1f,
    val customPalette: List<Color> = emptyList()
)
