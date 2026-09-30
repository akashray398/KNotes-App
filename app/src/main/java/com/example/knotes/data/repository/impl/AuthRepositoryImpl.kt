package com.example.knotes.data.repository.impl

import android.util.Log
import com.example.knotes.data.dao.FolderDao
import com.example.knotes.data.dao.NoteDao
import com.example.knotes.data.dao.SearchHistoryDao
import com.example.knotes.data.dao.TaskDao
import com.example.knotes.domain.repository.AuthRepository
import com.example.knotes.domain.repository.AuthState
import com.example.knotes.domain.repository.UserProfile
import com.example.knotes.util.AuthValidator
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val folderDao: FolderDao,
    private val searchHistoryDao: SearchHistoryDao
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"

    override val currentUser: UserProfile?
        get() = auth?.currentUser?.toUserProfile()

    override val authState: Flow<AuthState> = callbackFlow {
        if (auth == null) {
            trySend(AuthState.LoggedOut)
            awaitClose { }
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(AuthState.LoggedOut)
            } else {
                user.reload()
                val profile = user.toUserProfile()
                if (user.isEmailVerified || isProviderGoogle(user)) {
                    trySend(AuthState.LoggedIn(profile))
                } else {
                    trySend(AuthState.EmailVerificationRequired(profile))
                }
            }
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun isProviderGoogle(user: FirebaseUser): Boolean {
        return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    private fun FirebaseUser.toUserProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            email = email ?: "",
            displayName = displayName ?: email?.substringBefore("@") ?: "KNotes User",
            isEmailVerified = isEmailVerified || isProviderGoogle(this),
            photoUrl = photoUrl?.toString()
        )
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            if (auth == null) return Result.failure(Exception("Authentication service unavailable"))
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: return Result.failure(Exception("Login failed"))
            val profile = user.toUserProfile()
            
            // Clean local database to guarantee user data isolation when logging in
            clearLocalDatabase()
            
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun signup(fullName: String, email: String, password: String): Result<UserProfile> {
        return try {
            if (auth == null) return Result.failure(Exception("Authentication service unavailable"))
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: return Result.failure(Exception("Signup failed"))

            // Set display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName.trim())
                .build()
            user.updateProfile(profileUpdates).await()

            // Send verification email
            try {
                user.sendEmailVerification().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send initial verification email", e)
            }

            // Save user profile document in Firestore
            saveUserToFirestore(user.uid, fullName.trim(), email.trim())

            // Clear previous local cache for data isolation
            clearLocalDatabase()

            Result.success(user.toUserProfile())
        } catch (e: Exception) {
            Log.e(TAG, "Signup error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        return try {
            if (auth == null) return Result.failure(Exception("Authentication service unavailable"))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: return Result.failure(Exception("Google Sign-In failed"))

            saveUserToFirestore(user.uid, user.displayName ?: "Google User", user.email ?: "")

            clearLocalDatabase()

            Result.success(user.toUserProfile())
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            if (auth == null) return Result.failure(Exception("Authentication service unavailable"))
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error", e)
            // Return success with generic message to prevent account enumeration
            Result.success(Unit)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth?.currentUser ?: return Result.failure(Exception("No user logged in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Send email verification error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun reloadUser(): Result<UserProfile?> {
        return try {
            val user = auth?.currentUser ?: return Result.success(null)
            user.reload().await()
            Result.success(user.toUserProfile())
        } catch (e: Exception) {
            Log.e(TAG, "Reload user error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            auth?.signOut()
            clearLocalDatabase()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            Result.failure(Exception("Logout failed: ${e.message}"))
        }
    }

    override suspend fun updateProfile(displayName: String): Result<Unit> {
        return try {
            val user = auth?.currentUser ?: return Result.failure(Exception("No user logged in"))
            val updates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(updates).await()

            if (firestore != null) {
                firestore.collection("users").document(user.uid)
                    .set(mapOf("displayName" to displayName.trim()), SetOptions.merge())
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update profile error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth?.currentUser ?: return Result.failure(Exception("No user logged in"))
            val email = user.email ?: return Result.failure(Exception("User email missing"))

            // Re-authenticate
            val credential = EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()

            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Change password error", e)
            Result.failure(Exception(AuthValidator.mapFirebaseException(e)))
        }
    }

    private suspend fun saveUserToFirestore(uid: String, name: String, email: String) {
        if (firestore == null) return
        try {
            val userMap = mapOf(
                "uid" to uid,
                "displayName" to name,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid)
                .set(userMap, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save user profile to Firestore", e)
        }
    }

    private suspend fun clearLocalDatabase() {
        try {
            noteDao.deleteAll()
            taskDao.deleteAll()
            folderDao.deleteAll()
            searchHistoryDao.clearHistory()
            Log.d(TAG, "Local database cleared for user isolation")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local database", e)
        }
    }
}
