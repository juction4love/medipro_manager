package com.medipro.manager.feature.backup.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.core.worker.AutoBackupScheduler
import com.medipro.manager.core.security.BackupPassphraseStore
import com.medipro.manager.domain.model.BackupRecord
import com.medipro.manager.domain.repository.BackupRepository
import com.medipro.manager.domain.repository.SettingsRepository
import com.medipro.manager.feature.backup.util.AppRestarter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val autoBackupEnabled: Boolean = false,
    val latestBackup: BackupRecord? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val restoreRequested: Boolean = false,
) {
    val lastBackupLabel: String
        get() = latestBackup?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.createdAt))
        } ?: "Never"

    val backupSizeLabel: String
        get() = latestBackup?.let {
            val mb = it.fileSize / (1024.0 * 1024.0)
            String.format(Locale.US, "%.1f MB", mb)
        } ?: "—"
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val passphraseStore: BackupPassphraseStore,
    private val autoBackupScheduler: AutoBackupScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.observeBackupHistory().collect { history ->
                _state.update { it.copy(latestBackup = history.firstOrNull()) }
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _state.update { it.copy(autoBackupEnabled = settings.autoBackupEnabled) }
        }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null) }

    fun onAutoBackupChange(enabled: Boolean) {
        _state.update { it.copy(autoBackupEnabled = enabled) }
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            settingsRepository.updateSettings(settings.copy(autoBackupEnabled = enabled))
            if (enabled) {
                autoBackupScheduler.scheduleWeekly()
            } else {
                autoBackupScheduler.cancel()
            }
        }
    }

    fun savePassphraseForAutoBackup() {
        val password = _state.value.password
        if (password.length < 8) {
            _state.update { it.copy(error = "Use at least 8 characters for backup password") }
            return
        }
        if (password != _state.value.confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            passphraseStore.savePassphrase(password.toCharArray())
            _state.update { it.copy(message = "Auto-backup password saved on this device", error = null) }
        }
    }

    fun createBackup() {
        val password = _state.value.password.toCharArray()
        if (password.isEmpty()) {
            _state.update { it.copy(error = "Enter backup password") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null, message = null) }
            try {
                backupRepository.createBackup(password)
                    .fold(
                        onSuccess = { record ->
                            _state.update {
                                it.copy(
                                    isBusy = false,
                                    message = "Backup saved: ${record.fileName}",
                                    latestBackup = record,
                                )
                            }
                        },
                        onFailure = { err ->
                            _state.update { it.copy(isBusy = false, error = err.message ?: "Backup failed") }
                        },
                    )
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        val password = _state.value.password.toCharArray()
        if (password.isEmpty()) {
            _state.update { it.copy(error = "Enter backup password to restore") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, error = null, message = null) }
            try {
                runCatching {
                    val tempFile = java.io.File(context.cacheDir, "restore_input.medipro")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Cannot read backup file")
                    backupRepository.restoreBackup(tempFile.absolutePath, password)
                        .getOrThrow()
                    tempFile.delete()
                }.fold(
                    onSuccess = {
                        _state.update { it.copy(isBusy = false, message = "Restore complete — restarting app…") }
                        AppRestarter.restart(context)
                    },
                    onFailure = { err ->
                        _state.update { it.copy(isBusy = false, error = err.message ?: "Restore failed") }
                    },
                )
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(message = null, error = null) }
}
