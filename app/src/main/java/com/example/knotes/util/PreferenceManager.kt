package com.example.knotes.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "PreferenceManager"
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "knotes_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to standard", e)
            context.getSharedPreferences("knotes_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isAppLocked(): Boolean {
        return try {
            sharedPreferences.getBoolean("app_locked", false)
        } catch (e: Exception) {
            false
        }
    }

    fun setAppLocked(locked: Boolean) {
        try {
            sharedPreferences.edit().putBoolean("app_locked", locked).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set app_locked", e)
        }
    }
}
