package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.security.LicenseCacheManager
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.data.remote.CreateLicenseRequest
import com.medipro.manager.data.remote.LicenseApiClient
import com.medipro.manager.data.remote.VerifyLicenseRequest
import com.medipro.manager.data.remote.toDomain
import com.medipro.manager.domain.model.License
import com.medipro.manager.domain.model.LicenseStatus
import com.medipro.manager.domain.licensing.LicenseAccessState
import com.medipro.manager.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepositoryImpl @Inject constructor(
    private val licenseDao: LicenseDao,
    private val licenseApiClient: LicenseApiClient,
    private val licenseCacheManager: LicenseCacheManager,
    private val authSessionProvider: com.medipro.manager.data.auth.FirebaseAuthSessionProvider,
) : LicenseRepository {

    override fun observeLicense(): Flow<License?> =
        licenseDao.observe().map { it?.toDomain() }

    override suspend fun getLicense(): License? =
        licenseCacheManager.get() ?: licenseDao.get()?.toDomain()

    override suspend fun getAccessState(deviceId: String): LicenseAccessState {
        val license = getLicense() ?: return LicenseAccessState.NO_LICENSE
        if (license.isExpired || license.status == LicenseStatus.EXPIRED) {
            return LicenseAccessState.EXPIRED
        }
        if (!validateLocal(license, deviceId)) {
            return LicenseAccessState.NO_LICENSE
        }
        return if (license.daysRemaining <= EXPIRING_THRESHOLD_DAYS) {
            LicenseAccessState.EXPIRING_SOON
        } else {
            LicenseAccessState.VALID
        }
    }

    override suspend fun saveLicense(license: License) {
        licenseDao.insert(license.toEntity())
        licenseCacheManager.save(license)
    }

    override suspend fun isLicenseValid(deviceId: String): Boolean {
        val license = getLicense() ?: return false
        return validateLocal(license, deviceId)
    }

    override suspend fun isLicenseExpired(): Boolean {
        val license = getLicense() ?: return true
        return license.isExpired || license.status == LicenseStatus.EXPIRED
    }

    override suspend fun activateFromServer(
        firebaseUid: String,
        idToken: String,
        mobileNumber: String,
        deviceId: String,
        pharmacyName: String,
        ownerName: String,
    ): Result<License> {
        return licenseApiClient.createLicense(
            CreateLicenseRequest(
                idToken = idToken,
                firebaseUid = firebaseUid,
                mobileNumber = mobileNumber,
                deviceId = deviceId,
                pharmacyName = pharmacyName,
                ownerName = ownerName,
            )
        ).mapCatching { response ->
            val license = response.toDomain()
            if (license.deviceId != deviceId) error("Device binding failed")
            saveLicense(license)
            license
        }
    }

    override suspend fun syncWithServer(deviceId: String): Result<License> {
        val local = getLicense() ?: return Result.failure(IllegalStateException("No local license"))
        if (!validateLocal(local, deviceId)) {
            return Result.failure(IllegalStateException("Local license invalid for this device"))
        }
        return licenseApiClient.verifyLicense(
            VerifyLicenseRequest(
                idToken = authSessionProvider.getFreshIdToken().orEmpty(),
                licenseId = local.licenseId,
                deviceId = deviceId,
            )
        ).mapCatching { response ->
            val updated = response.toDomain().copy(
                pharmacyName = local.pharmacyName.ifBlank { response.pharmacyName },
                ownerName = local.ownerName.ifBlank { response.ownerName },
                lastVerifiedAt = System.currentTimeMillis(),
            )
            saveLicense(updated)
            updated
        }.recoverCatching {
            // Offline grace: keep using local license if not expired
            if (!local.isExpired) local else throw it
        }
    }

    override suspend fun shouldSyncWithServer(): Boolean {
        val license = getLicense() ?: return false
        val last = license.lastVerifiedAt ?: license.activatedAt
        return System.currentTimeMillis() - last > SYNC_INTERVAL_MS
    }

    private fun validateLocal(license: License, deviceId: String): Boolean {
        if (!license.isActive) return false
        if (license.status != LicenseStatus.ACTIVE) return false
        if (license.isExpired) return false
        if (license.deviceId != deviceId) return false
        return true
    }

    companion object {
        /** Re-verify with server every 14 days when online. */
        const val SYNC_INTERVAL_MS = 14L * 24 * 60 * 60 * 1000
        const val EXPIRING_THRESHOLD_DAYS = 30
    }
}
