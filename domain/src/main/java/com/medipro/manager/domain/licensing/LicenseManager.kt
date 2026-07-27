package com.medipro.manager.domain.licensing

import kotlinx.coroutines.flow.StateFlow

interface LicenseManager {
    val accessState: StateFlow<LicenseAccessState>

    /** Silent background refresh on app start or resume. */
    suspend fun refreshInBackground(deviceId: String)

    fun getState(): LicenseAccessState

    fun canAccessPremiumFeature(feature: PremiumFeature): Boolean

    suspend fun checkPremiumFeature(deviceId: String, feature: PremiumFeature): Boolean
}
