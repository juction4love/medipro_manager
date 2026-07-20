package com.medipro.manager.domain.model

data class Medicine(
    val id: Long = 0,
    val uuid: String = "",
    val brandName: String,
    val genericName: String,
    val composition: String = "",
    val strength: String = "",
    val dosageForm: String = "Tablet",
    val manufacturer: String = "",
    val category: String = "General",
    val barcode: String? = null,
    val unit: String = "pcs",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val vatPercent: Double = 13.0,
    val reorderLevel: Int = 10,
    val description: String? = null,
    val requiresPrescription: Boolean = false,
    val controlledSubstance: Boolean = false,
    val scheduleCategory: String = "OTC",
    val isActive: Boolean = true,
    val stockQuantity: Int = 0,
    val syncStatus: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Primary display label in lists and POS */
    val name: String get() = brandName
}

data class Supplier(
    val id: Long = 0,
    val uuid: String = "",
    val name: String,
    val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val panNumber: String? = null,
    val creditLimit: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val isActive: Boolean = true
)

data class Customer(
    val id: Long = 0,
    val uuid: String = "",
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val creditLimit: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val isActive: Boolean = true
)

data class Batch(
    val id: Long = 0,
    val uuid: String = "",
    val medicineId: Long,
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val supplierId: Long? = null
)

data class DashboardStats(
    // Financial
    val todaySales: Double = 0.0,
    val todayPurchase: Double = 0.0,
    val todayPurchaseReturn: Double = 0.0,
    val todaySalesReturn: Double = 0.0,
    val netSales: Double = 0.0,
    val todayExpense: Double = 0.0,
    val profit: Double = 0.0,
    val cashInDrawer: Double = 0.0,
    val bankBalance: Double = 0.0,
    val todaySalesCount: Int = 0,
    val todayPurchaseCount: Int = 0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    // Inventory
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val nearExpiryCount: Int = 0,
    val expiredCount: Int = 0,
    val inventoryValue: Double = 0.0,
    val activeMedicineCount: Int = 0,
    val totalStockUnits: Int = 0,
    val todayAdjustments: Int = 0,
    // Customer
    val todayCustomers: Int = 0,
    val pendingCustomerDue: Double = 0.0,
    val pendingCustomerDueCount: Int = 0,
    val collectedToday: Double = 0.0,
    // Supplier
    val supplierDue: Double = 0.0,
    val supplierDueCount: Int = 0,
    val todaySupplierPayment: Double = 0.0,
    // Business
    val bestSellingMedicine: String? = null,
    val topCategory: String? = null,
    val todayTransactions: Int = 0,
    // Legacy / analytics
    val expiringCount: Int = 0,
    val pendingPurchaseCount: Int = 0,
    val pendingPayments: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val last7DaysSales: Double = 0.0,
    val last30DaysSales: Double = 0.0,
    val ocrAnalytics: OcrAnalytics = OcrAnalytics(),
)

data class DashboardAlert(
    val message: String,
    val severity: AlertSeverity = AlertSeverity.WARNING,
    val route: String? = null,
)

enum class AlertSeverity { INFO, WARNING, CRITICAL, OK }

data class DashboardSnapshot(
    val pharmacyName: String = "",
    val dateLabel: String = "",
    val stats: DashboardStats = DashboardStats(),
    val alerts: List<DashboardAlert> = emptyList(),
    val recentSales: List<Sale> = emptyList(),
    val recentPurchases: List<Purchase> = emptyList(),
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
    val cloudBackupConnected: Boolean = false,
    val cloudBackupStatus: String = "Not Connected",
    val prescriptionModuleEnabled: Boolean = false,
    val syncStatusLabel: String = "Offline Ready",
    val masterCatalogCount: Int = 271_044,
    val isFromCache: Boolean = false,
    val cachedAt: Long? = null,
)

data class Sale(
    val id: Long = 0,
    val uuid: String = "",
    val customerId: Long? = null,
    val customerName: String? = null,
    val invoiceNumber: String,
    val saleDate: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val vatAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PAID",
    val paymentMethod: String = "CASH",
    val isCredit: Boolean = false,
    val prescriptionNumber: String? = null,
    val doctorName: String? = null,
    val patientName: String? = null,
    val items: List<SaleItem> = emptyList(),
    val printCount: Int = 0,
    val lastPrintedAt: Long? = null,
    val syncVersion: Long = 0,
    val isCancelled: Boolean = false,
)

data class SaleItem(
    val id: Long = 0,
    val uuid: String = "",
    val saleId: Long = 0,
    val medicineId: Long,
    val medicineUuid: String = "",
    val medicineName: String = "",
    val batchId: Long,
    val batchUuid: String = "",
    val batchNumber: String = "",
    val expiryDate: Long? = null,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val totalPrice: Double
)

data class Purchase(
    val id: Long = 0,
    val uuid: String = "",
    val supplierId: Long?,
    val supplierName: String? = null,
    val invoiceNumber: String,
    val purchaseDate: Long = System.currentTimeMillis(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val vatAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PENDING",
    val paymentMethod: String = "CASH",
    val notes: String? = null,
    val items: List<PurchaseItem> = emptyList(),
    val syncVersion: Long = 0,
)

data class PurchaseItem(
    val id: Long = 0,
    val uuid: String = "",
    val purchaseId: Long = 0,
    val medicineId: Long,
    val medicineUuid: String = "",
    val medicineName: String = "",
    val batchUuid: String = "",
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int,
    val unitPrice: Double,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val totalPrice: Double
)

data class License(
    val licenseId: String,
    val licenseKey: String,
    val mobileNumber: String,
    val pharmacyName: String,
    val ownerName: String,
    val deviceId: String,
    val plan: String = LicensePlan.FREE,
    val status: String = LicenseStatus.ACTIVE,
    val activatedAt: Long,
    val expiresAt: Long,
    val lastVerifiedAt: Long? = null,
    val isActive: Boolean = true,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    val daysRemaining: Int get() = ((expiresAt - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
}

object LicensePlan {
    const val FREE = "FREE"
}

object LicenseStatus {
    const val ACTIVE = "ACTIVE"
    const val EXPIRED = "EXPIRED"
    const val REVOKED = "REVOKED"
    const val PENDING_TRANSFER = "PENDING_TRANSFER"
}

enum class LicenseActivationResult {
    ACTIVATED,
    DEVICE_MISMATCH,
    EXPIRED,
    REVOKED,
    NETWORK_ERROR,
    INVALID_OTP,
}

data class PharmacySettings(
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val pharmacyPhone: String = "",
    val pharmacyEmail: String = "",
    val panNumber: String = "",
    val vatNumber: String = "",
    val currency: String = "NPR",
    val language: String = "ne",
    val theme: String = "SYSTEM",
    val autoBackupEnabled: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val lowStockThreshold: Int = 10,
    val expiryAlertDays: Int = 90,
    val prescriptionModuleEnabled: Boolean = true,
    val requirePrescriptionDetails: Boolean = true,
    val ocrFeedbackOptIn: Boolean = false,
)
