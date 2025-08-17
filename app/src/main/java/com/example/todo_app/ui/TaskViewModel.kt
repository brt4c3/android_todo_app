package com.example.todo_app.ui

// ui/TaskViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo_app.data.Task
import com.example.todo_app.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.Duration

class TaskViewModel(private val repo: TaskRepository) : ViewModel() {
    fun forId(id: Long): Flow<Task?> = flow { emit(repo.get(id)) }
    fun formatActual(mins: Long) = repo.formatActual(mins)

    fun start(id: Long) {
        viewModelScope.launch {
            repo.get(id)?.let { t ->
                if (t.running_since_epoch == null) repo.update(t.copy(running_since_epoch = System.currentTimeMillis()))
            }
        }
    }

    fun stop(id: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.get(id)?.let { t ->
                val add = t.running_since_epoch?.let { startMs ->
                    Duration.ofMillis(now - startMs).toMinutes()
                } ?: 0
                repo.update(t.copy(act_minutes = t.act_minutes + add, running_since_epoch = null))
            }
        }
    }
}
