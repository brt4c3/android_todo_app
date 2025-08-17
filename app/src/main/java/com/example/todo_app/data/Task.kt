package com.example.todo_app.data

// data/Task.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    // UI fields
    val task: String,                // e.g., "task_0001"

    // 1) Expiration (strict due) — full datetime in millis
    val exp_due_epoch: Long,

    // 2) Expected finish (manual) — full datetime in millis
    val exp_finish_epoch: Long,

    // 3) Expected duration (manual) — minutes total
    val exp_dur_minutes: Long,

    // 4) Actual time spent — minutes total (accumulated)
    val act_minutes: Long,

    // Notes (markdown)
    val note: String,

    // Status
    val status: Status = Status.WIP,

    // Timer — non-null while running
    val running_since_epoch: Long? = null
)

