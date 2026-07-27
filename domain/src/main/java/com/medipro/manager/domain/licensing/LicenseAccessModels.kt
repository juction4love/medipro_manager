package com.medipro.manager.domain.licensing

enum class LicenseAccessState {
    VALID,
    EXPIRING_SOON,
    EXPIRED,
    NO_LICENSE,
}

/**
 * Pro-only capabilities. Free plan covers daily pharmacy workflow without these.
 *
 * Upgrade popup must only appear when user attempts one of these — never during
 * manual purchase, sales, or stock update.
 */
enum class PremiumFeature {
    /** Scan purchase invoice (OCR). */
    OCR_PURCHASE,
    /** Cloud backup and restore. */
    BACKUP_RESTORE,
    /** Firestore multi-device sync. */
    CLOUD_SYNC,
    /** Financial / medicine analytics tabs beyond basic sales/purchase/inventory. */
    ADVANCED_REPORTS,
    /** Profit & loss and analytics dashboards. */
    PROFIT_ANALYTICS,
    /** Staff accounts and role-based access. */
    MULTI_USER,
    /** Bulk barcode catalog import. */
    BARCODE_BULK_IMPORT,
    /** Audit log export to Excel/PDF. */
    AUDIT_EXPORT,
    /** OCR alias learning and feedback (future AI). */
    OCR_LEARNING,
}

/** Product editions — same codebase, different licensing via build flavor / config. */
enum class MediProEdition {
    /** Play Store: Free + Pro subscription (freemium). */
    COMMUNITY,
    /** Paid subscription required to use the app. */
    PROFESSIONAL,
    /** Distributor-issued offline license (.lic / QR). */
    ENTERPRISE,
}

object FreemiumPlan {
    const val FREE = "FREE"
    const val PRO = "PRO"

    val freeCapabilities = listOf(
        "Manual Purchase",
        "Sales / POS",
        "Stock Management",
        "Purchase Return",
        "Sales Return",
        "Stock Adjustment",
        "Batch & FEFO",
        "Basic Dashboard",
        "Basic Reports",
        "Local Database",
    )

    val proCapabilities = listOf(
        "OCR Purchase Invoice",
        "Cloud Backup / Restore",
        "Cloud Sync",
        "Advanced Reports",
        "Profit Analytics",
        "Multi-user / Staff Roles",
        "Audit Export (Excel/PDF)",
        "Barcode Bulk Import",
        "OCR / AI Learning",
        "Priority Support",
    )

    /** Features that may show the subscription screen when tapped without Pro. */
    val upgradeTriggers = listOf(
        PremiumFeature.OCR_PURCHASE,
        PremiumFeature.BACKUP_RESTORE,
        PremiumFeature.ADVANCED_REPORTS,
        PremiumFeature.PROFIT_ANALYTICS,
        PremiumFeature.MULTI_USER,
        PremiumFeature.CLOUD_SYNC,
    )
}
