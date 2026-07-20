package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_scan_sessions",
    indices = [Index(value = ["scannedAt"])],
)
data class OcrScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scannedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val totalLines: Int = 0,
    val matchedLines: Int = 0,
    val aliasMatchedLines: Int = 0,
    val manualCorrections: Int = 0,
    val avgConfidence: Int = 0,
    val parserUsed: String = "Generic",
    val supplierName: String? = null,
)
