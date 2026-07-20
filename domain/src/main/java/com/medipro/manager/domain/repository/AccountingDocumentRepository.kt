package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.ThermalPrintResult
import java.io.File

interface AccountingDocumentRepository {
    suspend fun generatePaymentVoucherPdf(voucher: PaymentVoucherInput): File
    suspend fun printPaymentVoucher(
        voucher: PaymentVoucherInput,
        profile: PrinterProfile = PrinterProfile.COUNTER,
    ): ThermalPrintResult
}
