package com.medipro.manager.data.sync

import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.CustomerEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.SupplierEntity
import com.medipro.manager.domain.model.PendingOperation
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseReturn
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleReturn
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.SyncEntityType
import com.medipro.manager.domain.model.SyncOperationType
import com.medipro.manager.domain.repository.SyncQueueRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEnqueueHelper @Inject constructor(
    private val syncQueue: SyncQueueRepository,
    private val serializer: SyncDocumentSerializer,
    private val licenseDao: LicenseDao,
    private val syncScheduler: SyncScheduler,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val medicineDao: MedicineDao,
    private val uuidResolver: SyncUuidResolver,
) {
    suspend fun enqueueInvoice(sale: Sale) {
        val deviceId = deviceId()
        val envelope = serializer.invoiceEnvelope(sale, deviceId)
        enqueue(envelope)
    }

    suspend fun enqueuePurchase(purchase: Purchase) {
        val deviceId = deviceId()
        val envelope = serializer.purchaseEnvelope(purchase, deviceId)
        enqueue(envelope)
    }

    suspend fun enqueuePurchaseReturn(purchaseReturn: PurchaseReturn) {
        val deviceId = deviceId()
        val envelope = serializer.purchaseReturnEnvelope(purchaseReturn, deviceId)
        enqueue(envelope)
    }

    suspend fun enqueueSaleReturn(saleReturn: SaleReturn) {
        val deviceId = deviceId()
        val envelope = serializer.saleReturnEnvelope(saleReturn, deviceId)
        enqueue(envelope)
    }

    suspend fun enqueueStockAdjustment(adjustment: StockAdjustment) {
        val deviceId = deviceId()
        val envelope = serializer.stockAdjustmentEnvelope(adjustment, deviceId)
        enqueue(envelope)
    }

    suspend fun enqueueStockBatchById(batchId: Long) {
        val batch = batchDao.getById(batchId) ?: return
        val stock = stockDao.getByBatchId(batchId) ?: return
        val medicineUuid = medicineDao.getById(batch.medicineId)?.uuid ?: return
        val supplierUuid = batch.supplierId?.let { supplierDao.getById(it)?.uuid }
        val envelope = serializer.stockBatchEnvelope(batch, stock, medicineUuid, supplierUuid, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueMedicine(medicine: MedicineEntity) {
        val envelope = serializer.medicineEnvelope(medicine, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueMedicineById(medicineId: Long) {
        medicineDao.getById(medicineId)?.let { enqueueMedicine(it) }
    }

    suspend fun enqueueSettings(settings: SettingsEntity) {
        val envelope = serializer.settingsEnvelope(settings, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueAuditLog(log: AuditLogEntity) {
        val envelope = serializer.auditLogEnvelope(log, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueCustomer(customer: CustomerEntity) {
        val envelope = serializer.customerEnvelope(customer, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueSupplier(supplier: SupplierEntity) {
        val envelope = serializer.supplierEnvelope(supplier, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueuePayment(payment: PaymentEntity, referenceUuid: String) {
        val envelope = serializer.paymentEnvelope(payment, referenceUuid, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueueLedger(entry: LedgerEntity, accountUuid: String?, referenceUuid: String?) {
        val envelope = serializer.ledgerEnvelope(entry, accountUuid, referenceUuid, deviceId())
        enqueue(envelope)
    }

    suspend fun enqueuePaymentsForReference(type: String, referenceId: Long) {
        val referenceUuid = referenceUuidFor(type, referenceId) ?: return
        paymentDao.getByReference(type, referenceId).forEach { payment ->
            enqueuePayment(payment, referenceUuid)
        }
    }

    suspend fun enqueueLedgerForReference(referenceType: String, referenceId: Long) {
        val referenceUuid = referenceUuidFor(referenceType, referenceId)
        ledgerDao.getByReference(referenceType, referenceId).forEach { entry ->
            val accountUuid = entry.accountId?.let { accountId ->
                when (entry.accountType) {
                    "CUSTOMER" -> uuidResolver.customerUuid(accountId)
                    "SUPPLIER" -> uuidResolver.supplierUuid(accountId)
                    else -> null
                }
            }
            enqueueLedger(entry, accountUuid, referenceUuid)
        }
    }

    suspend fun enqueueCustomerById(customerId: Long) {
        customerDao.getById(customerId)?.let { enqueueCustomer(it) }
    }

    suspend fun enqueueSupplierById(supplierId: Long) {
        supplierDao.getById(supplierId)?.let { enqueueSupplier(it) }
    }

    private suspend fun referenceUuidFor(type: String, referenceId: Long): String? = when (type) {
        "SALE" -> saleDao.getById(referenceId)?.uuid
        "PURCHASE" -> purchaseDao.getById(referenceId)?.uuid
        else -> null
    }

    private suspend fun deviceId(): String = licenseDao.get()?.deviceId ?: "local"

    private suspend fun enqueue(envelope: SyncDocumentEnvelope) {
        syncQueue.enqueue(
            PendingOperation(
                uuid = UUID.randomUUID().toString(),
                operationType = SyncOperationType.UPSERT,
                entityType = envelope.entityType,
                entityUuid = envelope.uuid,
                payloadJson = serializer.toQueuePayload(envelope),
            )
        )
        syncScheduler.schedulePush()
    }

    companion object {
        val ENTITY_PRIORITY: Map<String, Int> = mapOf(
            SyncEntityType.CUSTOMER to 0,
            SyncEntityType.SUPPLIER to 1,
            SyncEntityType.PAYMENT to 2,
            SyncEntityType.LEDGER to 3,
            SyncEntityType.INVOICE to 4,
            SyncEntityType.PURCHASE to 5,
            SyncEntityType.PURCHASE_RETURN to 6,
            SyncEntityType.SALE_RETURN to 7,
            SyncEntityType.MEDICINE to 8,
            SyncEntityType.STOCK_BATCH to 9,
            SyncEntityType.STOCK_ADJUSTMENT to 10,
            SyncEntityType.SETTINGS to 11,
            SyncEntityType.AUDIT_LOG to 12,
        )
    }
}

suspend fun LicenseDao.pharmacyUuid(): String = get()?.licenseId.orEmpty()