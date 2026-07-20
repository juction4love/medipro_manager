package com.medipro.manager.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.medipro.manager.domain.model.License
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.licenseStore: DataStore<Preferences> by preferencesDataStore("license_cache")

/**
 * Encrypted local license cache for offline validation.
 * Room license row is also kept; this cache holds the server-signed payload copy.
 */
@Singleton
class LicenseCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager,
) {
    private val cacheKey = stringPreferencesKey("encrypted_license")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(license: License) {
        val payload = json.encodeToString(license.toCache())
        val encrypted = keystoreManager.encrypt(payload.toByteArray(Charsets.UTF_8))
        val blob = "${encrypted.iv}:${encrypted.ciphertext}"
        context.licenseStore.edit { it[cacheKey] = blob }
    }

    suspend fun get(): License? {
        val blob = context.licenseStore.data.map { it[cacheKey] }.first() ?: return null
        return runCatching {
            val parts = blob.split(":", limit = 2)
            if (parts.size != 2) return null
            val decrypted = keystoreManager.decrypt(KeystoreManager.EncryptedData(parts[1], parts[0]))
            json.decodeFromString<LicenseCacheDto>(String(decrypted, Charsets.UTF_8)).toDomain()
        }.getOrNull()
    }

    suspend fun clear() {
        context.licenseStore.edit { it.remove(cacheKey) }
    }

    @kotlinx.serialization.Serializable
    private data class LicenseCacheDto(
        val licenseId: String,
        val licenseKey: String,
        val mobileNumber: String,
        val pharmacyName: String,
        val ownerName: String,
        val deviceId: String,
        val plan: String,
        val status: String,
        val activatedAt: Long,
        val expiresAt: Long,
        val lastVerifiedAt: Long? = null,
        val isActive: Boolean = true,
    ) {
        fun toDomain() = License(
            licenseId = licenseId,
            licenseKey = licenseKey,
            mobileNumber = mobileNumber,
            pharmacyName = pharmacyName,
            ownerName = ownerName,
            deviceId = deviceId,
            plan = plan,
            status = status,
            activatedAt = activatedAt,
            expiresAt = expiresAt,
            lastVerifiedAt = lastVerifiedAt,
            isActive = isActive,
        )
    }

    private fun License.toCache() = LicenseCacheDto(
        licenseId = licenseId,
        licenseKey = licenseKey,
        mobileNumber = mobileNumber,
        pharmacyName = pharmacyName,
        ownerName = ownerName,
        deviceId = deviceId,
        plan = plan,
        status = status,
        activatedAt = activatedAt,
        expiresAt = expiresAt,
        lastVerifiedAt = lastVerifiedAt,
        isActive = isActive,
    )
}
