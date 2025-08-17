package com.example.todo_app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    task: Task,
    isRefreshing: Boolean,
    formatActual: (Long) -> String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val pct = progressPercent(task.act_minutes, task.exp_dur_minutes).coerceIn(0, 999)
    val progColor = progressColor(pct, task.status)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        topBar = { TopAppBar(title = { Text(task.task) }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = onStart, enabled = task.running_since_epoch == null && !isRefreshing, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Start")
                        Spacer(Modifier.width(8.dp)); Text("")
                    }
                    Button(onClick = onStop, enabled = task.running_since_epoch != null && !isRefreshing, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Stop, contentDescription = "Stop")
                        Spacer(Modifier.width(8.dp)); Text("")
                    }
                    OutlinedButton(onClick = onEdit, enabled = !isRefreshing, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                        Spacer(Modifier.width(8.dp)); Text("")
                    }
                    OutlinedButton(onClick = onBack, enabled = !isRefreshing, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(Modifier.width(8.dp)); Text("")
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize()) {

            Column(
                Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Donut + status
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProgressDonut(
                            percent = pct.coerceAtMost(100),
                            diameter = 180.dp,
                            strokeWidth = 16.dp,
                            trackColor = trackColor,
                            progressColor = progColor
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$pct%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                StatusBadge(task.status)
                            }
                        }
                        Text(
                            text = "Actual: ${formatActual(task.act_minutes)}  •  Expected: ${formatActual(task.exp_dur_minutes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Details grid (uses the new fields)
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelValue("Expiration (due)", fmt.format(Date(task.exp_due_epoch)))
                        LabelValue("Expected finish", fmt.format(Date(task.exp_finish_epoch)))
                        LabelValue("Expected duration", formatActual(task.exp_dur_minutes))
                    }
                }

                // Notes
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, tonalElevation = 0.dp) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) { MarkdownText(task.note) }
                        }
                    }
                }
            }

            if (isRefreshing) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable private fun LabelValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.3f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

@Composable private fun ProgressDonut(
    percent: Int,
    diameter: Dp,
    strokeWidth: Dp,
    trackColor: Color,
    progressColor: Color,
    center: @Composable () -> Unit
) {
    val anim by animateFloatAsState(targetValue = percent / 100f, label = "donut")
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawArc(trackColor, 0f, 360f, false, size = this.size, style = Stroke(width = strokeWidth.toPx()))
            drawArc(progressColor, -90f, 360f * anim, false, size = this.size, style = Stroke(width = strokeWidth.toPx()))
        }
        center()
    }
}
