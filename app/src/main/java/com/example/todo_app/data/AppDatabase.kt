package com.example.todo_app.data

// data/AppDatabase.kt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "todo-db"
            )
                // For a quick start, destructive migration. Ask me for a proper Migration if needed.
                // Old overload is deprecated; this keeps the same behavior (drop & recreate on mismatch)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
