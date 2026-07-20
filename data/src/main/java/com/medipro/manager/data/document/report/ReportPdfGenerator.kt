package com.medipro.manager.data.document.report

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.document.DocumentGenerator
import com.medipro.manager.data.document.PdfFooter
import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.model.ReportDateRange
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ReportRenderInput(
    val range: ReportDateRange,
    val tabLabel: String,
    val summary: ReportDashboardSummary,
    val settings: PharmacySettings,
    val generatedAt: Long = System.currentTimeMillis(),
)

@Singleton
class ReportPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appVersionProvider: AppVersionProvider,
) : DocumentGenerator<ReportRenderInput> {

    override fun generate(input: ReportRenderInput): File {
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
        canvas.drawText(input.settings.pharmacyName.ifBlank { "MediPro Pharmacy" }, left, y, titlePaint)
        y += 22f
        canvas.drawText("${input.tabLabel} — ${input.range.label}", left, y, headerPaint)
        y += 28f

        val rows = listOf(
            "Sales" to input.summary.sales,
            "Sales Return" to input.summary.salesReturn,
            "Net Sales" to input.summary.netSales,
            "Purchase" to input.summary.purchase,
            "Expense" to input.summary.expense,
            "Gross Profit" to input.summary.grossProfit,
            "Net Profit" to input.summary.netProfit,
            "Margin %" to input.summary.marginPercent,
            "VAT Collected" to input.summary.vatCollected,
            "VAT Paid" to input.summary.vatPaid,
            "Cash Balance" to input.summary.cashBalance,
            "Customer Due" to input.summary.customerDue,
            "Supplier Due" to input.summary.supplierDue,
            "Inventory Value" to input.summary.inventoryValue,
        )
        rows.forEach { (label, value) ->
            canvas.drawText(label, left, y, headerPaint)
            val text = if (label == "Margin %") {
                String.format(Locale.getDefault(), "%.2f%%", value)
            } else {
                String.format(Locale.getDefault(), "Rs. %.2f", value)
            }
            canvas.drawText(text, 280f, y, bodyPaint)
            y += 18f
        }

        PdfFooter.draw(canvas, left, pageHeight.toFloat(), appVersionProvider.versionName, input.generatedAt, smallPaint)
        document.finishPage(page)

        val file = reportFile(input.range.period.name.lowercase(), input.tabLabel)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun reportFile(period: String, tab: String): File {
        val safeTab = tab.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "MediPro/Reports",
        ).apply { mkdirs() }
        return File(dir, "report_${period}_${safeTab}_${System.currentTimeMillis()}.pdf")
    }
}
