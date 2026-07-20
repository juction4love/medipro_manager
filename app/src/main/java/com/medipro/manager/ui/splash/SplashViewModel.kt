package com.medipro.manager.ui.splash

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.core.security.AppLockManager
import com.medipro.manager.domain.usecase.license.IsLicenseExpiredUseCase
import com.medipro.manager.domain.usecase.license.SyncLicenseUseCase
import com.medipro.manager.domain.usecase.license.VerifyLicenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verifyLicense: VerifyLicenseUseCase,
    private val isLicenseExpired: IsLicenseExpiredUseCase,
    private val syncLicense: SyncLicenseUseCase,
    private val appLockManager: AppLockManager,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.LOADING)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    init {
        viewModelScope.launch {
            val hasValidLicense = verifyLicense(deviceId)
            if (!hasValidLicense) {
                _destination.value = if (isLicenseExpired()) {
                    SplashDestination.LICENSE_EXPIRED
                } else {
                    SplashDestination.LICENSE
                }
                return@launch
            }

            // Background sync when online (graceful offline fallback inside repository)
            syncLicense(deviceId)

            val lockEnabled = appLockManager.isLockEnabled.first()
            _destination.value = if (lockEnabled) {
                SplashDestination.APP_LOCK
            } else {
                SplashDestination.DASHBOARD
            }
        }
    }
}

enum class SplashDestination {
    LOADING, LICENSE, LICENSE_EXPIRED, APP_LOCK, DASHBOARD
}
