package com.example.todo_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import java.text.SimpleDateFormat
import java.util.*

// ✅ Opt-in + import for SmallTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.SmallTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    task: Task,
    formatActual: (Long) -> String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    Scaffold(topBar = { TopAppBar(title = { Text(task.task) }) }) { pad ->
    //Scaffold(topBar = { SmallTopAppBar(title = { Text(task.task) }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Expiration date: ${fmt.format(Date(task.exp_date_epoch))}")
            Text("Expected duration: ${task.exp_dur_minutes} minutes")
            Text("Actual time spent: ${formatActual(task.act_minutes)}")
            Spacer(Modifier.height(12.dp))
            Text("Note:")
            OutlinedTextField(
                value = task.note,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().height(160.dp),
                readOnly = true
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onStart, enabled = task.running_since_epoch == null) { Text("Start") }
                Button(onClick = onStop,  enabled = task.running_since_epoch != null) { Text("Stop") }
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                OutlinedButton(onClick = onBack) { Text("Back") }
            }
        }
    }
}
