package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stock_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["medicineUuid"]),
        Index(value = ["batchUuid"]),
        Index(value = ["type"]),
        Index(value = ["createdAt"]),
    ],
)
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val adjustmentNumber: String,
    val medicineId: Long,
    val medicineUuid: String,
    val batchId: Long,
    val batchUuid: String,
    val type: String,
    val oldQty: Int,
    val adjustQty: Int,
    val newQty: Int,
    val reason: String,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)

object StockAdjustmentType {
    const val STOCK_INCREASE = "STOCK_INCREASE"
    const val STOCK_DECREASE = "STOCK_DECREASE"
    const val PHYSICAL_COUNT = "PHYSICAL_COUNT"
    const val DAMAGE = "DAMAGE"
    const val EXPIRED = "EXPIRED"
    const val LOST = "LOST"
    const val FREE_SAMPLE = "FREE_SAMPLE"
    const val OPENING_STOCK = "OPENING_STOCK"
    const val MANUAL_CORRECTION = "MANUAL_CORRECTION"
}
