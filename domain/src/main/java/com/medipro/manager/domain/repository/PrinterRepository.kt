package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.BluetoothDeviceInfo
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.ThermalPrintResult
import kotlinx.coroutines.flow.Flow

interface PrinterRepository {
    fun observePrinterSettings(profile: PrinterProfile = PrinterProfile.COUNTER): Flow<PrinterSettings>
    suspend fun getPrinterSettings(profile: PrinterProfile = PrinterProfile.COUNTER): PrinterSettings
    suspend fun updatePrinterSettings(profile: PrinterProfile, settings: PrinterSettings)
    suspend fun getPairedDevices(): List<BluetoothDeviceInfo>
    suspend fun testPrint(profile: PrinterProfile = PrinterProfile.COUNTER): ThermalPrintResult
    suspend fun printReceipt(bytes: ByteArray, profile: PrinterProfile = PrinterProfile.COUNTER): ThermalPrintResult
    fun isBluetoothAvailable(): Boolean
}
