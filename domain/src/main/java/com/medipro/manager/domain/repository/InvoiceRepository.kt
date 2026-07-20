package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.InvoiceDocument
import com.medipro.manager.domain.model.Sale

interface InvoiceRepository {
    suspend fun generateInvoice(
        sale: Sale,
        pharmacistName: String? = null,
        forceRegenerate: Boolean = false,
    ): InvoiceDocument

    fun findExistingInvoice(invoiceNumber: String): InvoiceDocument?

    suspend fun getOrGenerateInvoice(
        sale: Sale,
        pharmacistName: String? = null,
    ): InvoiceDocument

    suspend fun recordPrintAndRegenerate(sale: Sale, pharmacistName: String? = null): InvoiceDocument

    suspend fun printThermal(sale: Sale, pharmacistName: String? = null): Result<Unit>
}
