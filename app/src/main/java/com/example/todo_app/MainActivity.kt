package com.example.todo_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

// Navigation
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// UI
import com.example.todo_app.ui.*
import com.example.todo_app.ui.theme.TodoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppTheme {
                val nav = rememberNavController()

                // ✅ Theme VM that holds the full BackgroundThemeSelection
                val themeVm: ThemeViewModel = viewModel()
                val bg by themeVm.bg.collectAsState()

                NavHost(
                    navController = nav,
                    startDestination = "home"
                ) {
                    /* --------------------------- Home --------------------------- */
                    composable("home") {
                        HomeScreen(
                            bg = bg,  // ✅ pass current background selection
                            onEnter = {
                                nav.navigate("dashboard") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    /* ------------------------ Dashboard ------------------------ */
                    composable("dashboard") {
                        val vm: DashboardViewModel = viewModel(factory = VmFactory(application))
                        val state by vm.state.collectAsState()

                        DashboardScreen(
                            state = state,
                            bg = bg, // ✅ pass current background selection
                            onNewTask = { /* TODO open create task screen */ },
                            onOpen = { id -> nav.navigate("task/$id") },
                            onDelete = { task -> /* TODO delete via vm or repo */ },
                            onUndo = { /* TODO undo via vm or repo */ },

                            onOpenBackgroundTheme = { nav.navigate("themePicker") },
                            onHome = {
                                nav.navigate("home") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    /* -------------------- Background Theme Picker -------------------- */
                    composable("themePicker") {
                        BackgroundThemePickerPage(
                            onApply = { selection ->
                                // ✅ Save the entire selection so both screens update
                                themeVm.apply(selection)
                                nav.popBackStack()
                            }
                        )
                    }


                    /* --------------------------- Task detail ------------------------ */
                    composable(
                        route = "task/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val vm: TaskViewModel = viewModel(factory = VmFactory(application))
                        val id = backStackEntry.arguments!!.getLong("id")
                        val task by vm.forId(id).collectAsState(initial = null)
                        val isLoading by vm.loading.collectAsState(initial = false)

                        task?.let { t ->
                            TaskScreen(
                                task = t,
                                isRefreshing = isLoading,
                                formatActual = vm::formatActual,
                                onStart = { vm.start(id) },
                                onStop  = { vm.stop(id) },
                                onEdit  = { nav.navigate("edit/$id") },
                                onBack  = { nav.popBackStack() }
                            )
                        }
                    }

                    /* ----------------------------- Edit task ------------------------- */
                    composable(
                        route = "edit/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val vm: EditTaskViewModel = viewModel(factory = VmFactory(application))
                        val id = backStackEntry.arguments!!.getLong("id")
                        val taskState by vm.load(id).collectAsState(initial = null)

                        taskState?.let { t ->
                            EditTaskScreen(
                                state    = t,
                                onSave   = { updated -> vm.save(updated) { nav.popBackStack() } },
                                onCancel = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
