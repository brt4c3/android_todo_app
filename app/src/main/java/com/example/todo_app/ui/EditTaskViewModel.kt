package com.example.todo_app.ui

// ui/EditTaskViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo_app.data.Task
import com.example.todo_app.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class EditTaskViewModel(private val repo: TaskRepository) : ViewModel() {
    fun load(id: Long): Flow<Task?> = flow { emit(repo.get(id)) }
    fun save(updated: Task, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.update(updated); onDone()
        }
    }
}
