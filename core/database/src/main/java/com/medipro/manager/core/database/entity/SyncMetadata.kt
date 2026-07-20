package com.medipro.manager.core.database.entity

/**
 * Standard sync fields for every cloud-synced entity.
 * Local [id] (Long) is Room-only; [uuid] is the global identity.
 *
 * Required on: Medicine, Batch, Customer, Supplier, Sale, SaleItem,
 * Purchase, PurchaseItem, Payment, Ledger, Audit.
 */
object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
    const val DELETED = "DELETED"
}

object ScheduleCategory {
    const val OTC = "OTC"
    const val PRESCRIPTION = "PRESCRIPTION"
    const val SCHEDULE_H = "SCHEDULE_H"
    const val SCHEDULE_X = "SCHEDULE_X"
    const val NARCOTIC = "NARCOTIC"
}
