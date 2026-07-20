package com.medipro.manager.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "day_closings",
    indices = [
        Index(value = ["closingDate"], unique = true),
        Index(value = ["uuid"], unique = true),
    ],
)
data class DayClosingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val pharmacyUuid: String = "",
    val closingDate: Long,
    val openingCash: Double,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val esewaSales: Double = 0.0,
    val khaltiSales: Double = 0.0,
    val imeSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val customerReceipts: Double = 0.0,
    val supplierPayments: Double = 0.0,
    val expenses: Double = 0.0,
    val returnsAmount: Double = 0.0,
    val salesCount: Int = 0,
    val returnCount: Int = 0,
    val discountTotal: Double = 0.0,
    val vatTotal: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val differenceReason: String? = null,
    val remarks: String? = null,
    val reference: String = "",
    val pdfPath: String? = null,
    val closedAt: Long = System.currentTimeMillis(),
    val deviceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

object DayClosingDifferenceReason {
    const val SHORT_CASH = "SHORT_CASH"
    const val EXCESS_CASH = "EXCESS_CASH"
}
