package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sale_returns",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["invoiceUuid"]),
        Index(value = ["saleId"]),
        Index(value = ["returnDate"]),
    ],
)
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val saleId: Long,
    val invoiceUuid: String,
    val customerId: Long? = null,
    val customerUuid: String? = null,
    val returnNumber: String,
    val reason: String,
    val returnDate: Long = System.currentTimeMillis(),
    val subtotalPaisa: Long = 0,
    val discountPaisa: Long = 0,
    val vatPaisa: Long = 0,
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
    tableName = "sale_return_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleReturnId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["saleReturnId"]),
        Index(value = ["invoiceItemUuid"]),
    ],
)
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val saleReturnId: Long,
    val saleReturnUuid: String,
    val saleItemId: Long,
    val invoiceItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val batchId: Long,
    val batchUuid: String,
    val quantity: Int,
    val sellingPricePaisa: Long,
    val amountPaisa: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)
