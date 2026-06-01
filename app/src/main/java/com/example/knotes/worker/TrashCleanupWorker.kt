package com.example.knotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.knotes.data.repository.NoteRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NoteRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        repository.deleteOldTrashedNotes(thirtyDaysAgo)
        return Result.success()
    }
}
