package com.medipro.manager.domain.usecase.license

import com.medipro.manager.domain.licensing.LicenseAccessState
import com.medipro.manager.domain.licensing.LicenseManager
import com.medipro.manager.domain.licensing.PremiumFeature
import com.medipro.manager.domain.model.License
import com.medipro.manager.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    operator fun invoke(): Flow<License?> = repository.observeLicense()
}

class GetLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(): License? = repository.getLicense()
}

class GetLicenseAccessStateUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(deviceId: String): LicenseAccessState =
        repository.getAccessState(deviceId)
}

class ObserveLicenseAccessStateUseCase @Inject constructor(
    private val licenseManager: LicenseManager,
) {
    operator fun invoke(): Flow<LicenseAccessState> =
        licenseManager.accessState
}

class CanAccessPremiumFeatureUseCase @Inject constructor(
    private val licenseManager: LicenseManager,
) {
    operator fun invoke(feature: PremiumFeature): Boolean =
        licenseManager.canAccessPremiumFeature(feature)

    suspend fun check(deviceId: String, feature: PremiumFeature): Boolean =
        licenseManager.checkPremiumFeature(deviceId, feature)
}

class RefreshLicenseInBackgroundUseCase @Inject constructor(
    private val licenseManager: LicenseManager,
) {
    suspend operator fun invoke(deviceId: String) = licenseManager.refreshInBackground(deviceId)
}

class VerifyLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(deviceId: String): Boolean = repository.isLicenseValid(deviceId)
}

class IsLicenseExpiredUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(): Boolean = repository.isLicenseExpired()
}

class ActivateLicenseWithOtpUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(
        firebaseUid: String,
        idToken: String,
        mobileNumber: String,
        deviceId: String,
        pharmacyName: String,
        ownerName: String,
    ): Result<License> = repository.activateFromServer(
        firebaseUid = firebaseUid,
        idToken = idToken,
        mobileNumber = mobileNumber,
        deviceId = deviceId,
        pharmacyName = pharmacyName,
        ownerName = ownerName,
    )
}

class SyncLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(deviceId: String): Result<License> {
        if (!repository.shouldSyncWithServer()) {
            val local = repository.getLicense()
            return if (local != null) Result.success(local)
            else Result.failure(IllegalStateException("No license"))
        }
        return repository.syncWithServer(deviceId)
    }
}
