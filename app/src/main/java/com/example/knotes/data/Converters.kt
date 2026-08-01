package com.example.knotes.data

import androidx.room.TypeConverter
import com.example.knotes.data.entity.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): Priority {
        return Priority.valueOf(priority)
    }

    @TypeConverter
    fun fromRecurrence(recurrence: com.example.knotes.data.entity.Recurrence): String {
        return recurrence.name
    }

    @TypeConverter
    fun toRecurrence(recurrence: String): com.example.knotes.data.entity.Recurrence {
        return com.example.knotes.data.entity.Recurrence.valueOf(recurrence)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
