package com.example.capturetrigger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class CounterRepository(private val context: Context) {
    private val COUNTER_KEY = intPreferencesKey("capture_counter")

    val counterFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[COUNTER_KEY] ?: 0
    }

    suspend fun incrementCounter() {
        context.dataStore.edit { preferences ->
            val current = preferences[COUNTER_KEY] ?: 0
            preferences[COUNTER_KEY] = current + 1
        }
    }
}