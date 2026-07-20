package com.medipro.manager.data.document.printing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.medipro.manager.core.datastore.PrinterPreferences
import com.medipro.manager.data.document.receipt.EscPosEncoder
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.ThermalPrintResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ThermalPrinterAdapter {
    suspend fun print(receiptBytes: ByteArray, settings: PrinterSettings): ThermalPrintResult
    fun isAvailable(): Boolean
}

@Singleton
class BluetoothThermalPrinterAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val escPosEncoder: EscPosEncoder,
) : ThermalPrinterAdapter {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter?
        get() {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return manager?.adapter
        }

    override fun isAvailable(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override suspend fun print(receiptBytes: ByteArray, settings: PrinterSettings): ThermalPrintResult =
        withContext(Dispatchers.IO) {
            if (!isAvailable()) {
                return@withContext ThermalPrintResult.Failure(
                    "Bluetooth is off — enable Bluetooth and try again",
                )
            }
            val mac = settings.macAddress.trim()
            if (mac.isBlank()) {
                return@withContext ThermalPrintResult.Failure(
                    "Printer not configured — set MAC address in Settings → Printer",
                )
            }

            val adapter = bluetoothAdapter ?: return@withContext ThermalPrintResult.Failure("Bluetooth unavailable")
            val device = runCatching { adapter.getRemoteDevice(mac) }.getOrElse {
                return@withContext ThermalPrintResult.Failure("Invalid printer MAC address")
            }

            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                adapter.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()

                val output = socket.outputStream
                if (settings.openCashDrawer) {
                    output.write(escPosEncoder.cashDrawerPulse())
                    output.flush()
                }

                output.write(receiptBytes)
                output.flush()

                if (settings.printDuplicateCopy) {
                    Thread.sleep(300)
                    output.write(receiptBytes)
                    output.flush()
                }

                ThermalPrintResult.Success
            } catch (e: IOException) {
                ThermalPrintResult.Failure(
                    message = when {
                        e.message?.contains("read failed", ignoreCase = true) == true ->
                            "Printer disconnected — check power and paper"
                        e.message?.contains("socket closed", ignoreCase = true) == true ->
                            "Connection lost during print"
                        else -> e.message ?: "Failed to print — check printer connection"
                    },
                )
            } finally {
                runCatching { socket?.close() }
            }
        }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<Pair<String, String>> {
        if (!isAvailable()) return emptyList()
        return bluetoothAdapter?.bondedDevices.orEmpty().map { device ->
            (device.name ?: "Unknown") to device.address
        }
    }
}
