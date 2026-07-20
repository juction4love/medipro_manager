package com.medipro.manager.data.document.receipt

import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.model.PrinterSettings
import com.medipro.manager.domain.model.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Unified receipt content — single source for PDF preview parity and ESC/POS output. */
data class ReceiptContent(
    val lines: List<ReceiptLine>,
    val qrPayload: String?,
)

enum class ReceiptLineStyle { NORMAL, BOLD, DIVIDER }

data class ReceiptLine(
    val text: String,
    val style: ReceiptLineStyle = ReceiptLineStyle.NORMAL,
)

@Singleton
class ReceiptFormatter @Inject constructor() {

    fun formatSale(
        sale: Sale,
        settings: PharmacySettings,
        printerSettings: PrinterSettings,
        cashierName: String? = null,
        watermark: String? = null,
    ): ReceiptContent {
        val width = printerSettings.charsPerLine
        val divider = "-".repeat(width)
        val dateFmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val expiryFmt = SimpleDateFormat("MM/yy", Locale.getDefault())
        val qrPayload = "medipro://invoice/${sale.invoiceNumber}"

        val lines = buildList {
            if (printerSettings.printLogo) {
                add(ReceiptLine(settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, ReceiptLineStyle.BOLD))
            } else {
                add(ReceiptLine(settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, ReceiptLineStyle.BOLD))
            }
            if (settings.panNumber.isNotBlank()) add(ReceiptLine("PAN: ${settings.panNumber}"))
            if (settings.pharmacyPhone.isNotBlank()) add(ReceiptLine("Tel: ${settings.pharmacyPhone}"))
            if (settings.pharmacyAddress.isNotBlank()) {
                settings.pharmacyAddress.wrap(width).forEach { add(ReceiptLine(it)) }
            }
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine("Invoice: ${sale.invoiceNumber}"))
            add(ReceiptLine("Date: ${dateFmt.format(Date(sale.saleDate))}"))
            cashierName?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Cashier: $it")) }
            sale.customerName?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Customer: $it")) }
            watermark?.let { add(ReceiptLine("*** $it ***")) }
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))

            sale.items.forEach { item ->
                add(ReceiptLine(item.medicineName.take(width), ReceiptLineStyle.BOLD))
                if (item.batchNumber.isNotBlank()) add(ReceiptLine(" Batch: ${item.batchNumber}"))
                item.expiryDate?.let { add(ReceiptLine(" Exp: ${expiryFmt.format(Date(it))}")) }
                add(
                    ReceiptLine(
                        " Qty ${item.quantity} x ${fmt(item.unitPrice)} = ${fmt(item.totalPrice)}",
                    ),
                )
            }

            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine(row("Subtotal", sale.subtotal, width)))
            if (sale.discount > 0) add(ReceiptLine(row("Discount", sale.discount, width)))
            if (sale.vatAmount > 0) add(ReceiptLine(row("VAT", sale.vatAmount, width)))
            add(ReceiptLine(row("Grand Total", sale.totalAmount, width), ReceiptLineStyle.BOLD))
            add(ReceiptLine("Payment: ${sale.paymentMethod}"))
            sale.prescriptionNumber?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Rx: $it")) }
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine(center("Thank You", width)))
            add(ReceiptLine(center("MediPro ERP", width)))
        }

        return ReceiptContent(lines = lines, qrPayload = qrPayload)
    }

    fun formatTestReceipt(
        settings: PharmacySettings,
        printerSettings: PrinterSettings,
    ): ReceiptContent {
        val width = printerSettings.charsPerLine
        val divider = "-".repeat(width)
        val lines = buildList {
            add(ReceiptLine(settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, ReceiptLineStyle.BOLD))
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine("TEST PRINT", ReceiptLineStyle.BOLD))
            add(ReceiptLine("Paper: ${printerSettings.paperWidthMm}mm"))
            add(ReceiptLine("Columns: $width"))
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine(center("Printer OK", width)))
        }
        return ReceiptContent(lines = lines, qrPayload = "medipro://test")
    }

    fun formatPaymentVoucher(
        voucher: com.medipro.manager.domain.model.PaymentVoucherInput,
        settings: PharmacySettings,
        printerSettings: PrinterSettings,
    ): ReceiptContent {
        val width = printerSettings.charsPerLine
        val divider = "-".repeat(width)
        val dateFmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val lines = buildList {
            add(ReceiptLine(settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, ReceiptLineStyle.BOLD))
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine(voucher.title, ReceiptLineStyle.BOLD))
            add(ReceiptLine("Ref: ${voucher.reference}"))
            add(ReceiptLine("Date: ${dateFmt.format(Date(voucher.paymentDate))}"))
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine("Party: ${voucher.partyName.take(width - 7)}"))
            add(ReceiptLine(row("Amount", voucher.amount, width), ReceiptLineStyle.BOLD))
            add(ReceiptLine("Method: ${voucher.paymentMethod.replace('_', ' ')}"))
            if (voucher.previousBalance > 0) {
                add(ReceiptLine(row("Prev Due", voucher.previousBalance, width)))
            }
            if (voucher.remainingBalance >= 0) {
                add(ReceiptLine(row("Balance", voucher.remainingBalance, width)))
            }
            voucher.notes?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine("Note: ${it.take(width - 6)}")) }
            add(ReceiptLine(divider, ReceiptLineStyle.DIVIDER))
            add(ReceiptLine(center("Thank You", width)))
        }
        return ReceiptContent(lines = lines, qrPayload = null)
    }

    private fun fmt(amount: Double) = String.format(Locale.getDefault(), "%.2f", amount)

    private fun row(label: String, amount: Double, width: Int): String {
        val value = fmt(amount)
        val spaces = (width - label.length - value.length).coerceAtLeast(1)
        return label + " ".repeat(spaces) + value
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun String.wrap(width: Int): List<String> =
        if (isEmpty()) emptyList() else chunked(width.coerceAtLeast(1))
}
