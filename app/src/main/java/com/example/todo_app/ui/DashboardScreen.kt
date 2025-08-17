package com.example.todo_app.ui

// ui/DashboardScreen.kt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
// added Horizontal divider
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onNewTask: () -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Task) -> Unit,
    onUndo: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = onNewTask) { Text("+") } }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            HeaderRow()
            // Divider is deprecated; use HorizontalDivider instead
            HorizontalDivider()
            LazyColumn {
                items(state.tasks) { t ->
                    RowItem(
                        t,
                        onClick = { onOpen(t.id) },
                        onDeleteClicked = {
                            onDelete(t)
                            scope.launch {
                                val res = snackbar.showSnackbar(
                                    message = "Deleted ${t.task}",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) onUndo()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Task", Modifier.weight(2f))
        Text("Expiration", Modifier.weight(1.2f))
        Text("Expected (min)", Modifier.weight(1.2f))
        Text("Actual", Modifier.weight(1f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable private fun RowItem(t: Task, onClick: () -> Unit, onDeleteClicked: () -> Unit) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(t.task, Modifier.weight(2f).clickable { onClick() })
        Text(fmt.format(Date(t.exp_date_epoch)), Modifier.weight(1.2f))
        Text("${t.exp_dur_minutes}", Modifier.weight(1.2f))
        Text(formatActual(t.act_minutes), Modifier.weight(1f))
        IconButton(onClick = onDeleteClicked) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
    }
}

private fun formatActual(mins: Long): String {
    val d = mins / (60*24); val h = (mins % (60*24)) / 60; val m = mins % 60
    return "$d d $h h $m m"
}
