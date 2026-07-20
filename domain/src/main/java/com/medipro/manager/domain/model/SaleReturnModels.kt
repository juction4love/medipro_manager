package com.medipro.manager.domain.model

enum class SaleReturnReason(val label: String) {
    DAMAGED("Damaged"),
    WRONG_MEDICINE("Wrong Medicine"),
    CUSTOMER_RETURN("Customer Return"),
    EXPIRED("Expired"),
    RECALL("Recall"),
    OTHER("Other"),
}

data class SaleReturn(
    val id: Long = 0,
    val uuid: String = "",
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
    val items: List<SaleReturnItem> = emptyList(),
    val syncVersion: Long = 0,
) {
    val grandTotal: Double get() = grandTotalPaisa / 100.0
}

data class SaleReturnItem(
    val id: Long = 0,
    val uuid: String = "",
    val saleItemId: Long,
    val invoiceItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val medicineName: String = "",
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String = "",
    val quantity: Int,
    val sellingPricePaisa: Long,
    val amountPaisa: Long,
)

data class SaleReturnLine(
    val saleItemId: Long,
    val invoiceItemUuid: String,
    val medicineId: Long,
    val medicineUuid: String,
    val medicineName: String,
    val batchId: Long,
    val batchUuid: String,
    val batchNumber: String,
    val soldQty: Int,
    val alreadyReturnedQty: Int,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val returnQty: Int = 0,
) {
    val remainingSoldQty: Int get() = (soldQty - alreadyReturnedQty).coerceAtLeast(0)
    val maxReturnableQty: Int get() = remainingSoldQty
    val lineSubtotal: Double get() = unitPrice * returnQty
    val lineVat: Double get() = lineSubtotal * (vatPercent / 100.0)
    val lineTotal: Double get() = lineSubtotal + lineVat
}

data class SaleReturnContext(
    val sale: Sale,
    val lines: List<SaleReturnLine>,
)
