package com.medipro.manager.core.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupPassphraseStore by preferencesDataStore("backup_passphrase")

/**
 * Stores auto-backup passphrase encrypted with [KeystoreManager] (device-bound).
 */
@Singleton
class BackupPassphraseStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager,
) {
    suspend fun savePassphrase(passphrase: CharArray) {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val payload = keystoreManager.encrypt(String(passphrase).toByteArray(Charsets.UTF_8))
        context.backupPassphraseStore.edit { prefs ->
            prefs[KEY_CIPHER] = payload.ciphertext
            prefs[KEY_IV] = payload.iv
        }
    }

    suspend fun getPassphrase(): CharArray? {
        val prefs = context.backupPassphraseStore.data.first()
        val cipher = prefs[KEY_CIPHER] ?: return null
        val iv = prefs[KEY_IV] ?: return null
        return runCatching {
            String(
                keystoreManager.decrypt(KeystoreManager.EncryptedData(cipher, iv)),
                Charsets.UTF_8,
            ).toCharArray()
        }.getOrNull()
    }

    suspend fun hasPassphrase(): Boolean =
        context.backupPassphraseStore.data.map { it[KEY_CIPHER] != null }.first()

    suspend fun clear() {
        context.backupPassphraseStore.edit { it.clear() }
    }

    companion object {
        private val KEY_CIPHER = stringPreferencesKey("cipher")
        private val KEY_IV = stringPreferencesKey("iv")
    }
}
