package com.example.knotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.TaskDao
import com.example.knotes.data.dao.SearchHistoryDao
import com.example.knotes.data.dao.NoteVersionDao
import com.example.knotes.data.entity.*

@Database(
    entities = [
        Note::class,
        Task::class,
        Folder::class,
        ChecklistItem::class,
        Attachment::class,
        Tag::class,
        NoteTagCrossRef::class,
        SearchHistory::class,
        NoteVersion::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun folderDao(): FolderDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun noteVersionDao(): NoteVersionDao

    companion object {
        @Volatile
        private var INSTANCE: KNotesDatabase? = null

        fun getDatabase(context: Context): KNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KNotesDatabase::class.java,
                    "knotes_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
