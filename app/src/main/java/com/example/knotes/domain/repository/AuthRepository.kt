package com.example.knotes.domain.repository

import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean,
    val photoUrl: String? = null
)

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val user: UserProfile) : AuthState()
    data class EmailVerificationRequired(val user: UserProfile) : AuthState()
}

interface AuthRepository {
    val currentUser: UserProfile?
    val authState: Flow<AuthState>

    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun signup(fullName: String, email: String, password: String): Result<UserProfile>
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Result<UserProfile?>
    suspend fun logout(): Result<Unit>
    suspend fun updateProfile(displayName: String): Result<Unit>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
}
