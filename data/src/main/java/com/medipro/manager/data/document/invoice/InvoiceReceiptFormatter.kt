package com.medipro.manager.data.document.invoice

import com.medipro.manager.data.document.receipt.EscPosEncoder
import com.medipro.manager.data.document.receipt.ReceiptFormatter
import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.Sale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats a [Sale] into ESC/POS bytes for Bluetooth thermal printers.
 * Delegates to [ReceiptFormatter] + [EscPosEncoder] — same content model as PDF.
 */
@Singleton
class InvoiceReceiptFormatter @Inject constructor(
    private val receiptFormatter: ReceiptFormatter,
    private val escPosEncoder: EscPosEncoder,
) {

    fun format(
        sale: Sale,
        settings: PharmacySettings,
        printerSettings: PrinterSettings,
        pharmacistName: String? = null,
        watermark: String? = null,
    ): ByteArray {
        val content = receiptFormatter.formatSale(
            sale = sale,
            settings = settings,
            printerSettings = printerSettings,
            cashierName = pharmacistName,
            watermark = watermark,
        )
        return escPosEncoder.encode(content, printerSettings)
    }
}
