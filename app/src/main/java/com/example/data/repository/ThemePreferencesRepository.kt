package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "styleora_prefs")

class ThemePreferencesRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val rememberMeKey = booleanPreferencesKey("remember_me_enabled")
    private val rememberedEmailKey = stringPreferencesKey("remembered_email")

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    val rememberMeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[rememberMeKey] ?: false
    }

    val rememberedEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[rememberedEmailKey] ?: ""
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }

    suspend fun setRememberedLogin(rememberMe: Boolean, email: String) {
        context.dataStore.edit { prefs ->
            prefs[rememberMeKey] = rememberMe
            prefs[rememberedEmailKey] = if (rememberMe) email.trim() else ""
        }
    }
}
