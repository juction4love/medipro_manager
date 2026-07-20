package com.medipro.manager.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore("app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val seedImportedKey = booleanPreferencesKey("seed_imported")
    private val themeKey = stringPreferencesKey("theme")
    private val languageKey = stringPreferencesKey("language")

    val isOnboardingComplete: Flow<Boolean> = context.appDataStore.data.map {
        it[onboardingCompleteKey] ?: false
    }

    val theme: Flow<String> = context.appDataStore.data.map {
        it[themeKey] ?: "SYSTEM"
    }

    val language: Flow<String> = context.appDataStore.data.map {
        it[languageKey] ?: "ne"
    }

    suspend fun setOnboardingComplete() {
        context.appDataStore.edit { it[onboardingCompleteKey] = true }
    }

    suspend fun setTheme(theme: String) {
        context.appDataStore.edit { it[themeKey] = theme }
    }

    suspend fun setLanguage(language: String) {
        context.appDataStore.edit { it[languageKey] = language }
    }

    suspend fun isSeedImported(): Boolean =
        context.appDataStore.data.map { it[seedImportedKey] ?: false }.first()

    suspend fun setSeedImported() {
        context.appDataStore.edit { it[seedImportedKey] = true }
    }
}
