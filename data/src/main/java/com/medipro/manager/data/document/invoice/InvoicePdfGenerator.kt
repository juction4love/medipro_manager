package com.medipro.manager.data.document.invoice

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.document.DocumentGenerator
import com.medipro.manager.data.document.PdfFooter
import com.medipro.manager.data.document.PdfWatermark
import com.medipro.manager.data.invoice.InvoiceStorage
import com.medipro.manager.data.invoice.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoicePdfGenerator @Inject constructor(
    private val qrCodeGenerator: QrCodeGenerator,
    private val invoiceStorage: InvoiceStorage,
    private val appVersionProvider: AppVersionProvider,
) : DocumentGenerator<InvoiceRenderInput> {

    override fun generate(input: InvoiceRenderInput): File = generateInternal(input)

    private fun generateInternal(input: InvoiceRenderInput): File {
        val sale = input.sale
        val settings = input.settings
        val pageWidth = 595
        val pageHeight = 842
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        input.watermark?.let { PdfWatermark.draw(canvas, pageWidth, pageHeight, it) }

        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val smallPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt() }
        val dividerPaint = Paint().apply { strokeWidth = 1f; color = 0xFFCCCCCC.toInt() }

        val left = 40f
        val right = pageWidth - 40f
        var y = 48f

        canvas.drawText(settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, left, y, titlePaint)
        y += 22f
        if (settings.pharmacyAddress.isNotBlank()) {
            canvas.drawText(settings.pharmacyAddress, left, y, bodyPaint)
            y += 16f
        }
        if (settings.pharmacyPhone.isNotBlank()) {
            canvas.drawText(settings.pharmacyPhone, left, y, bodyPaint)
            y += 16f
        }
        val taxLine = buildList {
            if (settings.panNumber.isNotBlank()) add("PAN: ${settings.panNumber}")
            if (settings.vatNumber.isNotBlank()) add("VAT: ${settings.vatNumber}")
        }.joinToString("  |  ")
        if (taxLine.isNotBlank()) {
            canvas.drawText(taxLine, left, y, smallPaint)
            y += 18f
        }

        val qrPayload = sale.uuid.takeIf { it.isNotBlank() }?.let { "medipro://invoice/$it" }
            ?: "medipro://invoice/${sale.invoiceNumber}"
        qrCodeGenerator.encode(qrPayload)?.let { qr ->
            canvas.drawBitmap(qr, right - qr.width, 36f, null)
        }

        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 22f

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(sale.saleDate))
        canvas.drawText("Invoice No : ${sale.invoiceNumber}", left, y, headerPaint)
        y += 18f
        canvas.drawText("Date       : $dateStr", left, y, bodyPaint)
        y += 16f
        sale.customerName?.let {
            canvas.drawText("Customer   : $it", left, y, bodyPaint)
            y += 16f
        }
        input.pharmacistName?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("Pharmacist : $it", left, y, bodyPaint)
            y += 16f
        }

        y += 6f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        sale.items.forEach { item ->
            canvas.drawText(item.medicineName, left, y, headerPaint)
            y += 16f
            canvas.drawText("Qty ${item.quantity} × ${formatMoney(item.unitPrice)}", left + 8f, y, bodyPaint)
            canvas.drawText(formatMoney(item.totalPrice), right - 60f, y, bodyPaint)
            y += 14f
            val meta = buildList {
                if (item.batchNumber.isNotBlank()) add("Batch: ${item.batchNumber}")
                item.expiryDate?.let { exp ->
                    add("Exp: ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(exp))}")
                }
            }.joinToString("  •  ")
            if (meta.isNotBlank()) {
                canvas.drawText(meta, left + 8f, y, smallPaint)
                y += 14f
            }
            y += 6f
        }

        y += 4f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 22f
        drawTotalRow(canvas, "Subtotal", sale.subtotal, left, right, bodyPaint, y)
        y += 18f
        drawTotalRow(canvas, "Discount", sale.discount, left, right, bodyPaint, y)
        y += 18f
        drawTotalRow(canvas, "VAT", sale.vatAmount, left, right, bodyPaint, y)
        y += 22f
        drawTotalRow(canvas, "TOTAL ${settings.currency}", sale.totalAmount, left, right, headerPaint, y)
        y += 24f
        canvas.drawText("Payment : ${sale.paymentMethod}", left, y, bodyPaint)
        y += 16f
        if (sale.paidAmount < sale.totalAmount) {
            canvas.drawText("Paid: ${formatMoney(sale.paidAmount)}", left, y, bodyPaint)
            y += 16f
        }
        sale.prescriptionNumber?.let {
            y += 8f
            canvas.drawText("Rx #: $it", left, y, bodyPaint)
            y += 14f
        }
        sale.doctorName?.let {
            canvas.drawText("Doctor: $it", left, y, bodyPaint)
            y += 14f
        }
        sale.patientName?.let {
            canvas.drawText("Patient: $it", left, y, bodyPaint)
            y += 14f
        }

        y += 20f
        canvas.drawText("Thank You — Visit Again", left, y, headerPaint)

        PdfFooter.draw(
            canvas = canvas,
            left = left,
            pageHeight = pageHeight.toFloat(),
            versionName = appVersionProvider.versionName,
            generatedAt = input.generatedAt,
            paint = smallPaint,
        )

        document.finishPage(page)

        val documentsFile = invoiceStorage.invoiceFile(sale.invoiceNumber)
        FileOutputStream(documentsFile).use { document.writeTo(it) }
        invoiceStorage.cacheInvoiceFile(sale.invoiceNumber)
            .also { cache -> documentsFile.copyTo(cache, overwrite = true) }
        document.close()
        return documentsFile
    }

    private fun drawTotalRow(
        canvas: android.graphics.Canvas,
        label: String,
        amount: Double,
        left: Float,
        right: Float,
        paint: Paint,
        y: Float,
    ) {
        canvas.drawText(label, left, y, paint)
        canvas.drawText(formatMoney(amount), right - 80f, y, paint)
    }

    private fun formatMoney(value: Double): String =
        String.format(Locale.getDefault(), "%.2f", value)
}
