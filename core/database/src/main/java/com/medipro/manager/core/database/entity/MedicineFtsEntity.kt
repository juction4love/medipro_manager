package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MedicineEntity::class)
@Entity(tableName = "medicines_fts")
data class MedicineFtsEntity(
    val brandName: String,
    val genericName: String,
    val composition: String,
    val strength: String,
    val manufacturer: String,
    val barcode: String?
)
