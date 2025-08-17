package com.example.todo_app.data
// data/TaskRepository.kt


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.*

class TaskRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun insert(task: Task) = dao.insert(task)
    suspend fun update(task: Task) = dao.update(task)
    suspend fun delete(task: Task) = dao.delete(task)

    // Create a new task with your requested defaults:
    suspend fun createDefault(): Long = withContext(Dispatchers.IO) {
        val count = dao.count() + 1
        val name = "task_%04d".format(count)
        val expDateMidnight = LocalDate.now().plusWeeks(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val task = Task(
            task = name,
            exp_date_epoch = expDateMidnight,
            exp_dur_minutes = 7L * 24 * 60, // 1 week
            act_minutes = 0,
            note = defaultMarkdown(),
            running_since_epoch = null
        )
        dao.insert(task)
    }

    fun formatActual(minutes: Long): String {
        val d = minutes / (60*24)
        val h = (minutes % (60*24)) / 60
        val m = minutes % 60
        return "${d}days ${h} hours ${m} minutes"
    }

    fun defaultMarkdown(): String = """
        # title

        ## subtitle

        code snippet```bash

        echo "Hello, world!"```
    """.trimIndent()
}
