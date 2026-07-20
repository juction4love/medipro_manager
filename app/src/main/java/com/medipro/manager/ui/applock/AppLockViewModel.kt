package com.medipro.manager.ui.applock

import androidx.lifecycle.ViewModel
import com.medipro.manager.core.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {

    suspend fun verifyPin(pin: String): Boolean = appLockManager.verifyPin(pin)

    suspend fun setPin(pin: String) = appLockManager.setPin(pin)
}
