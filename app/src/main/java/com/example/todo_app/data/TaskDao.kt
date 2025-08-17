package com.example.todo_app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY exp_due_epoch ASC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Long

    // timer helpers (optional shortcuts)
    @Query("UPDATE tasks SET running_since_epoch = :now WHERE id = :id AND running_since_epoch IS NULL")
    suspend fun markStarted(id: Long, now: Long)

    @Query("""
        UPDATE tasks
        SET act_minutes = act_minutes + ((:now - running_since_epoch)/60000),
            running_since_epoch = NULL
        WHERE id = :id AND running_since_epoch IS NOT NULL
    """)
    suspend fun markStopped(id: Long, now: Long)
}
