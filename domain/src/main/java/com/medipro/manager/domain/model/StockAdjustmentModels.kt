package com.medipro.manager.domain.model

enum class StockAdjustmentType(val label: String, val emoji: String) {
    STOCK_INCREASE("Stock Increase", "➕"),
    STOCK_DECREASE("Stock Decrease", "➖"),
    PHYSICAL_COUNT("Physical Count", "🔄"),
    DAMAGE("Damage", "💥"),
    EXPIRED("Expired", "⏳"),
    LOST("Lost", "❓"),
    FREE_SAMPLE("Free Sample", "🎁"),
    OPENING_STOCK("Opening Stock", "📦"),
    MANUAL_CORRECTION("Manual Correction", "🔧"),
    ;

    val isIncrease: Boolean
        get() = this in listOf(STOCK_INCREASE, OPENING_STOCK, FREE_SAMPLE)

    val isDecrease: Boolean
        get() = this in listOf(STOCK_DECREASE, MANUAL_CORRECTION, DAMAGE, EXPIRED, LOST)

    val usesNewQty: Boolean get() = this == PHYSICAL_COUNT

    companion object {
        fun fromKey(key: String): StockAdjustmentType =
            entries.find { it.name == key } ?: MANUAL_CORRECTION
    }
}

data class StockAdjustment(
    val id: Long = 0,
    val uuid: String = "",
    val adjustmentNumber: String,
    val medicineId: Long,
    val medicineUuid: String,
    val medicineName: String = "",
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String = "",
    val type: StockAdjustmentType,
    val oldQty: Int,
    val adjustQty: Int,
    val newQty: Int,
    val reason: String,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = 0,
) {
    val lossValue: Double get() = 0.0 // populated by repository when needed
}

data class BatchStockDetail(
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String,
    val expiryDate: Long,
    val sellableQty: Int,
    val damagedQty: Int,
    val purchasePrice: Double,
    val isExpired: Boolean,
)

data class InventoryMedicineStock(
    val medicine: Medicine,
    val totalQty: Int,
    val batches: List<BatchStockDetail>,
)

data class StockAdjustmentContext(
    val medicine: Medicine,
    val batch: BatchStockDetail,
)

data class InventorySummary(
    val inventoryValue: Double,
    val totalStockUnits: Int,
    val activeMedicineCount: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val nearExpiryCount: Int,
    val expiredCount: Int,
    val todayAdjustments: Int,
)

data class DamageReportRow(
    val medicineName: String,
    val batchNumber: String,
    val quantity: Int,
    val lossValue: Double,
    val reason: String,
    val date: Long,
)

data class ExpiryReportRow(
    val medicineName: String,
    val batchNumber: String,
    val expiryDate: Long,
    val remainingQty: Int,
)
