package com.medipro.manager.data.sync

import com.medipro.manager.core.database.entity.BatchEntity
import com.medipro.manager.core.database.entity.CustomerEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.StockAdjustmentEntity
import com.medipro.manager.core.database.entity.StockEntity
import com.medipro.manager.core.database.entity.SupplierEntity
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseReturn
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleReturn
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.SyncEntityType
import com.medipro.manager.domain.model.SyncOperationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncDocumentEnvelope(
    val uuid: String,
    val entityType: String,
    val operationType: String = SyncOperationType.UPSERT,
    val createdAt: Long,
    val updatedAt: Long,
    val syncVersion: Long,
    val deletedAt: Long? = null,
    val deviceId: String? = null,
    val payloadJson: String,
)

@Singleton
class SyncDocumentSerializer @Inject constructor(
    private val uuidResolver: SyncUuidResolver,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun invoiceEnvelope(sale: Sale, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = sale.uuid.ifBlank { UUID.randomUUID().toString() },
            entityType = SyncEntityType.INVOICE,
            createdAt = sale.saleDate,
            updatedAt = now,
            syncVersion = sale.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(uuidResolver.toInvoicePayload(sale)),
        )
    }

    suspend fun purchaseEnvelope(purchase: Purchase, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = purchase.uuid.ifBlank { UUID.randomUUID().toString() },
            entityType = SyncEntityType.PURCHASE,
            createdAt = purchase.purchaseDate,
            updatedAt = now,
            syncVersion = purchase.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(uuidResolver.toPurchasePayload(purchase)),
        )
    }

    fun purchaseReturnEnvelope(purchaseReturn: PurchaseReturn, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = purchaseReturn.uuid.ifBlank { UUID.randomUUID().toString() },
            entityType = SyncEntityType.PURCHASE_RETURN,
            createdAt = purchaseReturn.returnDate,
            updatedAt = now,
            syncVersion = purchaseReturn.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(purchaseReturn.toPayload()),
        )
    }

    fun saleReturnEnvelope(saleReturn: SaleReturn, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = saleReturn.uuid.ifBlank { UUID.randomUUID().toString() },
            entityType = SyncEntityType.SALE_RETURN,
            createdAt = saleReturn.returnDate,
            updatedAt = now,
            syncVersion = saleReturn.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(saleReturn.toPayload()),
        )
    }

    fun stockAdjustmentEnvelope(adjustment: StockAdjustment, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = adjustment.uuid.ifBlank { UUID.randomUUID().toString() },
            entityType = SyncEntityType.STOCK_ADJUSTMENT,
            createdAt = adjustment.createdAt,
            updatedAt = now,
            syncVersion = adjustment.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(adjustment.toPayload()),
        )
    }

    fun stockAdjustmentEnvelope(entity: StockAdjustmentEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = entity.uuid,
            entityType = SyncEntityType.STOCK_ADJUSTMENT,
            createdAt = entity.createdAt,
            updatedAt = now,
            syncVersion = entity.syncVersion + 1,
            deletedAt = entity.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(entity.toPayload()),
        )
    }

    fun stockBatchEnvelope(
        batch: BatchEntity,
        stock: StockEntity,
        medicineUuid: String,
        supplierUuid: String?,
        deviceId: String,
    ): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = batch.uuid,
            entityType = SyncEntityType.STOCK_BATCH,
            createdAt = batch.createdAt,
            updatedAt = now,
            syncVersion = batch.syncVersion + 1,
            deletedAt = batch.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(
                batch.toStockBatchPayload(medicineUuid, stock.quantity, stock.damagedQuantity, supplierUuid)
            ),
        )
    }

    fun medicineEnvelope(medicine: MedicineEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = medicine.uuid,
            entityType = SyncEntityType.MEDICINE,
            createdAt = medicine.createdAt,
            updatedAt = now,
            syncVersion = medicine.syncVersion + 1,
            deletedAt = medicine.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(medicine.toSyncPayload()),
        )
    }

    fun settingsEnvelope(settings: SettingsEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = settings.uuid,
            entityType = SyncEntityType.SETTINGS,
            createdAt = settings.createdAt,
            updatedAt = now,
            syncVersion = settings.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(settings.toPayload()),
        )
    }

    fun auditLogEnvelope(log: AuditLogEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = log.uuid,
            entityType = SyncEntityType.AUDIT_LOG,
            createdAt = log.createdAt,
            updatedAt = now,
            syncVersion = log.syncVersion + 1,
            deviceId = deviceId,
            payloadJson = json.encodeToString(log.toPayload()),
        )
    }

    fun customerEnvelope(customer: CustomerEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = customer.uuid,
            entityType = SyncEntityType.CUSTOMER,
            createdAt = customer.createdAt,
            updatedAt = now,
            syncVersion = customer.syncVersion + 1,
            deletedAt = customer.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(customer.toPayload()),
        )
    }

    fun supplierEnvelope(supplier: SupplierEntity, deviceId: String): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = supplier.uuid,
            entityType = SyncEntityType.SUPPLIER,
            createdAt = supplier.createdAt,
            updatedAt = now,
            syncVersion = supplier.syncVersion + 1,
            deletedAt = supplier.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(supplier.toPayload()),
        )
    }

    fun paymentEnvelope(
        payment: PaymentEntity,
        referenceUuid: String,
        deviceId: String,
    ): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = payment.uuid,
            entityType = SyncEntityType.PAYMENT,
            createdAt = payment.createdAt,
            updatedAt = now,
            syncVersion = payment.syncVersion + 1,
            deletedAt = payment.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(payment.toPayload(referenceUuid)),
        )
    }

    fun ledgerEnvelope(
        entry: LedgerEntity,
        accountUuid: String?,
        referenceUuid: String?,
        deviceId: String,
    ): SyncDocumentEnvelope {
        val now = System.currentTimeMillis()
        return SyncDocumentEnvelope(
            uuid = entry.uuid,
            entityType = SyncEntityType.LEDGER,
            createdAt = entry.createdAt,
            updatedAt = now,
            syncVersion = entry.syncVersion + 1,
            deletedAt = entry.deletedAt,
            deviceId = deviceId,
            payloadJson = json.encodeToString(entry.toPayload(accountUuid, referenceUuid)),
        )
    }

    fun toQueuePayload(envelope: SyncDocumentEnvelope): String = json.encodeToString(envelope)

    internal fun parseInvoicePayload(payloadJson: String): InvoicePayload =
        json.decodeFromString(payloadJson)

    internal fun parsePurchasePayload(payloadJson: String): PurchasePayload =
        json.decodeFromString(payloadJson)

    internal fun parseCustomerPayload(payloadJson: String): CustomerPayload =
        json.decodeFromString(payloadJson)

    internal fun parseSupplierPayload(payloadJson: String): SupplierPayload =
        json.decodeFromString(payloadJson)

    internal fun parsePaymentPayload(payloadJson: String): PaymentPayload =
        json.decodeFromString(payloadJson)

    internal fun parseLedgerPayload(payloadJson: String): LedgerPayload =
        json.decodeFromString(payloadJson)

    internal fun parseStockBatchPayload(payloadJson: String): StockBatchPayload =
        json.decodeFromString(payloadJson)

    internal fun parseMedicineSyncPayload(payloadJson: String): MedicineSyncPayload =
        json.decodeFromString(payloadJson)

    internal fun parseStockAdjustmentPayload(payloadJson: String): StockAdjustmentPayload =
        json.decodeFromString(payloadJson)

    internal fun parseSettingsPayload(payloadJson: String): SettingsPayload =
        json.decodeFromString(payloadJson)

    internal fun parseAuditLogPayload(payloadJson: String): AuditLogPayload =
        json.decodeFromString(payloadJson)
}
