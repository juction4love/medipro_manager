package com.medipro.manager.feature.reports.presentation

import com.medipro.manager.core.common.ReportPeriod
import com.medipro.manager.domain.model.AuditReportRow
import com.medipro.manager.domain.model.CustomerReportDetail
import com.medipro.manager.domain.model.FinancialReportDetail
import com.medipro.manager.domain.model.InventoryReportDetail
import com.medipro.manager.domain.model.MedicineAnalyticsDetail
import com.medipro.manager.domain.model.PurchaseReportDetail
import com.medipro.manager.domain.model.RankedRow
import com.medipro.manager.domain.model.SalesReportDetail
import com.medipro.manager.domain.model.SupplierReportDetail

enum class ReportTab(val label: String) {
    OVERVIEW("Overview"),
    SALES("Sales"),
    PURCHASE("Purchase"),
    INVENTORY("Inventory"),
    FINANCIAL("Financial"),
    CUSTOMERS("Customers"),
    SUPPLIERS("Suppliers"),
    MEDICINE("Medicine"),
    AUDIT("Audit"),
}

data class KpiUi(val label: String, val value: String)

data class ReportsState(
    val isLoading: Boolean = true,
    val isTabLoading: Boolean = false,
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val showCustomDatePicker: Boolean = false,
    val selectedTab: ReportTab = ReportTab.OVERVIEW,
    val overviewKpis: List<KpiUi> = emptyList(),
    val salesReport: SalesReportDetail? = null,
    val purchaseReport: PurchaseReportDetail? = null,
    val inventoryReport: InventoryReportDetail? = null,
    val financialReport: FinancialReportDetail? = null,
    val customerReport: CustomerReportDetail? = null,
    val supplierReport: SupplierReportDetail? = null,
    val medicineAnalytics: MedicineAnalyticsDetail? = null,
    val auditRows: List<AuditReportRow> = emptyList(),
    val errorMessage: String? = null,
)

data class RankedRowUi(
    val name: String,
    val primary: String,
    val secondary: String? = null,
)

fun RankedRow.toUi(primaryFormatter: (RankedRow) -> String): RankedRowUi =
    RankedRowUi(name = name, primary = primaryFormatter(this), secondary = subtitle)

sealed class ReportsEvent {
    data class ShareExport(val filePath: String, val mimeType: String) : ReportsEvent()
}
