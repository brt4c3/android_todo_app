package com.example.todo_app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onNewTask: () -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Task) -> Unit,
    onUndo: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { FloatingActionButton(onClick = onNewTask) { Text("+") } }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            HeaderRow()
            HorizontalDivider()
            LazyColumn {
                items(state.tasks) { t ->
                    RowItem(
                        t,
                        onClick = { onOpen(t.id) },
                        onDeleteClicked = {
                            onDelete(t)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
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

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Task", Modifier.weight(2f))
        Text("Expiration", Modifier.weight(1.2f))
        Text("Progress", Modifier.weight(1f))
        Text("Status", Modifier.weight(1f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun RowItem(t: Task, onClick: () -> Unit, onDeleteClicked: () -> Unit) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val pct = progressPercent(t.act_minutes, t.exp_dur_minutes)
    val pColor = progressColor(pct, t.status)

    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(t.task, Modifier.weight(2f).clickable { onClick() })
        Text(fmt.format(Date(t.exp_due_epoch)), Modifier.weight(1.2f))
        Text("$pct%", Modifier.weight(1f), color = pColor)
        StatusBadge(t.status, Modifier.weight(1f))
        IconButton(onClick = onDeleteClicked) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
    }
}
