package com.example.todo_app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Status

/** Pure math helper (non-composable) */
fun progressPercent(actualMin: Long, expectedMin: Long): Int =
    if (expectedMin <= 0) 0 else ((actualMin.toDouble() / expectedMin.toDouble()) * 100.0).toInt()

/** Pure mapping helper (non-composable) */
fun progressColor(pct: Int, status: Status): Color = when {
    status == Status.DONE -> Color(0xFF2E7D32) // green override
    pct > 100            -> Color(0xFFB00020) // red
    pct == 100           -> Color(0xFF1E88E5) // blue
    else                 -> Color(0xFFFBC02D) // yellow
}

/** Renders a small status chip (COMPOSABLE) */
@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        Status.DONE   -> Color(0xFF2E7D32) to Color.White
        Status.CANCEL -> Color(0xFF9E9E9E) to Color.White
        Status.WIP    -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = bg, tonalElevation = 0.dp, modifier = modifier) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(status.name.lowercase(), color = fg)
        }
    }
}
