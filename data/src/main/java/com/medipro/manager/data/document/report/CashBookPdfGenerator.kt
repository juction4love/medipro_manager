package com.medipro.manager.data.document.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.document.DocumentGenerator
import com.medipro.manager.data.document.PdfFooter
import com.medipro.manager.domain.model.CashBookEntry
import com.medipro.manager.domain.model.PharmacySettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CashBookRenderInput(
    val entries: List<CashBookEntry>,
    val openingBalance: Double,
    val closingBalance: Double,
    val dateLabel: String,
    val settings: PharmacySettings,
    val generatedAt: Long = System.currentTimeMillis(),
)

@Singleton
class CashBookPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appVersionProvider: AppVersionProvider,
) : DocumentGenerator<CashBookRenderInput> {

    override fun generate(input: CashBookRenderInput): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }
        val smallPaint = Paint().apply { textSize = 9f; color = 0xFF555555.toInt() }

        val left = 40f
        var y = 40f
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())

        canvas.drawText(input.settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, left, y, titlePaint)
        y += 22f
        canvas.drawText("Cash Book — ${input.dateLabel}", left, y, headerPaint)
        y += 20f
        canvas.drawText("Opening: ${formatRs(input.openingBalance)}", left, y, bodyPaint)
        y += 24f

        canvas.drawText("Time", left, y, headerPaint)
        canvas.drawText("Description", left + 70f, y, headerPaint)
        canvas.drawText("In", pageWidth - 120f, y, headerPaint)
        canvas.drawText("Out", pageWidth - 60f, y, headerPaint)
        y += 18f

        input.entries.forEach { entry ->
            if (y > pageHeight - 80f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 40f
            }
            canvas.drawText(timeFmt.format(Date(entry.entryDate)), left, y, bodyPaint)
            canvas.drawText(entry.description.take(28), left + 70f, y, bodyPaint)
            if (entry.cashIn > 0) canvas.drawText(formatRs(entry.cashIn), pageWidth - 120f, y, bodyPaint)
            if (entry.cashOut > 0) canvas.drawText(formatRs(entry.cashOut), pageWidth - 60f, y, bodyPaint)
            y += 16f
        }

        y += 12f
        canvas.drawText("Closing: ${formatRs(input.closingBalance)}", left, y, headerPaint)

        PdfFooter.draw(canvas, left, pageHeight.toFloat(), appVersionProvider.versionName, input.generatedAt, smallPaint)
        document.finishPage(page)

        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "MediPro/CashBook",
        ).apply { mkdirs() }
        val file = File(dir, "cashbook_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun formatRs(amount: Double): String =
        String.format(Locale.getDefault(), "%.2f", amount)
}
