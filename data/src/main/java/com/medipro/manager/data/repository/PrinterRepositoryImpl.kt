package com.medipro.manager.data.repository

import com.medipro.manager.core.datastore.PrinterPreferences
import com.medipro.manager.core.datastore.StoredPrinterProfile
import com.medipro.manager.core.datastore.StoredPrinterSettings
import com.medipro.manager.data.document.printing.BluetoothThermalPrinterAdapter
import com.medipro.manager.data.document.receipt.EscPosEncoder
import com.medipro.manager.data.document.receipt.ReceiptFormatter
import com.medipro.manager.domain.model.BluetoothDeviceInfo
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.ThermalPrintResult
import com.medipro.manager.domain.repository.PrinterRepository
import com.medipro.manager.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterRepositoryImpl @Inject constructor(
    private val printerPreferences: PrinterPreferences,
    private val bluetoothAdapter: BluetoothThermalPrinterAdapter,
    private val receiptFormatter: ReceiptFormatter,
    private val escPosEncoder: EscPosEncoder,
    private val settingsRepository: SettingsRepository,
) : PrinterRepository {

    override fun observePrinterSettings(profile: PrinterProfile): Flow<PrinterSettings> =
        printerPreferences.settings(profile.toStored()).map { it.toDomain() }

    override suspend fun getPrinterSettings(profile: PrinterProfile): PrinterSettings =
        printerPreferences.get(profile.toStored()).toDomain()

    override suspend fun updatePrinterSettings(profile: PrinterProfile, settings: PrinterSettings) {
        printerPreferences.save(profile.toStored(), settings.toStored())
    }

    override suspend fun getPairedDevices(): List<BluetoothDeviceInfo> =
        bluetoothAdapter.getPairedDevices().map { (name, address) ->
            BluetoothDeviceInfo(name = name, address = address)
        }

    override suspend fun testPrint(profile: PrinterProfile): ThermalPrintResult {
        val printerSettings = getPrinterSettings(profile)
        if (!printerSettings.isConfigured) {
            return ThermalPrintResult.Failure("Configure ${profile.label} MAC address first")
        }
        val pharmacy = settingsRepository.getSettings()
        val content = receiptFormatter.formatTestReceipt(pharmacy, printerSettings)
        val bytes = escPosEncoder.encode(content, printerSettings)
        return bluetoothAdapter.print(bytes, printerSettings)
    }

    override suspend fun printReceipt(bytes: ByteArray, profile: PrinterProfile): ThermalPrintResult {
        val printerSettings = getPrinterSettings(profile)
        return bluetoothAdapter.print(bytes, printerSettings)
    }

    override fun isBluetoothAvailable(): Boolean = bluetoothAdapter.isAvailable()

    private fun StoredPrinterSettings.toDomain() = PrinterSettings(
        printerName = printerName,
        macAddress = macAddress,
        paperWidthMm = paperWidthMm,
        charsPerLine = charsPerLine,
        autoConnect = autoConnect,
        autoCut = autoCut,
        openCashDrawer = openCashDrawer,
        printLogo = printLogo,
        printDuplicateCopy = printDuplicateCopy,
    )

    private fun PrinterSettings.toStored() = StoredPrinterSettings(
        printerName = printerName,
        macAddress = macAddress,
        paperWidthMm = paperWidthMm,
        charsPerLine = charsPerLine,
        autoConnect = autoConnect,
        autoCut = autoCut,
        openCashDrawer = openCashDrawer,
        printLogo = printLogo,
        printDuplicateCopy = printDuplicateCopy,
    )

    private fun PrinterProfile.toStored(): StoredPrinterProfile = when (this) {
        PrinterProfile.COUNTER -> StoredPrinterProfile.COUNTER
        PrinterProfile.OFFICE -> StoredPrinterProfile.OFFICE
    }
}
