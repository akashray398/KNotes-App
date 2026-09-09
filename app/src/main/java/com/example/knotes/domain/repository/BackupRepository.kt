package com.example.knotes.domain.repository

import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    suspend fun exportData(outputStream: OutputStream): Result<Unit>
    suspend fun importData(inputStream: InputStream): Result<Unit>
    suspend fun clearAllData(): Result<Unit>
}
