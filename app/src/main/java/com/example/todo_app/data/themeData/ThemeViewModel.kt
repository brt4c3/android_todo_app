// data/themeData/ThemeViewModel.kt
package com.example.todo_app.data.themeData

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todo_app.ui.PaletteTheme

class ThemeViewModel : ViewModel() {
    private val _themeData = MutableStateFlow(ThemeData())
    val themeData: StateFlow<ThemeData> = _themeData

    fun setTheme(theme: PaletteTheme) {
        _themeData.update { it.copy(theme = theme) }
    }

    fun setThreshold(value: Float) {
        _themeData.update { it.copy(threshold = value) }
    }

    fun setRadiantUnit(value: Float) {
        _themeData.update { it.copy(radiantUnit = value.dp) }
    }

    fun setColorUnit(value: Float) {
        _themeData.update { it.copy(colorUnit = value) }
    }

    fun setOpacity(value: Float) {
        _themeData.update { it.copy(opacity = value) }
    }

    fun setCustomPalette(palette: List<Color>) {
        _themeData.update { it.copy(customPalette = palette) }
    }
}
