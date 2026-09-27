package com.example.knotes.util

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

object AiErrorMapper {

    fun mapErrorToUserMessage(throwable: Throwable): String {
        if (throwable is CancellationException) throw throwable

        return when (throwable) {
            is SocketTimeoutException -> {
                "The AI request timed out. Please try again."
            }
            is UnknownHostException, is IOException -> {
                "Unable to connect to the AI service. Please check your internet connection and try again."
            }
            is HttpException -> {
                when (throwable.code()) {
                    401, 403 -> "The AI service configuration is invalid. Please check the configured API credentials."
                    429 -> "The AI service is temporarily busy. Please try again shortly."
                    in 500..599 -> "The AI service is temporarily unavailable. Please try again later."
                    else -> "AI service returned an error (${throwable.code()}). Please try again."
                }
            }
            is IllegalArgumentException, is IllegalStateException -> {
                throwable.message ?: "Invalid request parameters."
            }
            else -> {
                val msg = throwable.message ?: ""
                when {
                    msg.contains("API_KEY", ignoreCase = true) || msg.contains("401", ignoreCase = true) || msg.contains("unauthorized", ignoreCase = true) -> {
                        "The AI service configuration is invalid. Please check the configured API credentials."
                    }
                    msg.contains("429", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true) -> {
                        "The AI service is temporarily busy. Please try again shortly."
                    }
                    msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) -> {
                        "The AI request timed out. Please try again."
                    }
                    else -> msg.ifBlank { "Something went wrong with the AI service. Please try again." }
                }
            }
        }
    }

    fun isTransientError(throwable: Throwable): Boolean {
        if (throwable is CancellationException) return false
        
        return when (throwable) {
            is SocketTimeoutException, is UnknownHostException, is IOException -> true
            is HttpException -> {
                val code = throwable.code()
                code == 429 || code in 500..599
            }
            else -> {
                val msg = throwable.message ?: ""
                msg.contains("429", ignoreCase = true) ||
                msg.contains("500", ignoreCase = true) ||
                msg.contains("503", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true)
            }
        }
    }
}
