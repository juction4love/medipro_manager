package com.medipro.manager.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.ThermalPrintResult
import com.medipro.manager.domain.usecase.printer.GetPairedBluetoothDevicesUseCase
import com.medipro.manager.domain.usecase.printer.ObservePrinterSettingsUseCase
import com.medipro.manager.domain.usecase.printer.TestThermalPrintUseCase
import com.medipro.manager.domain.usecase.printer.UpdatePrinterSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrinterSettingsState(
    val selectedProfile: PrinterProfile = PrinterProfile.COUNTER,
    val settings: PrinterSettings = PrinterSettings(),
    val pairedDevices: List<Pair<String, String>> = emptyList(),
    val isTesting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val observePrinterSettings: ObservePrinterSettingsUseCase,
    private val updateSettings: UpdatePrinterSettingsUseCase,
    private val getPairedDevices: GetPairedBluetoothDevicesUseCase,
    private val testPrint: TestThermalPrintUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PrinterSettingsState())
    val state: StateFlow<PrinterSettingsState> = _state.asStateFlow()
    private var persistJob: Job? = null
    private var observeJob: Job? = null

    init {
        selectProfile(PrinterProfile.COUNTER)
        refreshDevices()
    }

    fun selectProfile(profile: PrinterProfile) {
        observeJob?.cancel()
        _state.update { it.copy(selectedProfile = profile) }
        observeJob = viewModelScope.launch {
            observePrinterSettings(profile).collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            runCatching { getPairedDevices() }
                .onSuccess { devices ->
                    _state.update {
                        it.copy(pairedDevices = devices.map { d -> d.name to d.address })
                    }
                }
        }
    }

    private fun updateLocal(block: PrinterSettings.() -> PrinterSettings) {
        _state.update { it.copy(settings = it.settings.block()) }
    }

    private fun persist(settings: PrinterSettings, debounce: Boolean = false) {
        val profile = _state.value.selectedProfile
        if (debounce) {
            persistJob?.cancel()
            persistJob = viewModelScope.launch {
                delay(PERSIST_DEBOUNCE_MS)
                updateSettings(profile, settings)
            }
        } else {
            persistJob?.cancel()
            viewModelScope.launch { updateSettings(profile, settings) }
        }
    }

    fun onPrinterNameChange(value: String) {
        val updated = _state.value.settings.copy(printerName = value)
        updateLocal { copy(printerName = value) }
        persist(updated, debounce = true)
    }

    fun onMacAddressChange(value: String) {
        val updated = _state.value.settings.copy(macAddress = value.uppercase())
        updateLocal { copy(macAddress = value.uppercase()) }
        persist(updated, debounce = true)
    }

    fun onPaperWidthChange(mm: Int) {
        val updated = _state.value.settings.copy(
            paperWidthMm = mm,
            charsPerLine = when (mm) {
                80 -> 48
                58 -> 32
                else -> _state.value.settings.charsPerLine
            },
        )
        updateLocal { updated }
        persist(updated)
    }

    fun onCharsPerLineChange(chars: Int) {
        val updated = _state.value.settings.copy(charsPerLine = chars)
        updateLocal { copy(charsPerLine = chars) }
        persist(updated)
    }

    fun onAutoConnectChange(enabled: Boolean) {
        val updated = _state.value.settings.copy(autoConnect = enabled)
        updateLocal { copy(autoConnect = enabled) }
        persist(updated)
    }

    fun onAutoCutChange(enabled: Boolean) {
        val updated = _state.value.settings.copy(autoCut = enabled)
        updateLocal { copy(autoCut = enabled) }
        persist(updated)
    }

    fun onOpenDrawerChange(enabled: Boolean) {
        val updated = _state.value.settings.copy(openCashDrawer = enabled)
        updateLocal { copy(openCashDrawer = enabled) }
        persist(updated)
    }

    fun onPrintLogoChange(enabled: Boolean) {
        val updated = _state.value.settings.copy(printLogo = enabled)
        updateLocal { copy(printLogo = enabled) }
        persist(updated)
    }

    fun onDuplicateCopyChange(enabled: Boolean) {
        val updated = _state.value.settings.copy(printDuplicateCopy = enabled)
        updateLocal { copy(printDuplicateCopy = enabled) }
        persist(updated)
    }

    fun selectDevice(name: String, address: String) {
        val updated = _state.value.settings.copy(printerName = name, macAddress = address)
        updateLocal { copy(printerName = name, macAddress = address) }
        persist(updated)
    }

    fun runTestPrint() {
        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, message = null) }
            when (val result = testPrint(_state.value.selectedProfile)) {
                is ThermalPrintResult.Success ->
                    _state.update {
                        it.copy(
                            isTesting = false,
                            message = "${_state.value.selectedProfile.label}: test print sent",
                            isError = false,
                        )
                    }
                is ThermalPrintResult.Failure ->
                    _state.update {
                        it.copy(isTesting = false, message = result.message, isError = true)
                    }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    companion object {
        private const val PERSIST_DEBOUNCE_MS = 400L
    }
}
