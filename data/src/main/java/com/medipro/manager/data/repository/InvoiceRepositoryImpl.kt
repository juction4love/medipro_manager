package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.data.document.invoice.InvoicePdfGenerator
import com.medipro.manager.data.document.invoice.InvoiceReceiptFormatter
import com.medipro.manager.data.document.invoice.InvoiceRenderInput
import com.medipro.manager.data.document.printing.ThermalPrinterAdapter
import com.medipro.manager.data.invoice.InvoiceStorage
import com.medipro.manager.domain.model.InvoiceDocument
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.ThermalPrintResult
import com.medipro.manager.domain.repository.InvoiceRepository
import com.medipro.manager.domain.repository.PrinterRepository
import com.medipro.manager.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val pdfGenerator: InvoicePdfGenerator,
    private val receiptFormatter: InvoiceReceiptFormatter,
    private val thermalPrinter: ThermalPrinterAdapter,
    private val printerRepository: PrinterRepository,
    private val invoiceStorage: InvoiceStorage,
    private val settingsRepository: SettingsRepository,
    private val saleDao: SaleDao,
) : InvoiceRepository {

    override suspend fun generateInvoice(
        sale: Sale,
        pharmacistName: String?,
        forceRegenerate: Boolean,
    ): InvoiceDocument {
        if (!forceRegenerate) {
            findExistingInvoice(sale.invoiceNumber)?.let { return it }
        }
        val settings = settingsRepository.getSettings()
        val watermark = watermarkFor(sale)
        val file = pdfGenerator.generate(
            InvoiceRenderInput(
                sale = sale,
                settings = settings,
                pharmacistName = pharmacistName,
                watermark = watermark,
            ),
        )
        return InvoiceDocument(
            invoiceNumber = sale.invoiceNumber,
            filePath = file.absolutePath,
            savedToDocuments = true,
        )
    }

    override fun findExistingInvoice(invoiceNumber: String): InvoiceDocument? {
        val documentsFile = invoiceStorage.invoiceFile(invoiceNumber)
        if (documentsFile.exists()) {
            return InvoiceDocument(invoiceNumber, documentsFile.absolutePath, savedToDocuments = true)
        }
        val cacheFile = invoiceStorage.cacheInvoiceFile(invoiceNumber)
        if (cacheFile.exists()) {
            return InvoiceDocument(invoiceNumber, cacheFile.absolutePath, savedToDocuments = false)
        }
        return null
    }

    override suspend fun getOrGenerateInvoice(
        sale: Sale,
        pharmacistName: String?,
    ): InvoiceDocument = generateInvoice(sale, pharmacistName, forceRegenerate = false)

    private fun watermarkFor(sale: Sale): String? =
        if (sale.printCount > 0) "REPRINT" else null

    override suspend fun recordPrintAndRegenerate(
        sale: Sale,
        pharmacistName: String?,
    ): InvoiceDocument {
        val watermark = watermarkFor(sale)
        saleDao.recordPrint(sale.id)
        val updated = sale.copy(
            printCount = sale.printCount + 1,
            lastPrintedAt = System.currentTimeMillis(),
        )
        val settings = settingsRepository.getSettings()
        val file = pdfGenerator.generate(
            InvoiceRenderInput(
                sale = updated,
                settings = settings,
                pharmacistName = pharmacistName,
                watermark = watermark,
            ),
        )
        return InvoiceDocument(updated.invoiceNumber, file.absolutePath, savedToDocuments = true)
    }

    override suspend fun printThermal(sale: Sale, pharmacistName: String?): Result<Unit> {
        val settings = settingsRepository.getSettings()
        val printerSettings = printerRepository.getPrinterSettings(com.medipro.manager.domain.model.PrinterProfile.COUNTER)
        val watermark = watermarkFor(sale)
        saleDao.recordPrint(sale.id)
        val updated = sale.copy(printCount = sale.printCount + 1, lastPrintedAt = System.currentTimeMillis())
        val bytes = receiptFormatter.format(
            sale = updated,
            settings = settings,
            printerSettings = printerSettings,
            pharmacistName = pharmacistName,
            watermark = watermark,
        )
        return when (val result = thermalPrinter.print(bytes, printerSettings)) {
            is ThermalPrintResult.Success -> Result.success(Unit)
            is ThermalPrintResult.Failure -> Result.failure(Exception(result.message))
        }
    }
}
