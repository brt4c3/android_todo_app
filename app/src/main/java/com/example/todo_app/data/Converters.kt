package com.example.todo_app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromStatus(s: Status): String = s.name
    @TypeConverter fun toStatus(s: String): Status = runCatching { Status.valueOf(s) }.getOrElse { Status.WIP }
}
