package com.example.todo_app.ui

// ui/EditTaskScreen.kt

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import java.util.*

import androidx.compose.material3.MenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    state: Task,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    var task by remember(state) { mutableStateOf(state) }
    val ctx = LocalContext.current

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = task.exp_date_epoch }
        DatePickerDialog(ctx, { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            task = task.copy(exp_date_epoch = c.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val minuteOptions = listOf(15L, 30L, 60L, 120L, 24L*60, 7L*24*60)
    var expanded by remember { mutableStateOf(false) }

    Scaffold { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = task.task, onValueChange = { task = task.copy(task = it) },
                label = { Text("Task") }, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = Date(task.exp_date_epoch).toString(), onValueChange = {},
                    label = { Text("Expiration date") }, readOnly = true, modifier = Modifier.weight(1f)
                )
                Button(onClick = { openDatePicker() }) { Text("Pick date") }
            }
            // Quick fix to import Menu Anchor Type
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = "${task.exp_dur_minutes} minutes", onValueChange = {},
                    label = { Text("Expected duration") }, readOnly = true,
                    //modifier = Modifier.menuAnchor().fillMaxWidth()
                    // New API wants an explicit anchor type
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    minuteOptions.forEach { mins ->
                        DropdownMenuItem(text = { Text("$mins minutes") }, onClick = {
                            task = task.copy(exp_dur_minutes = mins); expanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = task.note, onValueChange = { task = task.copy(note = it) },
                label = { Text("Note (markdown)") }, modifier = Modifier.fillMaxWidth().height(180.dp)
            )

            Text("Actual time spent: ${formatActual(task.act_minutes)}")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onSave(task) }) { Text("Save") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

private fun formatActual(mins: Long): String {
    val d = mins / (60*24); val h = (mins % (60*24)) / 60; val m = mins % 60
    return "$d days $h hours $m minutes"
}
