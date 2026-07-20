package com.medipro.manager.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.repository.BackupRepository
import com.medipro.manager.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class SettingsUiState(
    val pharmacyName: String = "",
    val pharmacyPhone: String = "",
    val pharmacyAddress: String = "",
    val ocrFeedbackOptIn: Boolean = false,
    val lastBackupLabel: String = "Never",
    val daysSinceBackup: Int? = null,
    val backupWarning: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update {
                    it.copy(
                        pharmacyName = settings.pharmacyName,
                        pharmacyPhone = settings.pharmacyPhone,
                        pharmacyAddress = settings.pharmacyAddress,
                        ocrFeedbackOptIn = settings.ocrFeedbackOptIn,
                    )
                }
            }
        }
        viewModelScope.launch {
            backupRepository.observeBackupHistory().collect { history ->
                val latest = history.maxByOrNull { it.createdAt }
                val daysSince = latest?.let {
                    max(0, ((System.currentTimeMillis() - it.createdAt) / DAY_MS).toInt())
                }
                _state.update {
                    it.copy(
                        lastBackupLabel = latest?.let { record ->
                            when (daysSince) {
                                0 -> "Today"
                                1 -> "Yesterday"
                                else -> "$daysSince days ago"
                            }
                        } ?: "Never",
                        daysSinceBackup = daysSince,
                        backupWarning = daysSince == null || daysSince >= BACKUP_WARN_DAYS,
                    )
                }
            }
        }
    }

    fun onPharmacyNameChange(value: String) {
        _state.update { it.copy(pharmacyName = value) }
        schedulePersist()
    }

    fun onPharmacyPhoneChange(value: String) {
        _state.update { it.copy(pharmacyPhone = value) }
        schedulePersist()
    }

    fun onPharmacyAddressChange(value: String) {
        _state.update { it.copy(pharmacyAddress = value) }
        schedulePersist()
    }

    fun onOcrFeedbackOptInChange(enabled: Boolean) {
        _state.update { it.copy(ocrFeedbackOptIn = enabled) }
        schedulePersist()
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            persistNow()
        }
    }

    private suspend fun persistNow() {
        val current = _state.value
        val existing = settingsRepository.getSettings()
        settingsRepository.updateSettings(
            existing.copy(
                pharmacyName = current.pharmacyName,
                pharmacyPhone = current.pharmacyPhone,
                pharmacyAddress = current.pharmacyAddress,
                ocrFeedbackOptIn = current.ocrFeedbackOptIn,
            )
        )
    }

    companion object {
        private const val PERSIST_DEBOUNCE_MS = 400L
        private const val BACKUP_WARN_DAYS = 3
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
