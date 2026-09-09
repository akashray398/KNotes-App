package com.example.knotes.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FILTER_PRIORITY = stringPreferencesKey("filter_priority")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val HIGHEST_STREAK = intPreferencesKey("highest_streak")
        val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val IS_AI_ENABLED = booleanPreferencesKey("is_ai_enabled")
        val AI_CONSENT_GIVEN = booleanPreferencesKey("ai_consent_given")
        val LAST_SYNCED = longPreferencesKey("last_sync_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    val lastSynced: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_SYNCED] ?: 0L
    }

    suspend fun setLastSynced(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_SYNCED] = time
        }
    }

    val isAiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_AI_ENABLED] ?: false
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_AI_ENABLED] = enabled
        }
    }

    val aiConsentGiven: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.AI_CONSENT_GIVEN] ?: false
    }

    suspend fun setAiConsentGiven(given: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_CONSENT_GIVEN] = given
        }
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_GRID_VIEW] ?: false
    }

    suspend fun setGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_GRID_VIEW] = isGrid
        }
    }

    val currentStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.CURRENT_STREAK] ?: 0
    }

    val highestStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.HIGHEST_STREAK] ?: 0
    }

    val lastStreakDate: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_STREAK_DATE] ?: 0L
    }

    suspend fun updateStreak(streak: Int, date: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CURRENT_STREAK] = streak
            val currentHighest = preferences[Keys.HIGHEST_STREAK] ?: 0
            if (streak > currentHighest) {
                preferences[Keys.HIGHEST_STREAK] = streak
            }
            preferences[Keys.LAST_STREAK_DATE] = date
        }
    }

    val themeMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: 0 // 0: System, 1: Light, 2: Dark
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    val sortOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SORT_ORDER] ?: "NEWEST"
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_ORDER] = order
        }
    }
}
