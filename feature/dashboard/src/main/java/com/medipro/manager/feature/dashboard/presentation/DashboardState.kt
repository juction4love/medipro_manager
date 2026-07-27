package com.medipro.manager.feature.dashboard.presentation

import com.medipro.manager.domain.model.AlertSeverity
import com.medipro.manager.domain.licensing.LicenseAccessState

data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFromCache: Boolean = false,
    val pharmacyName: String = "My Pharmacy",
    val dateLabel: String = "",
    val syncStatusLabel: String = "Offline Ready",
    val licenseDaysRemaining: Int? = null,
    // Financial
    val todaySales: String = "Rs. 0",
    val todaySalesReturn: String = "Rs. 0",
    val netSales: String = "Rs. 0",
    val todayAdjustments: String = "0",
    val todayPurchase: String = "Rs. 0",
    val todayExpense: String = "Rs. 0",
    val profit: String = "Rs. 0",
    val cashInDrawer: String = "Rs. 0",
    val bankBalance: String = "Rs. 0",
    val todaySalesCount: Int = 0,
    val todayPurchaseCount: Int = 0,
    // Inventory
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val nearExpiryCount: Int = 0,
    val expiredCount: Int = 0,
    val inventoryValue: String = "Rs. 0",
    val activeMedicineCount: String = "0",
    val masterCatalogCount: String = "271,044",
    val totalStockUnits: String = "0",
    // Customer
    val todayCustomers: Int = 0,
    val pendingCustomerDue: String = "Rs. 0",
    val pendingCustomerDueCount: Int = 0,
    val collectedToday: String = "Rs. 0",
    // Supplier
    val supplierDue: String = "Rs. 0",
    val supplierDueCount: Int = 0,
    val todaySupplierPayment: String = "Rs. 0",
    // Business
    val bestSellingMedicine: String = "—",
    val topCategory: String = "—",
    val todayTransactions: Int = 0,
    // Analytics
    val yesterdaySales: String = "Rs. 0",
    val last7DaysSales: String = "Rs. 0",
    val last30DaysSales: String = "Rs. 0",
    val ocrTodayBills: String = "0",
    val ocrAverageAccuracy: String = "—",
    val ocrLearnedAliases: String = "0",
    val ocrManualCorrections: String = "0",
    val ocrSavedTime: String = "0 min",
    val ocrLastScanTime: String = "—",
    val ocrLastScanSupplier: String = "—",
    val ocrLastScanAccuracy: String = "—",
    val alerts: List<AlertUi> = emptyList(),
    val recentSales: List<RecentSaleUi> = emptyList(),
    val recentPurchases: List<RecentPurchaseUi> = emptyList(),
    val prescriptionModuleEnabled: Boolean = false,
    // Backup
    val lastBackupLabel: String? = null,
    val lastBackupEncrypted: Boolean = false,
    val cloudBackupStatus: String = "Not Connected",
    // License
    val licenseMobile: String? = null,
    val licensePlan: String = "Free",
    val licenseValidUntil: String? = null,
    val licenseLastVerified: String? = null,
    val licenseAccessState: LicenseAccessState = LicenseAccessState.NO_LICENSE,
)

data class AlertUi(
    val message: String,
    val severity: AlertSeverity,
    val route: String?,
)

data class RecentSaleUi(
    val id: Long,
    val invoiceNumber: String,
    val amount: String,
    val date: String,
)

data class RecentPurchaseUi(
    val id: Long,
    val invoiceNumber: String,
    val amount: String,
    val date: String,
)

sealed interface DashboardEvent {
    data object Refresh : DashboardEvent
    data class NavigateToFeature(val route: String) : DashboardEvent
    data class OpenInvoice(val saleId: Long) : DashboardEvent
}

data class KpiItem(
    val label: String,
    val value: String,
    val subtitle: String? = null,
)
