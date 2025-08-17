package com.example.todo_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todo_app.data.Task
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

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
        //topBar = {
        //    CenterAlignedTopAppBar(
        //        title = { Text("Your Tasks", style = MaterialTheme.typography.titleLarge) }
        //    )
        //},
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewTask,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                text = { Text("New Task") }
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            // Home or Dashboard backgrounds:
            BackgroundAnimation(
                modifier = Modifier.fillMaxSize(),
                quality = Quality.AUTO,    // or LOW/MEDIUM/HIGH
                threshold = -0.10f,        // fewer blobs if higher
                radiantUnit = 20.dp,       // blob size scaling
                colorUnit = 1.0f,          // color intensity
                opacity = 1f               // background opacity
            )


            // Foreground content container with “glass card” feel
            Column(
                Modifier
                    .padding(pad)
                    .padding(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title row
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))

                        // Header
                        HeaderRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )

                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(4.dp))

                        // List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.tasks, key = { it.id }) { t ->
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
        }
    }
}

@Composable
private fun HeaderRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Task", Modifier.weight(2f), fontWeight = FontWeight.SemiBold)
        Text("Due", Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold)
        Text("Prog.", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text("Stat.", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun RowItem(
    t: Task,
    onClick: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val pct = progressPercent(t.act_minutes, t.exp_dur_minutes).coerceIn(0, 999)
    val pColor = progressColor(pct, t.status)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t.task,
                    Modifier
                        .weight(2f)
                        .clickable(onClick = onClick),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    fmt.format(Date(t.exp_due_epoch)),
                    Modifier.weight(1.2f),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Percent text colored by progress rule
                Text(
                    "$pct%",
                    Modifier.weight(1f),
                    color = pColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Status chip
                StatusBadge(t.status, Modifier.weight(1f))

                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            // Subtle linear progress hint under row (not exact time, just a quick visual)
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                color = pColor
            )
        }
    }
}
