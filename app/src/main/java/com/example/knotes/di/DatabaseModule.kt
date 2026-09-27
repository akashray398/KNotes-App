package com.example.knotes.di

import android.content.Context
import androidx.room.Room
import com.example.knotes.data.KNotesDatabase
import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KNotesDatabase {
        return Room.databaseBuilder(
            context,
            KNotesDatabase::class.java,
            "knotes_db"
        ).build()
    }

    @Provides
    fun provideNoteDao(database: KNotesDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideTaskDao(database: KNotesDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideFolderDao(database: KNotesDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideSearchHistoryDao(database: KNotesDatabase) = database.searchHistoryDao()

    @Provides
    fun provideNoteVersionDao(database: KNotesDatabase) = database.noteVersionDao()
}
