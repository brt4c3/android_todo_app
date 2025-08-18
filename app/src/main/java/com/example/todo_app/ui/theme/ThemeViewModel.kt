package com.example.todo_app.ui

import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel : ViewModel() {

    // Start with some defaults (same as in your picker)
    private val _bg = MutableStateFlow(
        BackgroundThemeSelection(
            theme = PaletteTheme.BIO_NEON,
            customPalette = listOf(
                androidx.compose.ui.graphics.Color(0xFF00D4FF),
                androidx.compose.ui.graphics.Color(0xFF7FE0FF),
                androidx.compose.ui.graphics.Color(0xFFFF00A5),
                androidx.compose.ui.graphics.Color(0xFFFFD54F)
            ),
            threshold = -0.10f,
            radiantUnit = 20.dp,
            colorUnit = 1.0f,
            opacity = 1f,
            quality = Quality.HIGH
        )
    )
    val bg: StateFlow<BackgroundThemeSelection> = _bg

    fun apply(selection: BackgroundThemeSelection) {
        _bg.value = selection
    }
}
