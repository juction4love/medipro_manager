package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["entityType", "entityUuid"]),
        Index(value = ["eventType"]),
        Index(value = ["createdAt"]),
        Index(value = ["uuid"], unique = true),
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val eventType: String,
    val entityType: String,
    val entityUuid: String?,
    val entityLocalId: Long? = null,
    val description: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val deviceId: String? = null,
    val userId: String? = null,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
)

object AuditEventType {
    const val MEDICINE_CREATED = "MEDICINE_CREATED"
    const val MEDICINE_UPDATED = "MEDICINE_UPDATED"
    const val MEDICINE_DELETED = "MEDICINE_DELETED"
    const val PRICE_CHANGED = "PRICE_CHANGED"
    const val PURCHASE_CREATED = "PURCHASE_CREATED"
    const val PURCHASE_RETURN = "PURCHASE_RETURN"
    const val PURCHASE_DELETED = "PURCHASE_DELETED"
    const val SALE_CREATED = "SALE_CREATED"
    const val SALE_UPDATED = "SALE_UPDATED"
    const val SALE_CANCELLED = "SALE_CANCELLED"
    const val SALE_RETURN = "SALE_RETURN"
    const val SALE_RETURN_CREATED = "SALE_RETURN_CREATED"
    const val CUSTOMER_CREATED = "CUSTOMER_CREATED"
    const val STOCK_ADJUSTED = "STOCK_ADJUSTED"
    const val STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT"
    const val DAMAGE = "DAMAGE"
    const val EXPIRED = "EXPIRED"
    const val OPENING_STOCK = "OPENING_STOCK"
    const val PHYSICAL_COUNT = "PHYSICAL_COUNT"
    const val LOGIN = "LOGIN"
    const val BACKUP = "BACKUP"
    const val RESTORE = "RESTORE"
    const val LICENSE_ACTIVATED = "LICENSE_ACTIVATED"
    const val EXPENSE_RECORDED = "EXPENSE_RECORDED"
    const val DAY_CLOSED = "DAY_CLOSED"
    const val CUSTOMER_RECEIPT = "CUSTOMER_RECEIPT"
    const val SUPPLIER_PAYMENT = "SUPPLIER_PAYMENT"
}
