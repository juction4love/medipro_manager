package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchase_returns",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["purchaseUuid"]),
        Index(value = ["purchaseId"]),
        Index(value = ["returnDate"]),
    ],
)
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val purchaseId: Long,
    val purchaseUuid: String,
    val supplierId: Long? = null,
    val supplierUuid: String? = null,
    val returnNumber: String,
    val reason: String,
    val returnDate: Long = System.currentTimeMillis(),
    val subtotalPaisa: Long = 0,
    val vatPaisa: Long = 0,
    val discountPaisa: Long = 0,
    val grandTotalPaisa: Long = 0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)

@Entity(
    tableName = "purchase_return_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseReturnId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PurchaseItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["purchaseReturnId"]),
        Index(value = ["purchaseItemUuid"]),
    ],
)
data class PurchaseReturnItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val purchaseReturnId: Long,
    val purchaseReturnUuid: String,
    val purchaseItemId: Long,
    val purchaseItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val batchId: Long,
    val batchUuid: String,
    val quantity: Int,
    val costPricePaisa: Long,
    val amountPaisa: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)
