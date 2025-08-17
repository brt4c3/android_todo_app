package com.example.todo_app.data

// data/Task.kt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val task: String,              // "task_0001", default set in repo
    val exp_date_epoch: Long,      // expiration date (midnight local) millis
    val exp_dur_minutes: Long,     // expected duration in minutes (e.g., 10080 = 7d)
    val act_minutes: Long,         // accumulated actual minutes
    val note: String,
    val running_since_epoch: Long? // null if not running
)
