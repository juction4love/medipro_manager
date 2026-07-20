package com.medipro.manager.data.document.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.document.DocumentGenerator
import com.medipro.manager.data.document.PdfFooter
import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.model.PharmacySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class PaymentVoucherRenderInput(
    val voucher: PaymentVoucherInput,
    val settings: PharmacySettings,
    val generatedAt: Long = System.currentTimeMillis(),
)

@Singleton
class PaymentVoucherPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appVersionProvider: AppVersionProvider,
) : DocumentGenerator<PaymentVoucherRenderInput> {

    override fun generate(input: PaymentVoucherRenderInput): File {
        val voucher = input.voucher
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val smallPaint = Paint().apply { textSize = 10f; color = 0xFF555555.toInt() }

        var y = 48f
        val left = 40f
        val valueX = 220f

        canvas.drawText(input.settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, left, y, titlePaint)
        y += 24f
        canvas.drawText(voucher.title, left, y, headerPaint)
        y += 20f
        canvas.drawText("Ref: ${voucher.reference}", left, y, bodyPaint)
        y += 18f
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(voucher.paymentDate))
        canvas.drawText("Date: $dateStr", left, y, bodyPaint)
        y += 28f

        fun row(label: String, value: String) {
            canvas.drawText(label, left, y, bodyPaint)
            canvas.drawText(value, valueX, y, bodyPaint)
            y += 20f
        }

        row("Party", voucher.partyName)
        row("Amount", formatRs(voucher.amount))
        row("Payment Method", voucher.paymentMethod.replace('_', ' '))
        if (voucher.previousBalance > 0) row("Previous Due", formatRs(voucher.previousBalance))
        if (voucher.remainingBalance >= 0) row("Remaining Due", formatRs(voucher.remainingBalance))
        voucher.notes?.takeIf { it.isNotBlank() }?.let {
            y += 6f
            canvas.drawText("Notes: $it", left, y, smallPaint)
            y += 18f
        }

        y += 24f
        canvas.drawText("Received / Paid By", left, y, smallPaint)
        y += 40f
        canvas.drawLine(left, y, left + 200f, y, bodyPaint)
        y += 16f
        canvas.drawText("Signature", left, y, smallPaint)

        PdfFooter.draw(canvas, left, pageHeight.toFloat(), appVersionProvider.versionName, input.generatedAt, smallPaint)
        document.finishPage(page)

        val subdir = if (voucher.type == "CUSTOMER_RECEIPT") "CustomerReceipts" else "SupplierPayments"
        val safeRef = voucher.reference.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "MediPro/$subdir",
        ).apply { mkdirs() }
        val file = File(dir, "voucher_${safeRef}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun formatRs(amount: Double): String =
        String.format(Locale.getDefault(), "Rs. %.2f", amount)
}
