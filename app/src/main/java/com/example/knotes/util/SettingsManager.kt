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
