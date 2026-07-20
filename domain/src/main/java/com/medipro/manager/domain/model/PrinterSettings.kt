package com.medipro.manager.domain.model

enum class PrinterProfile(val label: String) {
    COUNTER("Counter Printer"),
    OFFICE("Office Printer"),
}

data class PrinterSettings(
    val printerName: String = "",
    val macAddress: String = "",
    val paperWidthMm: Int = 58,
    val charsPerLine: Int = 32,
    val autoConnect: Boolean = true,
    val autoCut: Boolean = true,
    val openCashDrawer: Boolean = false,
    val printLogo: Boolean = true,
    val printDuplicateCopy: Boolean = false,
) {
    val isConfigured: Boolean
        get() = macAddress.isNotBlank()

    companion object {
        val PAPER_58_CHARS = 32
        val PAPER_80_CHARS = 48

        fun charsForPaper(paperWidthMm: Int): Int = when (paperWidthMm) {
            80 -> PAPER_80_CHARS
            else -> PAPER_58_CHARS
        }
    }
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
)

sealed class ThermalPrintResult {
    data object Success : ThermalPrintResult()
    data class Failure(val message: String, val recoverable: Boolean = true) : ThermalPrintResult()
}
