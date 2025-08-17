package com.example.todo_app.ui

// ui/VmFactory.kt

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todo_app.data.AppDatabase
import com.example.todo_app.data.TaskRepository

class VmFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = TaskRepository(AppDatabase.get(app).taskDao())
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repo) as T
            modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(repo) as T
            modelClass.isAssignableFrom(EditTaskViewModel::class.java) -> EditTaskViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown VM $modelClass")
        }
    }
}
