package com.medipro.manager.domain.usecase.sales

import com.medipro.manager.domain.model.InvoiceDocument
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.repository.InvoiceRepository
import com.medipro.manager.domain.repository.LicenseRepository
import javax.inject.Inject

class PrintInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val licenseRepository: LicenseRepository,
) {
    suspend fun thermal(sale: Sale): Result<Unit> {
        val pharmacist = licenseRepository.getLicense()?.ownerName?.takeIf { it.isNotBlank() }
        return invoiceRepository.printThermal(sale, pharmacist)
    }

    suspend fun regenerateForReprint(sale: Sale): InvoiceDocument {
        val pharmacist = licenseRepository.getLicense()?.ownerName?.takeIf { it.isNotBlank() }
        return invoiceRepository.recordPrintAndRegenerate(sale, pharmacist)
    }
}
