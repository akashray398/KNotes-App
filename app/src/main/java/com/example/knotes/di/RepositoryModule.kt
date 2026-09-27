package com.example.knotes.di

import com.example.knotes.data.repository.impl.AiRepositoryImpl
import com.example.knotes.data.repository.impl.BackupRepositoryImpl
import com.example.knotes.data.repository.impl.FolderRepositoryImpl
import com.example.knotes.data.repository.impl.GroqAiRepositoryImpl
import com.example.knotes.data.repository.impl.NoteRepositoryImpl
import com.example.knotes.data.repository.impl.NoteVersionRepositoryImpl
import com.example.knotes.data.repository.impl.TaskRepositoryImpl
import com.example.knotes.domain.repository.AiRepository
import com.example.knotes.domain.repository.BackupRepository
import com.example.knotes.domain.repository.FolderRepository
import com.example.knotes.domain.repository.NoteRepository
import com.example.knotes.domain.repository.NoteVersionRepository
import com.example.knotes.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        folderRepositoryImpl: FolderRepositoryImpl
    ): FolderRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(
        searchHistoryRepositoryImpl: com.example.knotes.data.repository.impl.SearchHistoryRepositoryImpl
    ): com.example.knotes.domain.repository.SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        dualAiRepositoryImpl: com.example.knotes.data.repository.impl.DualAiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindNoteVersionRepository(
        noteVersionRepositoryImpl: NoteVersionRepositoryImpl
    ): NoteVersionRepository
}
