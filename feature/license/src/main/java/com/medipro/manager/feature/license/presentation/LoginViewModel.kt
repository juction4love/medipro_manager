package com.medipro.manager.feature.license.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.feature.license.auth.PhoneAuthErrorMapper
import com.medipro.manager.feature.license.auth.PhoneAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginStep {
    MOBILE,
    OTP,
}

data class LoginUiState(
    val step: LoginStep = LoginStep.MOBILE,
    val mobileNumber: String = "",
    val otp: String = "",
    val verificationId: String? = null,
    val otpAutoVerified: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val devMode: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val phoneAuthHelper: PhoneAuthHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(devMode = !phoneAuthHelper.isFirebaseAvailable())
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        if (_state.value.devMode) {
            _state.update { it.copy(isLoggedIn = true) }
        } else if (phoneAuthHelper.isSignedIn()) {
            _state.update { it.copy(isLoggedIn = true) }
        }
    }

    fun onMobileChange(value: String) {
        _state.update { it.copy(mobileNumber = value.filter { it.isDigit() }.take(10), errorMessage = null) }
    }

    fun onOtpChange(value: String) {
        _state.update { it.copy(otp = value.filter { it.isDigit() }.take(6), errorMessage = null) }
    }

    fun sendOtp(activity: Activity) {
        val mobile = _state.value.mobileNumber
        if (mobile.length < 10) {
            _state.update { it.copy(errorMessage = "Enter valid 10-digit mobile number") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            phoneAuthHelper.sendOtp(mobile, activity)
                .onSuccess { result -> handleOtpSent(result) }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = PhoneAuthErrorMapper.message(e))
                    }
                }
        }
    }

    fun resendOtp(activity: Activity) {
        val mobile = _state.value.mobileNumber
        if (mobile.length < 10) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            phoneAuthHelper.resendOtp(mobile, activity)
                .onSuccess { result ->
                    handleOtpSent(result, preserveOtp = true)
                    _state.update { it.copy(errorMessage = "OTP sent again") }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = PhoneAuthErrorMapper.message(e))
                    }
                }
        }
    }

    fun verifyOtpAndContinue() {
        val current = _state.value
        if (current.otpAutoVerified || current.devMode) {
            completeLogin()
            return
        }
        val verificationId = current.verificationId ?: return
        if (current.otp.length < 4 && verificationId != PhoneAuthHelper.AUTO_VERIFIED_ID) {
            _state.update { it.copy(errorMessage = "Enter OTP code") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            phoneAuthHelper.verifyOtp(verificationId, current.otp)
                .onSuccess { completeLogin() }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = PhoneAuthErrorMapper.message(e))
                    }
                }
        }
    }

    fun continueInDevMode() {
        if (!_state.value.devMode) return
        completeLogin()
    }

    private fun completeLogin() {
        _state.update { it.copy(isLoading = false, isLoggedIn = true, errorMessage = null) }
    }

    private fun handleOtpSent(result: PhoneAuthHelper.OtpSendResult, preserveOtp: Boolean = false) {
        if (result.autoVerified) {
            completeLogin()
            return
        }
        _state.update {
            it.copy(
                isLoading = false,
                verificationId = result.verificationId,
                otpAutoVerified = false,
                otp = if (preserveOtp) it.otp else "",
                step = LoginStep.OTP,
                errorMessage = if (it.devMode) "Dev mode: enter any 6-digit OTP" else null,
            )
        }
    }
}
