package com.example.todo_app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todo_app.data.AppDatabase
import com.example.todo_app.data.TaskRepository

/**
 * ViewModel factory that supports three patterns:
 * 1) AndroidViewModel(Application)
 * 2) ViewModel(TaskRepository)
 * 3) ViewModel() no-arg
 *
 * Usage:
 *   val vm: TaskViewModel = viewModel(factory = VmFactory(application))
 */
class VmFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 1) AndroidViewModel(Application)
        try {
            val ctor = modelClass.getConstructor(Application::class.java)
            return ctor.newInstance(application) as T
        } catch (_: NoSuchMethodException) {
            // ignore and try next
        }

        // 2) ViewModel(TaskRepository)
        val repo by lazy { TaskRepository(AppDatabase.get(application).taskDao()) }
        try {
            val ctor = modelClass.getConstructor(TaskRepository::class.java)
            return ctor.newInstance(repo) as T
        } catch (_: NoSuchMethodException) {
            // ignore and try next
        }

        // 3) ViewModel() no-arg
        try {
            val ctor = modelClass.getConstructor()
            return ctor.newInstance() as T
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Unknown ViewModel class ${modelClass.name}. " +
                        "Expected a constructor with Application or TaskRepository or no-arg.", e
            )
        }
    }
}
