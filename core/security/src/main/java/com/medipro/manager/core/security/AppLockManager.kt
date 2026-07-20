package com.medipro.manager.core.security

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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore("security_prefs")

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val lockEnabledKey = booleanPreferencesKey("lock_enabled")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")

    val isLockEnabled: Flow<Boolean> = context.securityDataStore.data.map { it[lockEnabledKey] ?: false }
    val isBiometricEnabled: Flow<Boolean> = context.securityDataStore.data.map { it[biometricEnabledKey] ?: false }
    val hasPin: Flow<Boolean> = context.securityDataStore.data.map { it[pinHashKey] != null }

    suspend fun setPin(pin: String) {
        context.securityDataStore.edit { prefs ->
            prefs[pinHashKey] = hashPin(pin)
            prefs[lockEnabledKey] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.securityDataStore.data.first()[pinHashKey]
        return stored == hashPin(pin)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[biometricEnabledKey] = enabled }
    }

    suspend fun disableLock() {
        context.securityDataStore.edit {
            it.remove(pinHashKey)
            it[lockEnabledKey] = false
            it[biometricEnabledKey] = false
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
