package com.example.knotes.domain.usecase

import com.example.knotes.domain.repository.BackupRepository
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(outputStream: OutputStream) = repository.exportData(outputStream)
}

class ImportDataUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke(inputStream: InputStream) = repository.importData(inputStream)
}

class ClearAllDataUseCase @Inject constructor(private val repository: BackupRepository) {
    suspend operator fun invoke() = repository.clearAllData()
}
