package com.medipro.manager.data.document.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.document.DocumentGenerator
import com.medipro.manager.data.document.PdfFooter
import com.medipro.manager.domain.model.DayClosingRecord
import com.medipro.manager.domain.model.PharmacySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class DayClosingRenderInput(
    val record: DayClosingRecord,
    val settings: PharmacySettings,
    val generatedAt: Long = System.currentTimeMillis(),
)

@Singleton
class DayClosingPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appVersionProvider: AppVersionProvider,
) : DocumentGenerator<DayClosingRenderInput> {

    override fun generate(input: DayClosingRenderInput): File {
        val record = input.record
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val smallPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt() }

        var y = 40f
        val left = 40f
        val valueX = 280f

        canvas.drawText(input.settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, left, y, titlePaint)
        y += 22f
        canvas.drawText("Day Closing Report — ${record.dateLabel}", left, y, headerPaint)
        y += 18f
        canvas.drawText("Ref: ${record.reference}", left, y, smallPaint)
        y += 28f

        fun row(label: String, value: String) {
            canvas.drawText(label, left, y, bodyPaint)
            canvas.drawText(value, valueX, y, bodyPaint)
            y += 18f
        }

        row("Sales Count", record.salesCount.toString())
        row("Return Count", record.returnCount.toString())
        row("Discount", formatRs(record.discountTotal))
        row("VAT Collected", formatRs(record.vatTotal))
        y += 8f
        canvas.drawText("Payment Breakdown", left, y, headerPaint)
        y += 20f
        row("Cash Sales", formatRs(record.cashSales))
        row("Card", formatRs(record.cardSales))
        row("eSewa", formatRs(record.esewaSales))
        row("Khalti", formatRs(record.khaltiSales))
        row("IME Pay", formatRs(record.imeSales))
        row("Credit Sales", formatRs(record.creditSales))
        y += 8f
        canvas.drawText("Cash Movement", left, y, headerPaint)
        y += 20f
        row("Opening Cash", formatRs(record.openingCash))
        row("Customer Receipts", formatRs(record.customerReceipts))
        row("Supplier Payments", formatRs(-record.supplierPayments))
        row("Expenses", formatRs(-record.expenses))
        row("Returns", formatRs(-record.returnsAmount))
        y += 8f
        row("Expected Cash", formatRs(record.expectedCash))
        row("Actual Cash", formatRs(record.actualCash))
        row("Difference", formatRs(record.difference))
        record.differenceReason?.let {
            row("Reason", it.replace('_', ' '))
        }
        record.remarks?.takeIf { it.isNotBlank() }?.let {
            y += 6f
            canvas.drawText("Remarks: $it", left, y, smallPaint)
        }

        y += 36f
        canvas.drawText("Signatures", left, y, headerPaint)
        y += 28f
        val sigWidth = (pageWidth - left * 2 - 40f) / 2f
        drawSignatureBlock(canvas, left, y, sigWidth, "Cashier", bodyPaint, smallPaint)
        drawSignatureBlock(canvas, left + sigWidth + 40f, y, sigWidth, "Manager", bodyPaint, smallPaint)

        PdfFooter.draw(canvas, left, pageHeight.toFloat(), appVersionProvider.versionName, input.generatedAt, smallPaint)
        document.finishPage(page)

        val file = outputFile(record.reference)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun formatRs(amount: Double): String =
        String.format(Locale.getDefault(), "Rs. %.2f", amount)

    private fun drawSignatureBlock(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        width: Float,
        role: String,
        bodyPaint: Paint,
        smallPaint: Paint,
    ) {
        val lineY = y + 36f
        canvas.drawLine(x, lineY, x + width, lineY, bodyPaint)
        canvas.drawText(role, x + width / 2f - bodyPaint.measureText(role) / 2f, lineY + 16f, smallPaint)
        canvas.drawText("Sign", x + width / 2f - smallPaint.measureText("Sign") / 2f, lineY + 30f, smallPaint)
    }

    private fun outputFile(reference: String): File {
        val safeRef = reference.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "MediPro/DayClosing",
        ).apply { mkdirs() }
        return File(dir, "day_close_${safeRef}_${System.currentTimeMillis()}.pdf")
    }

    companion object {
        fun formatDateLabel(epochMs: Long): String =
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMs))
    }
}
