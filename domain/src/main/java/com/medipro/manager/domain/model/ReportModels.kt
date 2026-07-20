package com.medipro.manager.domain.model

import com.medipro.manager.core.common.ReportPeriod

data class ReportDateRange(
    val period: ReportPeriod,
    val startMillis: Long,
    val endMillis: Long,
    val label: String = period.label,
)

data class ReportDashboardSummary(
    val sales: Double = 0.0,
    val salesReturn: Double = 0.0,
    val netSales: Double = 0.0,
    val purchase: Double = 0.0,
    val purchaseReturn: Double = 0.0,
    val netPurchase: Double = 0.0,
    val expense: Double = 0.0,
    val grossProfit: Double = 0.0,
    val netProfit: Double = 0.0,
    val marginPercent: Double = 0.0,
    val cashBalance: Double = 0.0,
    val creditSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val customerDue: Double = 0.0,
    val supplierDue: Double = 0.0,
    val vatCollected: Double = 0.0,
    val vatPaid: Double = 0.0,
    val discountGiven: Double = 0.0,
    val salesCount: Int = 0,
    val purchaseCount: Int = 0,
    val inventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val nearExpiryCount: Int = 0,
    val expiredCount: Int = 0,
)

data class DailyAmountRow(
    val dayMillis: Long,
    val amount: Double,
    val count: Int = 0,
)

data class RankedRow(
    val name: String,
    val quantity: Int = 0,
    val amount: Double = 0.0,
    val subtitle: String? = null,
)

data class SalesReportDetail(
    val summary: ReportDashboardSummary,
    val dailyBreakdown: List<DailyAmountRow>,
    val medicineWise: List<RankedRow>,
    val categoryWise: List<RankedRow>,
    val manufacturerWise: List<RankedRow>,
    val paymentMethodWise: List<RankedRow>,
)

data class PurchaseReportDetail(
    val summary: ReportDashboardSummary,
    val supplierWise: List<RankedRow>,
    val dailyBreakdown: List<DailyAmountRow>,
)

data class InventoryReportDetail(
    val inventoryValue: Double,
    val totalUnits: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val nearExpiry30: Int,
    val nearExpiry60: Int,
    val nearExpiry90: Int,
    val expiredCount: Int,
    val fastMoving: List<RankedRow>,
    val slowMoving: List<RankedRow>,
    val topValueMedicines: List<RankedRow>,
)

data class FinancialReportDetail(
    val summary: ReportDashboardSummary,
    val vatSummary: VatSummary,
    val cashFlow: CashFlowSummary,
)

data class VatSummary(
    val salesVat: Double,
    val purchaseVat: Double,
    val netVat: Double,
)

data class CashFlowSummary(
    val cashIn: Double,
    val cashOut: Double,
    val netCash: Double,
)

data class CustomerReportDetail(
    val topCustomers: List<RankedRow>,
    val creditCustomers: List<RankedRow>,
    val totalOutstanding: Double,
)

data class SupplierReportDetail(
    val supplierDueList: List<RankedRow>,
    val totalOutstanding: Double,
    val recentPurchases: List<RankedRow>,
)

data class AuditReportRow(
    val eventType: String,
    val count: Int,
)

data class MedicineAnalyticsDetail(
    val topSelling: List<RankedRow>,
    val leastSelling: List<RankedRow>,
    val mostReturned: List<RankedRow>,
)

enum class ExportFormat { PDF, CSV }
