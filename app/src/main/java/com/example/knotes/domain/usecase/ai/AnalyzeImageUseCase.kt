package com.example.knotes.domain.usecase.ai

import android.graphics.Bitmap
import com.example.knotes.domain.repository.AiRepository
import javax.inject.Inject

class AnalyzeImageUseCase @Inject constructor(private val repository: AiRepository) {
    suspend operator fun invoke(bitmap: Bitmap, prompt: String) = repository.analyzeImage(bitmap, prompt)
}
