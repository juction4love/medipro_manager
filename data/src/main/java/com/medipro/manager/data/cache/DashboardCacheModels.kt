package com.medipro.manager.data.cache

import kotlinx.serialization.Serializable

@Serializable
data class DashboardCachePayload(
    val pharmacyName: String = "",
    val dateLabel: String = "",
    val stats: CachedDashboardStats = CachedDashboardStats(),
    val alerts: List<CachedDashboardAlert> = emptyList(),
    val recentSales: List<CachedRecentSale> = emptyList(),
    val recentPurchases: List<CachedRecentPurchase> = emptyList(),
    val licenseMobile: String? = null,
    val licensePlan: String? = null,
    val licenseDaysRemaining: Int? = null,
    val licenseValidUntil: String? = null,
    val licenseLastVerified: String? = null,
    val licenseExpired: Boolean = false,
    val lastBackupLabel: String? = null,
    val lastBackupEncrypted: Boolean = false,
    val backupDueToday: Boolean = false,
    val backupFailed: Boolean = false,
    val cloudBackupStatus: String = "Not Connected",
    val prescriptionModuleEnabled: Boolean = false,
    val syncStatusLabel: String = "Offline Ready",
    val masterCatalogCount: Int = 271_044,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CachedDashboardStats(
    val todaySales: Double = 0.0,
    val todayPurchase: Double = 0.0,
    val todayExpense: Double = 0.0,
    val profit: Double = 0.0,
    val cashInDrawer: Double = 0.0,
    val bankBalance: Double = 0.0,
    val todaySalesCount: Int = 0,
    val todayPurchaseCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val nearExpiryCount: Int = 0,
    val expiredCount: Int = 0,
    val inventoryValue: Double = 0.0,
    val activeMedicineCount: Int = 0,
    val totalStockUnits: Int = 0,
    val todayCustomers: Int = 0,
    val pendingCustomerDue: Double = 0.0,
    val pendingCustomerDueCount: Int = 0,
    val collectedToday: Double = 0.0,
    val supplierDue: Double = 0.0,
    val supplierDueCount: Int = 0,
    val todaySupplierPayment: Double = 0.0,
    val bestSellingMedicine: String? = null,
    val topCategory: String? = null,
    val todayTransactions: Int = 0,
    val yesterdaySales: Double = 0.0,
    val last7DaysSales: Double = 0.0,
    val last30DaysSales: Double = 0.0,
)

@Serializable
data class CachedDashboardAlert(
    val message: String,
    val severity: String,
    val route: String? = null,
)

@Serializable
data class CachedRecentSale(
    val id: Long,
    val invoiceNumber: String,
    val totalAmount: Double,
    val saleDate: Long,
)

@Serializable
data class CachedRecentPurchase(
    val id: Long,
    val invoiceNumber: String,
    val totalAmount: Double,
    val purchaseDate: Long,
)
