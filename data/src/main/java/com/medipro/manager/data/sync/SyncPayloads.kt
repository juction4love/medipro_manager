package com.medipro.manager.data.sync

import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseReturn
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleReturn
import com.medipro.manager.domain.model.StockAdjustment
import kotlinx.serialization.Serializable

@Serializable
internal data class InvoicePayload(
    val invoiceNumber: String,
    val customerUuid: String? = null,
    val subtotal: Double,
    val discount: Double,
    val vatAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val isCredit: Boolean,
    val items: List<InvoiceItemPayload>,
)

@Serializable
internal data class InvoiceItemPayload(
    val medicineUuid: String,
    val batchUuid: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
)

@Serializable
internal data class PurchasePayload(
    val invoiceNumber: String,
    val supplierUuid: String? = null,
    val subtotal: Double,
    val discount: Double,
    val vatAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val items: List<PurchaseItemPayload>,
)

@Serializable
internal data class PurchaseItemPayload(
    val medicineUuid: String,
    val batchUuid: String? = null,
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
)

internal suspend fun SyncUuidResolver.toInvoicePayload(sale: Sale): InvoicePayload {
    val customerUuid = sale.customerId?.let { customerUuid(it) }
    val items = sale.items.map { item ->
        val medUuid = item.medicineUuid.ifBlank { medicineUuid(item.medicineId).orEmpty() }
        val batUuid = item.batchUuid.ifBlank { batchUuid(item.batchId).orEmpty() }
        InvoiceItemPayload(
            medicineUuid = medUuid,
            batchUuid = batUuid,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            totalPrice = item.totalPrice,
        )
    }
    return InvoicePayload(
        invoiceNumber = sale.invoiceNumber,
        customerUuid = customerUuid,
        subtotal = sale.subtotal,
        discount = sale.discount,
        vatAmount = sale.vatAmount,
        totalAmount = sale.totalAmount,
        paidAmount = sale.paidAmount,
        paymentStatus = sale.paymentStatus,
        paymentMethod = sale.paymentMethod,
        isCredit = sale.isCredit,
        items = items,
    )
}

internal suspend fun SyncUuidResolver.toPurchasePayload(purchase: Purchase): PurchasePayload {
    val supUuid = purchase.supplierId?.let { supplierUuid(it) }
    val items = purchase.items.map { item ->
        val medUuid = item.medicineUuid.ifBlank { medicineUuid(item.medicineId).orEmpty() }
        val batUuid = item.batchUuid.ifBlank { null }
        PurchaseItemPayload(
            medicineUuid = medUuid,
            batchUuid = batUuid,
            batchNumber = item.batchNumber,
            expiryDate = item.expiryDate,
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            totalPrice = item.totalPrice,
        )
    }
    return PurchasePayload(
        invoiceNumber = purchase.invoiceNumber,
        supplierUuid = supUuid,
        subtotal = purchase.subtotal,
        discount = purchase.discount,
        vatAmount = purchase.vatAmount,
        totalAmount = purchase.totalAmount,
        paidAmount = purchase.paidAmount,
        paymentStatus = purchase.paymentStatus,
        paymentMethod = purchase.paymentMethod,
        items = items,
    )
}

@Serializable
internal data class PurchaseReturnPayload(
    val returnNumber: String,
    val purchaseUuid: String,
    val supplierUuid: String? = null,
    val reason: String,
    val subtotalPaisa: Long,
    val vatPaisa: Long,
    val discountPaisa: Long,
    val grandTotalPaisa: Long,
    val items: List<PurchaseReturnItemPayload>,
)

@Serializable
internal data class PurchaseReturnItemPayload(
    val purchaseItemUuid: String,
    val medicineUuid: String,
    val batchUuid: String,
    val quantity: Int,
    val costPricePaisa: Long,
    val amountPaisa: Long,
)

internal fun PurchaseReturn.toPayload(): PurchaseReturnPayload = PurchaseReturnPayload(
    returnNumber = returnNumber,
    purchaseUuid = purchaseUuid,
    supplierUuid = supplierUuid,
    reason = reason,
    subtotalPaisa = subtotalPaisa,
    vatPaisa = vatPaisa,
    discountPaisa = discountPaisa,
    grandTotalPaisa = grandTotalPaisa,
    items = items.map {
        PurchaseReturnItemPayload(
            purchaseItemUuid = it.purchaseItemUuid,
            medicineUuid = it.medicineUuid,
            batchUuid = it.batchUuid,
            quantity = it.quantity,
            costPricePaisa = it.costPricePaisa,
            amountPaisa = it.amountPaisa,
        )
    },
)

@Serializable
internal data class SaleReturnPayload(
    val returnNumber: String,
    val invoiceUuid: String,
    val customerUuid: String? = null,
    val reason: String,
    val subtotalPaisa: Long,
    val discountPaisa: Long,
    val vatPaisa: Long,
    val grandTotalPaisa: Long,
    val items: List<SaleReturnItemPayload>,
)

@Serializable
internal data class SaleReturnItemPayload(
    val invoiceItemUuid: String,
    val medicineUuid: String,
    val batchUuid: String,
    val quantity: Int,
    val sellingPricePaisa: Long,
    val amountPaisa: Long,
)

internal fun SaleReturn.toPayload(): SaleReturnPayload = SaleReturnPayload(
    returnNumber = returnNumber,
    invoiceUuid = invoiceUuid,
    customerUuid = customerUuid,
    reason = reason,
    subtotalPaisa = subtotalPaisa,
    discountPaisa = discountPaisa,
    vatPaisa = vatPaisa,
    grandTotalPaisa = grandTotalPaisa,
    items = items.map {
        SaleReturnItemPayload(
            invoiceItemUuid = it.invoiceItemUuid,
            medicineUuid = it.medicineUuid,
            batchUuid = it.batchUuid,
            quantity = it.quantity,
            sellingPricePaisa = it.sellingPricePaisa,
            amountPaisa = it.amountPaisa,
        )
    },
)

@Serializable
internal data class StockAdjustmentPayload(
    val adjustmentNumber: String,
    val medicineUuid: String,
    val batchUuid: String,
    val type: String,
    val oldQty: Int,
    val adjustQty: Int,
    val newQty: Int,
    val reason: String,
    val remarks: String? = null,
)

internal fun StockAdjustment.toPayload(): StockAdjustmentPayload = StockAdjustmentPayload(
    adjustmentNumber = adjustmentNumber,
    medicineUuid = medicineUuid,
    batchUuid = batchUuid,
    type = type.name,
    oldQty = oldQty,
    adjustQty = adjustQty,
    newQty = newQty,
    reason = reason,
    remarks = remarks,
)

@Serializable
internal data class CustomerPayload(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val creditLimit: Double,
    val outstandingBalance: Double,
    val isActive: Boolean,
)

@Serializable
internal data class SupplierPayload(
    val name: String,
    val contactPerson: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val panNumber: String? = null,
    val creditLimit: Double,
    val outstandingBalance: Double,
    val isActive: Boolean,
)

@Serializable
internal data class PaymentPayload(
    val type: String,
    val referenceUuid: String,
    val amount: Double,
    val paymentMethod: String,
    val paymentDate: Long,
    val notes: String? = null,
)

@Serializable
internal data class LedgerPayload(
    val accountType: String,
    val accountUuid: String? = null,
    val description: String,
    val debit: Double,
    val credit: Double,
    val balance: Double,
    val referenceType: String? = null,
    val referenceUuid: String? = null,
    val entryDate: Long,
)

internal fun com.medipro.manager.core.database.entity.CustomerEntity.toPayload() = CustomerPayload(
    name = name,
    phone = phone,
    email = email,
    address = address,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive,
)

internal fun com.medipro.manager.core.database.entity.SupplierEntity.toPayload() = SupplierPayload(
    name = name,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    address = address,
    panNumber = panNumber,
    creditLimit = creditLimit,
    outstandingBalance = outstandingBalance,
    isActive = isActive,
)

internal fun com.medipro.manager.core.database.entity.PaymentEntity.toPayload(referenceUuid: String) = PaymentPayload(
    type = type,
    referenceUuid = referenceUuid,
    amount = amount,
    paymentMethod = paymentMethod,
    paymentDate = paymentDate,
    notes = notes,
)

internal fun com.medipro.manager.core.database.entity.LedgerEntity.toPayload(
    accountUuid: String?,
    referenceUuid: String?,
) = LedgerPayload(
    accountType = accountType,
    accountUuid = accountUuid,
    description = description,
    debit = debit,
    credit = credit,
    balance = balance,
    referenceType = referenceType,
    referenceUuid = referenceUuid,
    entryDate = entryDate,
)

@Serializable
internal data class StockBatchPayload(
    val medicineUuid: String,
    val batchNumber: String,
    val expiryDate: Long,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val damagedQuantity: Int = 0,
    val supplierUuid: String? = null,
    val barcode: String? = null,
)

@Serializable
internal data class MedicineSyncPayload(
    val brandName: String,
    val genericName: String,
    val composition: String = "",
    val strength: String = "",
    val dosageForm: String = "Tablet",
    val manufacturer: String = "",
    val category: String = "General",
    val barcode: String? = null,
    val unit: String = "pcs",
    val purchasePricePaisa: Long,
    val sellingPricePaisa: Long,
    val mrpPaisa: Long,
    val vatPercent: Double,
    val reorderLevel: Int,
    val requiresPrescription: Boolean,
    val controlledSubstance: Boolean,
    val scheduleCategory: String,
    val isActive: Boolean,
    val catalogUuid: String? = null,
)

internal fun com.medipro.manager.core.database.entity.BatchEntity.toStockBatchPayload(
    medicineUuid: String,
    quantity: Int,
    damagedQuantity: Int,
    supplierUuid: String?,
) = StockBatchPayload(
    medicineUuid = medicineUuid,
    batchNumber = batchNumber,
    expiryDate = expiryDate,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    quantity = quantity,
    damagedQuantity = damagedQuantity,
    supplierUuid = supplierUuid,
    barcode = barcode,
)

internal fun com.medipro.manager.core.database.entity.MedicineEntity.toSyncPayload() = MedicineSyncPayload(
    brandName = brandName,
    genericName = genericName,
    composition = composition,
    strength = strength,
    dosageForm = dosageForm,
    manufacturer = manufacturer,
    category = category,
    barcode = barcode,
    unit = unit,
    purchasePricePaisa = purchasePricePaisa,
    sellingPricePaisa = sellingPricePaisa,
    mrpPaisa = mrpPaisa,
    vatPercent = vatPercent,
    reorderLevel = reorderLevel,
    requiresPrescription = requiresPrescription,
    controlledSubstance = controlledSubstance,
    scheduleCategory = scheduleCategory,
    isActive = isActive,
    catalogUuid = catalogUuid,
)

internal fun com.medipro.manager.core.database.entity.StockAdjustmentEntity.toPayload() = StockAdjustmentPayload(
    adjustmentNumber = adjustmentNumber,
    medicineUuid = medicineUuid,
    batchUuid = batchUuid,
    type = type,
    oldQty = oldQty,
    adjustQty = adjustQty,
    newQty = newQty,
    reason = reason,
    remarks = remarks,
)

@Serializable
internal data class SettingsPayload(
    val pharmacyName: String,
    val pharmacyAddress: String,
    val pharmacyPhone: String,
    val pharmacyEmail: String,
    val panNumber: String,
    val vatNumber: String,
    val currency: String,
    val language: String,
    val theme: String,
    val printerName: String? = null,
    val autoBackupEnabled: Boolean,
    val autoBackupIntervalDays: Int,
    val appLockEnabled: Boolean,
    val biometricEnabled: Boolean,
    val lowStockThreshold: Int,
    val expiryAlertDays: Int,
    val prescriptionModuleEnabled: Boolean,
    val requirePrescriptionDetails: Boolean,
)

@Serializable
internal data class AuditLogPayload(
    val eventType: String,
    val entityType: String,
    val entityUuid: String? = null,
    val description: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val createdAt: Long,
)

internal fun com.medipro.manager.core.database.entity.SettingsEntity.toPayload() = SettingsPayload(
    pharmacyName = pharmacyName,
    pharmacyAddress = pharmacyAddress,
    pharmacyPhone = pharmacyPhone,
    pharmacyEmail = pharmacyEmail,
    panNumber = panNumber,
    vatNumber = vatNumber,
    currency = currency,
    language = language,
    theme = theme,
    printerName = printerName,
    autoBackupEnabled = autoBackupEnabled,
    autoBackupIntervalDays = autoBackupIntervalDays,
    appLockEnabled = appLockEnabled,
    biometricEnabled = biometricEnabled,
    lowStockThreshold = lowStockThreshold,
    expiryAlertDays = expiryAlertDays,
    prescriptionModuleEnabled = prescriptionModuleEnabled,
    requirePrescriptionDetails = requirePrescriptionDetails,
)

internal fun com.medipro.manager.core.database.entity.AuditLogEntity.toPayload() = AuditLogPayload(
    eventType = eventType,
    entityType = entityType,
    entityUuid = entityUuid,
    description = description,
    oldValue = oldValue,
    newValue = newValue,
    createdAt = createdAt,
)
