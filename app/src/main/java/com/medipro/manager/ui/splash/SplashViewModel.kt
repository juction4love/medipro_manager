package com.medipro.manager.ui.splash

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.core.security.AppLockManager
import com.medipro.manager.domain.usecase.license.RefreshLicenseInBackgroundUseCase
import com.medipro.manager.feature.license.auth.PhoneAuthHelper
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
    private val refreshLicenseInBackground: RefreshLicenseInBackgroundUseCase,
    private val phoneAuthHelper: PhoneAuthHelper,
    private val appLockManager: AppLockManager,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.LOADING)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    init {
        viewModelScope.launch {
            // Silent background license check — no blocking popup.
            refreshLicenseInBackground(deviceId)

            val needsLogin = !phoneAuthHelper.isSignedIn()
            if (needsLogin) {
                _destination.value = SplashDestination.LOGIN
                return@launch
            }

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
    LOADING, LOGIN, APP_LOCK, DASHBOARD
}
