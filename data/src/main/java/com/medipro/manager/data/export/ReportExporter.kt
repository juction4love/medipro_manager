package com.medipro.manager.data.export

import android.content.Context
import com.medipro.manager.data.document.report.ReportPdfGenerator
import com.medipro.manager.data.document.report.ReportRenderInput
import com.medipro.manager.domain.model.ReportDashboardSummary
import com.medipro.manager.domain.model.ReportDateRange
import com.medipro.manager.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportPdfGenerator: ReportPdfGenerator,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun exportCsv(
        range: ReportDateRange,
        tabLabel: String,
        summary: ReportDashboardSummary,
    ): File {
        val settings = settingsRepository.getSettings()
        val file = File(context.cacheDir, "report_${range.period.name.lowercase()}_${System.currentTimeMillis()}.csv")
        val lines = buildList {
            add("Pharmacy,${settings.pharmacyName}")
            add("Period,${range.label}")
            add("Report,$tabLabel")
            add("")
            add("Metric,Value")
            add("Sales,${summary.sales}")
            add("Sales Return,${summary.salesReturn}")
            add("Net Sales,${summary.netSales}")
            add("Purchase,${summary.purchase}")
            add("Purchase Return,${summary.purchaseReturn}")
            add("Net Purchase,${summary.netPurchase}")
            add("Expense,${summary.expense}")
            add("Gross Profit,${summary.grossProfit}")
            add("Net Profit,${summary.netProfit}")
            add("Margin %,${"%.2f".format(summary.marginPercent)}")
            add("VAT Collected,${summary.vatCollected}")
            add("VAT Paid,${summary.vatPaid}")
            add("Discount Given,${summary.discountGiven}")
            add("Cash Balance,${summary.cashBalance}")
            add("Credit Sales,${summary.creditSales}")
            add("Customer Due,${summary.customerDue}")
            add("Supplier Due,${summary.supplierDue}")
            add("Inventory Value,${summary.inventoryValue}")
        }
        file.writeText(lines.joinToString("\n"))
        return file
    }

    suspend fun exportPdf(
        range: ReportDateRange,
        tabLabel: String,
        summary: ReportDashboardSummary,
    ): File {
        val settings = settingsRepository.getSettings()
        return reportPdfGenerator.generate(
            ReportRenderInput(
                range = range,
                tabLabel = tabLabel,
                summary = summary,
                settings = settings,
            ),
        )
    }
}
