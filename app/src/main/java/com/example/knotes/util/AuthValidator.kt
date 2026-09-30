package com.example.knotes.util

import android.util.Patterns

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
    val strength: PasswordStrength,
    val errorMessage: String? = null
)

object AuthValidator {

    fun validateEmail(email: String): Pair<Boolean, String?> {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) {
            return false to "Email address is required"
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return false to "Please enter a valid email address"
        }
        return true to null
    }

    fun validateFullName(name: String): Pair<Boolean, String?> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return false to "Full name is required"
        }
        if (trimmed.length < 2) {
            return false to "Name must be at least 2 characters long"
        }
        return true to null
    }

    fun validatePassword(password: String): PasswordValidationResult {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val isValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar

        var score = 0
        if (hasMinLength) score++
        if (hasUppercase) score++
        if (hasLowercase) score++
        if (hasDigit) score++
        if (hasSpecialChar) score++

        val strength = when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }

        val errorMessage = when {
            !hasMinLength -> "Password must be at least 8 characters long"
            !hasUppercase -> "Password must contain at least 1 uppercase letter"
            !hasLowercase -> "Password must contain at least 1 lowercase letter"
            !hasDigit -> "Password must contain at least 1 number"
            !hasSpecialChar -> "Password must contain at least 1 special character"
            else -> null
        }

        return PasswordValidationResult(
            isValid = isValid,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar,
            strength = strength,
            errorMessage = errorMessage
        )
    }

    fun mapFirebaseException(e: Throwable?): String {
        if (e == null) return "An unexpected error occurred. Please try again."
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("no network") || msg.contains("network error") || msg.contains("unable to resolve host") ->
                "Network unavailable. Please check your internet connection."
            msg.contains("invalid-credential") || msg.contains("wrong-password") || msg.contains("user-not-found") || msg.contains("invalid email or password") ->
                "Invalid email or password."
            msg.contains("email-already-in-use") || msg.contains("already exists") ->
                "An account with this email address already exists."
            msg.contains("too-many-requests") || msg.contains("blocked") ->
                "Too many failed attempts. Please wait a few minutes before trying again."
            msg.contains("user-disabled") ->
                "This account has been disabled. Please contact support."
            msg.contains("weak-password") ->
                "The password provided is too weak."
            else -> "Authentication failed. Please check your details and try again."
        }
    }
}
