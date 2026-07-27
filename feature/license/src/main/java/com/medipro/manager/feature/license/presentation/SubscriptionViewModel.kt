package com.medipro.manager.feature.license.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.licensing.LicenseAccessState
import com.medipro.manager.domain.model.LicensePlan
import com.medipro.manager.domain.usecase.license.GetLicenseUseCase
import com.medipro.manager.domain.usecase.license.ObserveLicenseAccessStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SubscriptionUiState(
    val currentPlan: String = LicensePlan.FREE,
    val accessState: LicenseAccessState = LicenseAccessState.NO_LICENSE,
    val validUntil: String? = null,
    val daysRemaining: Int? = null,
    val appVersion: String = "1.1.34",
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    observeLicenseAccessState: ObserveLicenseAccessStateUseCase,
    private val getLicense: GetLicenseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val license = getLicense()
            _state.update {
                it.copy(
                    currentPlan = license?.plan ?: LicensePlan.FREE,
                    validUntil = license?.expiresAt?.let(::formatDate),
                    daysRemaining = license?.daysRemaining,
                )
            }
        }
        viewModelScope.launch {
            observeLicenseAccessState().collect { accessState ->
                _state.update { it.copy(accessState = accessState) }
            }
        }
    }

    fun currentPlanLabel(): String = when (_state.value.accessState) {
        LicenseAccessState.VALID,
        LicenseAccessState.EXPIRING_SOON,
        -> "PRO"
        LicenseAccessState.EXPIRED -> "EXPIRED"
        LicenseAccessState.NO_LICENSE -> LicensePlan.FREE
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
}
