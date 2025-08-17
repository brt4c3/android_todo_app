package com.example.todo_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

// ✅ Navigation (add these)
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// your UI imports
import com.example.todo_app.ui.*

// add this import:
import com.example.todo_app.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppTheme {                  // ← was MaterialTheme { ... }
                val nav = androidx.navigation.compose.rememberNavController()
                androidx.navigation.compose.NavHost(navController = nav, startDestination = "dashboard") {
            //MaterialTheme {
                //val nav = rememberNavController()
                //NavHost(navController = nav, startDestination = "dashboard") {
                    composable("dashboard") {
                        val vm: DashboardViewModel = viewModel(factory = VmFactory(application))
                        val state by vm.state.collectAsState()

                        DashboardScreen(
                            state = state,
                            onNewTask = { vm.createDefault { id -> nav.navigate("task/$id") } },
                            onOpen = { id -> nav.navigate("task/$id") },
                            onDelete = { t -> vm.delete(t) },
                            onUndo   = { vm.undoDelete() }
                        )
                    }
                    composable(
                        route = "task/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) {
                        val vm: TaskViewModel = viewModel(factory = VmFactory(application))
                        val id = it.arguments!!.getLong("id")
                        val task by vm.forId(id).collectAsState(initial = null)
                        val isLoading by vm.loading.collectAsState(initial = false)

                        task?.let { t ->
                            TaskScreen(
                                task = t,
                                isRefreshing = isLoading,                 // NEW
                                formatActual = vm::formatActual,
                                onStart = { vm.start(id) },
                                onStop  = { vm.stop(id) },
                                onEdit  = { nav.navigate("edit/$id") },
                                onBack  = { nav.popBackStack() }
                            )
                        }
                    }
                    composable(
                        route = "edit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) {
                        val vm: EditTaskViewModel = viewModel(factory = VmFactory(application))
                        val id = it.arguments!!.getLong("id")
                        val task by vm.load(id).collectAsState(null)

                        task?.let { t ->
                            EditTaskScreen(
                                state   = t,
                                onSave  = { updated -> vm.save(updated) { nav.popBackStack() } },
                                onCancel= { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
