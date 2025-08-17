package com.example.todo_app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo_app.data.AppDatabase
import com.example.todo_app.data.Task
import com.example.todo_app.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository(AppDatabase.get(app).taskDao())

    fun forId(id: Long): Flow<Task?> = repo.observe(id)
    fun formatActual(mins: Long) = repo.formatDuration(mins)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun start(id: Long) = viewModelScope.launch {
        _loading.value = true
        try {
            repo.get(id)?.let { t ->
                if (t.running_since_epoch == null) {
                    repo.update(t.copy(running_since_epoch = System.currentTimeMillis()))
                }
            }
        } finally { _loading.value = false }
    }

    fun stop(id: Long) = viewModelScope.launch {
        _loading.value = true
        try {
            val now = System.currentTimeMillis()
            repo.get(id)?.let { t ->
                val add = t.running_since_epoch?.let { start ->
                    Duration.ofMillis(now - start).toMinutes()
                } ?: 0
                repo.update(t.copy(
                    act_minutes = t.act_minutes + add,
                    running_since_epoch = null
                ))
            }
        } finally { _loading.value = false }
    }
}
