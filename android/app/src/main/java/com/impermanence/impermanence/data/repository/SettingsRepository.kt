package com.impermanence.impermanence.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val use24HourClockKey = booleanPreferencesKey("use24HourClock")
    private val loopDaysKey = booleanPreferencesKey("loopDays")

    val use24HourClock: Flow<Boolean> = dataStore.booleanFlow(use24HourClockKey, defaultValue = false)
    val loopDays: Flow<Boolean> = dataStore.booleanFlow(loopDaysKey, defaultValue = true)

    suspend fun setUse24HourClock(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[use24HourClockKey] = enabled }
    }

    suspend fun setLoopDays(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[loopDaysKey] = enabled }
    }

    private fun DataStore<Preferences>.booleanFlow(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> {
        return data.catch { throwable ->
            if (throwable is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw throwable
            }
        }.map { prefs -> prefs[key] ?: defaultValue }
    }
}
