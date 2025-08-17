package com.example.todo_app.ui

// ui/DashboardViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo_app.data.Task
import com.example.todo_app.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(private val repo: TaskRepository) : ViewModel() {
    data class State(val tasks: List<Task>)
    val state: StateFlow<State> = repo.observeAll()
        .map { State(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State(emptyList()))

    private var lastDeleted: Task? = null

    fun createDefault(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.createDefault()
            onCreated(id)
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            lastDeleted = task
            repo.delete(task)
        }
    }

    fun undoDelete() {
        val snapshot = lastDeleted ?: return
        viewModelScope.launch {
            repo.insert(snapshot)
            lastDeleted = null
        }
    }
}
