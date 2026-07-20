package com.medipro.manager.domain.model

data class StockBatch(
    val batchId: Long,
    val batchUuid: String = "",
    val batchNumber: String,
    val expiryDate: Long,
    val availableQuantity: Int,
    val sellingPrice: Double
)

data class PosCartItem(
    val medicineId: Long,
    val medicineUuid: String = "",
    val medicineName: String,
    val batchId: Long,
    val batchUuid: String = "",
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int,
    val unitPrice: Double,
    val mrp: Double = 0.0,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
    val availableStock: Int,
    val requiresPrescription: Boolean = false,
    val scheduleCategory: String = "OTC"
) {
    val lineSubtotal: Double get() = (unitPrice * quantity) - discount
    val vatAmount: Double get() = lineSubtotal * (vatPercent / 100.0)
    val lineTotal: Double get() = lineSubtotal + vatAmount
    val savingsFromMrp: Double get() = if (mrp > 0) (mrp - unitPrice).coerceAtLeast(0.0) * quantity else 0.0
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    CARD("Card"),
    ESEWA("eSewa"),
    KHALTI("Khalti"),
    IME_PAY("IME Pay"),
    CREDIT("Credit"),
    MIXED("Mixed"),
}

fun PosCartItem.toSaleItem(): SaleItem = SaleItem(
    medicineId = medicineId,
    medicineUuid = medicineUuid,
    medicineName = medicineName,
    batchId = batchId,
    batchUuid = batchUuid,
    batchNumber = batchNumber,
    quantity = quantity,
    unitPrice = unitPrice,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = lineTotal
)

data class PurchaseCartItem(
    val medicineId: Long,
    val medicineName: String,
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int,
    val unitPrice: Double,
    val sellingPrice: Double = 0.0,
    val mrp: Double = 0.0,
    val discount: Double = 0.0,
    val vatPercent: Double = 13.0,
) {
    val lineSubtotal: Double get() = (unitPrice * quantity) - discount
    val vatAmount: Double get() = lineSubtotal * (vatPercent / 100.0)
    val lineTotal: Double get() = lineSubtotal + vatAmount
}

fun mergePurchaseCartItems(items: List<PurchaseCartItem>): List<PurchaseCartItem> =
    items.groupBy { it.medicineId to it.batchNumber.uppercase() }
        .values
        .map { group ->
            group.reduce { acc, item ->
                acc.copy(
                    quantity = acc.quantity + item.quantity,
                    unitPrice = item.unitPrice,
                    sellingPrice = maxOf(acc.sellingPrice, item.sellingPrice),
                    mrp = maxOf(acc.mrp, item.mrp),
                )
            }
        }

fun mergePurchaseCart(existing: List<PurchaseCartItem>, incoming: List<PurchaseCartItem>): List<PurchaseCartItem> =
    mergePurchaseCartItems(existing + incoming)

fun PurchaseCartItem.toPurchaseItem(): PurchaseItem = PurchaseItem(
    medicineId = medicineId,
    medicineName = medicineName,
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    quantity = quantity,
    unitPrice = unitPrice,
    sellingPrice = sellingPrice,
    mrp = mrp,
    discount = discount,
    vatPercent = vatPercent,
    totalPrice = lineTotal
)
