package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_medicine_aliases",
    indices = [
        Index(value = ["normalizedText"], unique = true),
        Index(value = ["medicineId"]),
    ],
)
data class OcrMedicineAliasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val normalizedText: String,
    val ocrText: String,
    val medicineId: Long,
    val medicineUuid: String? = null,
    val medicineName: String,
    val hitCount: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
