package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["purchaseDate"]),
        Index(value = ["purchaseDate", "deletedAt"]),
        Index(value = ["uuid"], unique = true),
        Index(value = ["supplierUuid"]),
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val supplierId: Long?,
    val supplierUuid: String? = null,
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["medicineId"]),
        Index(value = ["uuid"], unique = true),
    ]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val purchaseId: Long,
    val medicineId: Long,
    val medicineUuid: String,
    val batchUuid: String? = null,
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val totalPrice: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)
