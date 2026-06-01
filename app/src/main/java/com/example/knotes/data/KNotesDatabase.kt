package com.example.knotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.entity.Note
import com.example.knotes.data.entity.Task

@Database(entities = [Note::class, Task::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class KNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: KNotesDatabase? = null

        fun getDatabase(context: Context): KNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KNotesDatabase::class.java,
                    "knotes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
