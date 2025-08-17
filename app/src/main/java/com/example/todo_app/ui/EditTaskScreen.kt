package com.example.todo_app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Status
import com.example.todo_app.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class) // ✅ opt-in for TopAppBar in your M3 version
@Composable
fun EditTaskScreen(
    state: Task,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    var task by remember(state) { mutableStateOf(state) }

    val ctx = LocalContext.current
    val dateTimeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun pickDateTime(initialMillis: Long, onPicked: (Long) -> Unit) {
        val base = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), 0)
                    set(Calendar.MILLISECOND, 0)
                }
                TimePickerDialog(
                    ctx,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h)
                        cal.set(Calendar.MINUTE, min)
                        onPicked(cal.timeInMillis)
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(ctx)
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Status menu
    val statusOptions = remember { Status.values().toList() }
    var statusExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit task") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(onClick = { onSave(task) }, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                        Spacer(Modifier.width(8.dp)); Text("Save")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel")
                        Spacer(Modifier.width(8.dp)); Text("Cancel")
                    }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Task name
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Task name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = task.task,
                        onValueChange = { task = task.copy(task = it) },
                        label = { Text("Task") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Clock (Expiration + Expected finish)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Clock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    // Expiration Date & Time (strict due)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateTimeFmt.format(Date(task.exp_due_epoch)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expiration Date & Time") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { pickDateTime(task.exp_due_epoch) { picked -> task = task.copy(exp_due_epoch = picked) } },
                            modifier = Modifier.weight(0.8f)
                        ) { Text("Pick") }
                    }

                    // Expected finish Date & Time (manual, independent)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateTimeFmt.format(Date(task.exp_finish_epoch)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expected finish Date & Time") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { pickDateTime(task.exp_finish_epoch) { picked -> task = task.copy(exp_finish_epoch = picked) } },
                            modifier = Modifier.weight(0.8f)
                        ) { Text("Pick") }
                    }
                }
            }

            // Expected duration (manual HH:MM) — uses your external component
            ExpectedDurationInput(
                initialHours = (task.exp_dur_minutes / 60).toInt(),
                initialMinutes = (task.exp_dur_minutes % 60).toInt()
            ) { h, m ->
                task = task.copy(exp_dur_minutes = (h.coerceIn(0, 99) * 60 + m.coerceIn(0, 59)).toLong())
            }

            // Status (DropdownMenu anchored to a button)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = task.status.name.lowercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            OutlinedButton(onClick = { statusExpanded = true }) { Text("Change") }
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statusOptions.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name.lowercase()) },
                                        onClick = {
                                            task = task.copy(status = s)
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note (Markdown)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Note (Markdown)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = task.note,
                        onValueChange = { task = task.copy(note = it) },
                        label = { Text("Note") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        minLines = 6
                    )
                }
            }
        }
    }
}
