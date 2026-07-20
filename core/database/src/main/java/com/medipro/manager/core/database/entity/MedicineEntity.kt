package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "medicines",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["catalogUuid"]),
        Index(value = ["barcode"], unique = true),
        Index(value = ["brandName"]),
        Index(value = ["genericName"]),
        Index(value = ["composition"]),
        Index(value = ["manufacturer"]),
        Index(value = ["syncStatus"])
    ]
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    /** Links to master catalog entry when imported from catalog.db */
    val catalogUuid: String? = null,
    val pharmacyUuid: String = "",
    val branchUuid: String? = null,
    val brandName: String,
    val genericName: String,
    val composition: String = "",
    val strength: String = "",
    val dosageForm: String = "Tablet",
    val manufacturer: String = "",
    val category: String = "General",
    val barcode: String? = null,
    val unit: String = "pcs",
    val purchasePricePaisa: Long = 0,
    val sellingPricePaisa: Long = 0,
    val mrpPaisa: Long = 0,
    val vatPercent: Double = 13.0,
    val reorderLevel: Int = 10,
    val description: String? = null,
    val requiresPrescription: Boolean = false,
    val controlledSubstance: Boolean = false,
    val scheduleCategory: String = ScheduleCategory.OTC,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = SyncStatus.PENDING,
    val syncVersion: Long = 0,
    val deviceId: String? = null,
    val createdBy: String? = null,
)
