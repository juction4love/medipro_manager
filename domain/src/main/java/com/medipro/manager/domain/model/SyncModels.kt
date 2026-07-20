package com.medipro.manager.domain.model

/** Firestore-synced entity types. Master catalog (271K) is NEVER synced. */
object SyncEntityType {
    const val INVOICE = "INVOICE"
    const val PURCHASE = "PURCHASE"
    const val PURCHASE_RETURN = "PURCHASE_RETURN"
    const val SALE_RETURN = "SALE_RETURN"
    const val STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT"
    const val MEDICINE = "MEDICINE"
    const val STOCK_BATCH = "STOCK_BATCH"
    const val CUSTOMER = "CUSTOMER"
    const val SUPPLIER = "SUPPLIER"
    const val PAYMENT = "PAYMENT"
    const val LEDGER = "LEDGER"
    const val SETTINGS = "SETTINGS"
    const val AUDIT_LOG = "AUDIT_LOG"
}

/** Shared cloud sync metadata carried by every synced entity document. */
data class SyncMetadataFields(
    val uuid: String,
    val pharmacyUuid: String,
    val branchUuid: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val createdBy: String? = null,
    val deviceId: String? = null,
    val syncVersion: Long,
    val syncStatus: String,
)

object SyncOperationType {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

data class SyncStatusSnapshot(
    val pendingCount: Int = 0,
    val isCloudEnabled: Boolean = false,
    val lastSyncLabel: String = "Offline Ready",
    val cloudStatusLabel: String = "Not Connected",
)

data class AuditLog(
    val uuid: String,
    val eventType: String,
    val entityType: String,
    val entityUuid: String? = null,
    val description: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PendingOperation(
    val uuid: String,
    val operationType: String,
    val entityType: String,
    val entityUuid: String,
    val payloadJson: String,
    val status: String = "PENDING",
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
