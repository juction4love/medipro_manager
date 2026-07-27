package com.medipro.manager.feature.license.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.usecase.license.ActivateLicenseWithOtpUseCase
import com.medipro.manager.domain.usecase.license.RefreshLicenseInBackgroundUseCase
import com.medipro.manager.domain.usecase.license.VerifyLicenseUseCase
import com.medipro.manager.feature.license.auth.PhoneAuthErrorMapper
import com.medipro.manager.feature.license.auth.PhoneAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LicenseStep {
    MOBILE, OTP, DETAILS, EXPIRED
}

data class LicenseUiState(
    val step: LicenseStep = LicenseStep.MOBILE,
    val mobileNumber: String = "",
    val otp: String = "",
    val pharmacyName: String = "",
    val ownerName: String = "",
    val verificationId: String? = null,
    val firebaseUid: String? = null,
    val idToken: String? = null,
    val otpAutoVerified: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isActivated: Boolean = false,
    val devMode: Boolean = false,
    val licenseExpiryLabel: String? = null,
)

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val phoneAuthHelper: PhoneAuthHelper,
    private val activateLicenseWithOtpUseCase: ActivateLicenseWithOtpUseCase,
    private val verifyLicenseUseCase: VerifyLicenseUseCase,
    private val refreshLicenseInBackground: RefreshLicenseInBackgroundUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LicenseUiState(devMode = !phoneAuthHelper.isFirebaseAvailable()))
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    fun onMobileChange(value: String) {
        _state.update { it.copy(mobileNumber = value.filter { c -> c.isDigit() }.take(10), errorMessage = null) }
    }

    fun onOtpChange(value: String) {
        _state.update { it.copy(otp = value.filter { it.isDigit() }.take(6), errorMessage = null) }
    }

    fun onPharmacyNameChange(value: String) {
        _state.update { it.copy(pharmacyName = value, errorMessage = null) }
    }

    fun onOwnerNameChange(value: String) {
        _state.update { it.copy(ownerName = value, errorMessage = null) }
    }

    fun showExpired() {
        _state.update { it.copy(step = LicenseStep.EXPIRED, errorMessage = null) }
    }

    fun restartActivation() {
        phoneAuthHelper.clearSessionState()
        _state.update {
            it.copy(
                step = LicenseStep.MOBILE,
                mobileNumber = "",
                otp = "",
                verificationId = null,
                firebaseUid = null,
                idToken = null,
                otpAutoVerified = false,
                errorMessage = null,
                isActivated = false,
            )
        }
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
        val verificationId = current.verificationId ?: return
        if (current.otpAutoVerified) {
            advanceToDetails(current.firebaseUid, current.idToken)
            return
        }
        if (current.otp.length < 4 && verificationId != PhoneAuthHelper.AUTO_VERIFIED_ID) {
            _state.update { it.copy(errorMessage = "Enter OTP code") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            phoneAuthHelper.verifyOtp(verificationId, current.otp)
                .onSuccess { auth -> advanceToDetails(auth.firebaseUid, auth.idToken) }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = PhoneAuthErrorMapper.message(e))
                    }
                }
        }
    }

    fun activateLicense(deviceId: String) {
        val current = _state.value
        if (current.pharmacyName.isBlank()) {
            _state.update { it.copy(errorMessage = "Pharmacy name is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val authResult = resolveAuthForActivation(current)
                .getOrElse { error ->
                    _state.update { s -> s.copy(isLoading = false, errorMessage = PhoneAuthErrorMapper.message(error)) }
                    return@launch
                }
            activateLicenseWithOtpUseCase(
                firebaseUid = authResult.firebaseUid,
                idToken = authResult.idToken,
                mobileNumber = current.mobileNumber,
                deviceId = deviceId,
                pharmacyName = current.pharmacyName.trim(),
                ownerName = current.ownerName.trim(),
            ).onSuccess { license ->
                refreshLicenseInBackground(deviceId)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isActivated = true,
                        firebaseUid = authResult.firebaseUid,
                        idToken = authResult.idToken,
                        licenseExpiryLabel = formatExpiry(license.expiresAt),
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Activation failed")
                }
            }
        }
    }

    fun checkExistingLicense(deviceId: String) {
        viewModelScope.launch {
            if (verifyLicenseUseCase(deviceId)) {
                _state.update { it.copy(isActivated = true) }
            }
        }
    }

    private suspend fun resolveAuthForActivation(current: LicenseUiState): Result<PhoneAuthHelper.AuthResult> {
        if (current.devMode && !current.firebaseUid.isNullOrBlank() && !current.idToken.isNullOrBlank()) {
            return Result.success(PhoneAuthHelper.AuthResult(current.firebaseUid, current.idToken))
        }
        return phoneAuthHelper.getCurrentAuthResult()
    }

    private fun handleOtpSent(result: PhoneAuthHelper.OtpSendResult, preserveOtp: Boolean = false) {
        if (result.autoVerified) {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                phoneAuthHelper.getCurrentAuthResult()
                    .onSuccess { auth ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                verificationId = PhoneAuthHelper.AUTO_VERIFIED_ID,
                                otpAutoVerified = true,
                                firebaseUid = auth.firebaseUid,
                                idToken = auth.idToken,
                                step = LicenseStep.DETAILS,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { e ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                verificationId = PhoneAuthHelper.AUTO_VERIFIED_ID,
                                otpAutoVerified = true,
                                step = LicenseStep.OTP,
                                errorMessage = PhoneAuthErrorMapper.message(e),
                            )
                        }
                    }
            }
            return
        }
        _state.update {
            it.copy(
                isLoading = false,
                verificationId = result.verificationId,
                otpAutoVerified = false,
                otp = if (preserveOtp) it.otp else "",
                step = LicenseStep.OTP,
                errorMessage = if (it.devMode) "Dev mode: enter any 6-digit OTP" else null,
            )
        }
    }

    private fun advanceToDetails(firebaseUid: String?, idToken: String?) {
        _state.update {
            it.copy(
                isLoading = false,
                step = LicenseStep.DETAILS,
                firebaseUid = firebaseUid,
                idToken = idToken,
                errorMessage = null,
            )
        }
    }

    private fun formatExpiry(expiresAt: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = expiresAt }
        return "${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
    }
}
