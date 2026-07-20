package com.medipro.manager.data.sync

import com.medipro.manager.domain.model.SyncEntityType

/**
 * Firestore collection layout:
 * pharmacies/{pharmacyId}/invoices/{uuid}
 * pharmacies/{pharmacyId}/purchases/{uuid}
 * ...
 *
 * 271K master catalog is NEVER synced — local asset/catalog.db only.
 */
object FirestorePaths {
    const val ROOT = "pharmacies"

    fun collection(pharmacyId: String, entityType: String): String {
        val sub = when (entityType) {
            SyncEntityType.INVOICE -> "invoices"
            SyncEntityType.PURCHASE -> "purchases"
            SyncEntityType.PURCHASE_RETURN -> "purchase_returns"
            SyncEntityType.SALE_RETURN -> "sale_returns"
            SyncEntityType.STOCK_ADJUSTMENT -> "stock_adjustments"
            SyncEntityType.MEDICINE -> "medicines"
            SyncEntityType.STOCK_BATCH -> "stock_batches"
            SyncEntityType.CUSTOMER -> "customers"
            SyncEntityType.SUPPLIER -> "suppliers"
            SyncEntityType.PAYMENT -> "payments"
            SyncEntityType.LEDGER -> "ledger"
            SyncEntityType.SETTINGS -> "settings"
            SyncEntityType.AUDIT_LOG -> "audit_logs"
            else -> entityType.lowercase()
        }
        return "$ROOT/$pharmacyId/$sub"
    }
}
