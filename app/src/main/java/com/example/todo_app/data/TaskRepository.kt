package com.example.todo_app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.*
import java.util.*

class TaskRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()
    fun observe(id: Long): Flow<Task?> = dao.observeById(id)
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun insert(task: Task) = dao.insert(task)
    suspend fun update(task: Task) = dao.update(task)
    suspend fun delete(task: Task) = dao.delete(task)
    suspend fun count() = dao.count()

    suspend fun createDefault(): Long = withContext(Dispatchers.IO) {
        val idx = dao.count() + 1
        val name = "task_%04d".format(idx)

        val now = Instant.now()
        val zone = ZoneId.systemDefault()

        val defaultDue = LocalDate.now().plusWeeks(1)
            .atTime(18, 0) // next week 18:00 by default
            .atZone(zone).toInstant().toEpochMilli()

        val defaultFinish = now.plus(Duration.ofDays(7)).toEpochMilli()

        val t = Task(
            task = name,
            exp_due_epoch = defaultDue,        // strict due
            exp_finish_epoch = defaultFinish,  // manual expected finish
            exp_dur_minutes = 7L * 24 * 60,    // manual duration default (1 week)
            act_minutes = 0L,                  // actual spent
            note = defaultMarkdown(),
            status = Status.WIP,
            running_since_epoch = null
        )
        dao.insert(t)
    }

    fun formatDuration(mins: Long): String {
        val d = mins / (60 * 24)
        val h = (mins % (60 * 24)) / 60
        val m = mins % 60
        return "${d}days ${h} hours ${m} minutes"
    }

    fun defaultMarkdown(): String = """
        # title

        ## subtitle

        code snippet
        ```bash
        echo "Hello, world!"
        ```
    """.trimIndent()
}
