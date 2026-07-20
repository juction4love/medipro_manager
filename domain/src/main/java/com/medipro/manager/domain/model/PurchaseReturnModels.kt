package com.medipro.manager.domain.model

enum class PurchaseReturnReason(val label: String) {
    DAMAGED("Damaged"),
    EXPIRED("Expired"),
    WRONG_SUPPLY("Wrong Supply"),
    WRONG_QUANTITY("Wrong Quantity"),
    MANUFACTURER_RECALL("Manufacturer Recall"),
    OTHER("Other"),
}

data class PurchaseReturn(
    val id: Long = 0,
    val uuid: String = "",
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
    val items: List<PurchaseReturnItem> = emptyList(),
    val syncVersion: Long = 0,
) {
    val grandTotal: Double get() = grandTotalPaisa / 100.0
}

data class PurchaseReturnItem(
    val id: Long = 0,
    val uuid: String = "",
    val purchaseItemId: Long,
    val purchaseItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val medicineName: String = "",
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String = "",
    val quantity: Int,
    val costPricePaisa: Long,
    val amountPaisa: Long,
    val vatPercent: Double = 13.0,
)

/** UI model: one purchase line with return constraints pre-calculated. */
data class PurchaseReturnLine(
    val purchaseItemId: Long,
    val purchaseItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val medicineName: String,
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String,
    val purchasedQty: Int,
    val alreadyReturnedQty: Int,
    val currentBatchStock: Int,
    val unitPrice: Double,
    val vatPercent: Double = 13.0,
    val returnQty: Int = 0,
) {
    val remainingPurchasedQty: Int get() = (purchasedQty - alreadyReturnedQty).coerceAtLeast(0)
    val maxReturnableQty: Int get() = minOf(remainingPurchasedQty, currentBatchStock)
    val lineSubtotal: Double get() = unitPrice * returnQty
    val lineVat: Double get() = lineSubtotal * (vatPercent / 100.0)
    val lineTotal: Double get() = lineSubtotal + lineVat
}

data class PurchaseReturnContext(
    val purchase: Purchase,
    val lines: List<PurchaseReturnLine>,
)
