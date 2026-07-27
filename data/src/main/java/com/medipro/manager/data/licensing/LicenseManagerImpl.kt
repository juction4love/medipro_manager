package com.medipro.manager.data.licensing

import com.medipro.manager.domain.licensing.LicenseAccessState
import com.medipro.manager.domain.licensing.LicenseManager
import com.medipro.manager.domain.licensing.LicenseEnvironment
import com.medipro.manager.domain.licensing.PremiumFeature
import com.medipro.manager.domain.repository.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseManagerImpl @Inject constructor(
    private val repository: LicenseRepository,
    private val licenseEnvironment: LicenseEnvironment,
) : LicenseManager {

    private val _accessState = MutableStateFlow(LicenseAccessState.NO_LICENSE)
    override val accessState: StateFlow<LicenseAccessState> = _accessState.asStateFlow()

    override suspend fun refreshInBackground(deviceId: String) {
        if (licenseEnvironment.useDevLicensing) {
            _accessState.value = LicenseAccessState.VALID
            return
        }
        if (repository.shouldSyncWithServer()) {
            runCatching { repository.syncWithServer(deviceId) }
        }
        _accessState.value = repository.getAccessState(deviceId)
    }

    override fun getState(): LicenseAccessState = _accessState.value

    override fun canAccessPremiumFeature(feature: PremiumFeature): Boolean {
        if (licenseEnvironment.useDevLicensing) return true
        return when (_accessState.value) {
            LicenseAccessState.VALID,
            LicenseAccessState.EXPIRING_SOON,
            -> true
            LicenseAccessState.EXPIRED,
            LicenseAccessState.NO_LICENSE,
            -> false
        }
    }

    override suspend fun checkPremiumFeature(deviceId: String, feature: PremiumFeature): Boolean {
        refreshInBackground(deviceId)
        return canAccessPremiumFeature(feature)
    }
}
