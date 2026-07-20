package com.medipro.manager.data.repository

import com.medipro.manager.data.document.printing.BluetoothThermalPrinterAdapter
import com.medipro.manager.data.document.receipt.EscPosEncoder
import com.medipro.manager.data.document.receipt.ReceiptFormatter
import com.medipro.manager.data.document.report.PaymentVoucherPdfGenerator
import com.medipro.manager.data.document.report.PaymentVoucherRenderInput
import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.ThermalPrintResult
import com.medipro.manager.domain.repository.AccountingDocumentRepository
import com.medipro.manager.domain.repository.PrinterRepository
import com.medipro.manager.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountingDocumentRepositoryImpl @Inject constructor(
    private val pdfGenerator: PaymentVoucherPdfGenerator,
    private val receiptFormatter: ReceiptFormatter,
    private val escPosEncoder: EscPosEncoder,
    private val bluetoothAdapter: BluetoothThermalPrinterAdapter,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
) : AccountingDocumentRepository {

    override suspend fun generatePaymentVoucherPdf(voucher: PaymentVoucherInput): File {
        val settings = settingsRepository.getSettings()
        return pdfGenerator.generate(PaymentVoucherRenderInput(voucher = voucher, settings = settings))
    }

    override suspend fun printPaymentVoucher(
        voucher: PaymentVoucherInput,
        profile: PrinterProfile,
    ): ThermalPrintResult {
        val printerSettings = printerRepository.getPrinterSettings(profile)
        if (!printerSettings.isConfigured) {
            return ThermalPrintResult.Failure("Configure ${profile.label} MAC address first")
        }
        val pharmacy = settingsRepository.getSettings()
        val content = receiptFormatter.formatPaymentVoucher(voucher, pharmacy, printerSettings)
        val bytes = escPosEncoder.encode(content, printerSettings)
        return bluetoothAdapter.print(bytes, printerSettings)
    }
}
