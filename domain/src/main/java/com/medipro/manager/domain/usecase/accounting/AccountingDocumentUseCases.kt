package com.medipro.manager.domain.usecase.accounting

import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.repository.AccountingDocumentRepository
import java.io.File
import javax.inject.Inject

class GeneratePaymentVoucherPdfUseCase @Inject constructor(
    private val repository: AccountingDocumentRepository,
) {
    suspend operator fun invoke(voucher: PaymentVoucherInput): File =
        repository.generatePaymentVoucherPdf(voucher)
}

class PrintPaymentVoucherUseCase @Inject constructor(
    private val repository: AccountingDocumentRepository,
) {
    suspend operator fun invoke(
        voucher: PaymentVoucherInput,
        profile: PrinterProfile = PrinterProfile.COUNTER,
    ) = repository.printPaymentVoucher(voucher, profile)
}
