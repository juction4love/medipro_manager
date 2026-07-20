package com.medipro.manager.domain.usecase.printer

import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.repository.PrinterRepository
import javax.inject.Inject

class ObservePrinterSettingsUseCase @Inject constructor(
    private val repository: PrinterRepository,
) {
    operator fun invoke(profile: PrinterProfile) = repository.observePrinterSettings(profile)
}

class UpdatePrinterSettingsUseCase @Inject constructor(
    private val repository: PrinterRepository,
) {
    suspend operator fun invoke(profile: PrinterProfile, settings: PrinterSettings) =
        repository.updatePrinterSettings(profile, settings)
}

class GetPairedBluetoothDevicesUseCase @Inject constructor(
    private val repository: PrinterRepository,
) {
    suspend operator fun invoke() = repository.getPairedDevices()
}

class TestThermalPrintUseCase @Inject constructor(
    private val repository: PrinterRepository,
) {
    suspend operator fun invoke(profile: PrinterProfile = PrinterProfile.COUNTER) =
        repository.testPrint(profile)
}
