package com.medipro.manager.data.sync

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleItemDao
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.core.database.dao.AuditLogDao
import com.medipro.manager.core.database.dao.StockAdjustmentDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.BatchEntity
import com.medipro.manager.core.database.entity.CustomerEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.PurchaseEntity
import com.medipro.manager.core.database.entity.PurchaseItemEntity
import com.medipro.manager.core.database.entity.SaleEntity
import com.medipro.manager.core.database.entity.SaleItemEntity
import com.medipro.manager.core.database.entity.SettingsEntity
import com.medipro.manager.core.database.entity.StockAdjustmentEntity
import com.medipro.manager.core.database.entity.StockAdjustmentType as EntityAdjustmentType
import com.medipro.manager.core.database.entity.StockEntity
import com.medipro.manager.core.database.entity.SupplierEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.domain.model.SyncEntityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies inbound Firestore documents to Room.
 * Resolves UUID relations to local FK ids via [SyncUuidResolver].
 */
@Singleton
class FirestoreRemoteApplier @Inject constructor(
    private val database: MediProDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val settingsDao: SettingsDao,
    private val auditLogDao: AuditLogDao,
    private val serializer: SyncDocumentSerializer,
    private val uuidResolver: SyncUuidResolver,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun applyRemoteDocument(entityType: String, data: Map<String, Any?>) {
        scope.launch {
            runCatching { applyRemoteDocumentInternal(entityType, data) }
                .onFailure { Timber.w(it, "Remote sync apply failed for $entityType") }
        }
    }

    private suspend fun applyRemoteDocumentInternal(entityType: String, data: Map<String, Any?>) {
        val uuid = data["uuid"] as? String ?: return
        val remoteVersion = (data["syncVersion"] as? Number)?.toLong() ?: 0L
        val payloadJson = data["payload"] as? String ?: return
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: createdAt
        val deletedAt = (data["deletedAt"] as? Number)?.toLong()
        val deviceId = data["deviceId"] as? String

        when (entityType) {
            SyncEntityType.INVOICE -> applyInvoice(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.PURCHASE -> applyPurchase(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.CUSTOMER -> applyCustomer(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.SUPPLIER -> applySupplier(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.PAYMENT -> applyPayment(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.LEDGER -> applyLedger(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.MEDICINE -> applyMedicine(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.STOCK_BATCH -> applyStockBatch(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.STOCK_ADJUSTMENT -> applyStockAdjustment(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deletedAt, deviceId,
            )
            SyncEntityType.SETTINGS -> applySettings(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deviceId,
            )
            SyncEntityType.AUDIT_LOG -> applyAuditLog(
                uuid, remoteVersion, payloadJson, createdAt, updatedAt, deviceId,
            )
            else -> Timber.d("Remote sync skipped unsupported type: $entityType")
        }
    }

    private suspend fun applyInvoice(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = saleDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                localVersion = existing.syncVersion,
                localUpdatedAt = existing.updatedAt,
                localDeviceId = existing.deviceId,
                remoteVersion = remoteVersion,
                remoteUpdatedAt = updatedAt,
                remoteDeviceId = deviceId,
            )
        ) return

        if (deletedAt != null && existing != null) {
            saleDao.softDelete(existing.id, deletedAt)
            return
        }

        val payload = serializer.parseInvoicePayload(payloadJson)
        val customerId = payload.customerUuid?.let { uuidResolver.customerId(it) }

        database.withTransaction {
            val saleId = if (existing != null) {
                saleDao.insert(
                    existing.copy(
                        customerId = customerId,
                        customerUuid = payload.customerUuid,
                        invoiceNumber = payload.invoiceNumber,
                        saleDate = createdAt,
                        subtotal = payload.subtotal,
                        discount = payload.discount,
                        vatAmount = payload.vatAmount,
                        totalAmount = payload.totalAmount,
                        paidAmount = payload.paidAmount,
                        paymentStatus = payload.paymentStatus,
                        paymentMethod = payload.paymentMethod,
                        isCredit = payload.isCredit,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
                existing.id
            } else {
                saleDao.insert(
                    SaleEntity(
                        uuid = uuid,
                        customerId = customerId,
                        customerUuid = payload.customerUuid,
                        invoiceNumber = payload.invoiceNumber,
                        saleDate = createdAt,
                        subtotal = payload.subtotal,
                        discount = payload.discount,
                        vatAmount = payload.vatAmount,
                        totalAmount = payload.totalAmount,
                        paidAmount = payload.paidAmount,
                        paymentStatus = payload.paymentStatus,
                        paymentMethod = payload.paymentMethod,
                        isCredit = payload.isCredit,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
            }

            saleItemDao.softDeleteBySaleId(saleId)
            val itemEntities = payload.items.mapNotNull { item ->
                val medicineId = uuidResolver.medicineId(item.medicineUuid) ?: return@mapNotNull null
                val batchId = uuidResolver.batchId(item.batchUuid) ?: return@mapNotNull null
                SaleItemEntity(
                    saleId = saleId,
                    medicineId = medicineId,
                    medicineUuid = item.medicineUuid,
                    batchId = batchId,
                    batchUuid = item.batchUuid,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    syncStatus = SyncStatus.SYNCED,
                )
            }
            if (itemEntities.isNotEmpty()) saleItemDao.insertAll(itemEntities)
        }
        Timber.i("Remote invoice applied: $uuid v$remoteVersion")
    }

    private suspend fun applyPurchase(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = purchaseDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                localVersion = existing.syncVersion,
                localUpdatedAt = existing.updatedAt,
                localDeviceId = existing.deviceId,
                remoteVersion = remoteVersion,
                remoteUpdatedAt = updatedAt,
                remoteDeviceId = deviceId,
            )
        ) return

        if (deletedAt != null && existing != null) {
            purchaseDao.softDelete(existing.id, deletedAt)
            return
        }

        val payload = serializer.parsePurchasePayload(payloadJson)
        val supplierId = payload.supplierUuid?.let { uuidResolver.supplierId(it) }

        database.withTransaction {
            val purchaseId = if (existing != null) {
                purchaseDao.insert(
                    existing.copy(
                        supplierId = supplierId,
                        supplierUuid = payload.supplierUuid,
                        invoiceNumber = payload.invoiceNumber,
                        purchaseDate = createdAt,
                        subtotal = payload.subtotal,
                        discount = payload.discount,
                        vatAmount = payload.vatAmount,
                        totalAmount = payload.totalAmount,
                        paidAmount = payload.paidAmount,
                        paymentStatus = payload.paymentStatus,
                        paymentMethod = payload.paymentMethod,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
                existing.id
            } else {
                purchaseDao.insert(
                    PurchaseEntity(
                        uuid = uuid,
                        supplierId = supplierId,
                        supplierUuid = payload.supplierUuid,
                        invoiceNumber = payload.invoiceNumber,
                        purchaseDate = createdAt,
                        subtotal = payload.subtotal,
                        discount = payload.discount,
                        vatAmount = payload.vatAmount,
                        totalAmount = payload.totalAmount,
                        paidAmount = payload.paidAmount,
                        paymentStatus = payload.paymentStatus,
                        paymentMethod = payload.paymentMethod,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
            }

            purchaseItemDao.softDeleteByPurchaseId(purchaseId)
            val itemEntities = payload.items.mapNotNull { item ->
                val medicineId = uuidResolver.medicineId(item.medicineUuid) ?: return@mapNotNull null
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    medicineId = medicineId,
                    medicineUuid = item.medicineUuid,
                    batchUuid = item.batchUuid,
                    batchNumber = item.batchNumber,
                    expiryDate = item.expiryDate,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    syncStatus = SyncStatus.SYNCED,
                )
            }
            if (itemEntities.isNotEmpty()) purchaseItemDao.insertAll(itemEntities)
        }
        Timber.i("Remote purchase applied: $uuid v$remoteVersion")
    }

    private suspend fun applyCustomer(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = customerDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        if (deletedAt != null && existing != null) {
            customerDao.softDelete(existing.id, deletedAt)
            return
        }
        val payload = serializer.parseCustomerPayload(payloadJson)
        val entity = if (existing != null) {
            existing.copy(
                name = payload.name,
                phone = payload.phone,
                email = payload.email,
                address = payload.address,
                creditLimit = payload.creditLimit,
                outstandingBalance = payload.outstandingBalance,
                isActive = payload.isActive,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        } else {
            CustomerEntity(
                uuid = uuid,
                name = payload.name,
                phone = payload.phone,
                email = payload.email,
                address = payload.address,
                creditLimit = payload.creditLimit,
                outstandingBalance = payload.outstandingBalance,
                isActive = payload.isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        }
        if (existing != null) customerDao.update(entity) else customerDao.insert(entity)
        Timber.i("Remote customer applied: $uuid v$remoteVersion")
    }

    private suspend fun applySupplier(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = supplierDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        if (deletedAt != null && existing != null) {
            supplierDao.softDelete(existing.id, deletedAt)
            return
        }
        val payload = serializer.parseSupplierPayload(payloadJson)
        val entity = if (existing != null) {
            existing.copy(
                name = payload.name,
                contactPerson = payload.contactPerson,
                phone = payload.phone,
                email = payload.email,
                address = payload.address,
                panNumber = payload.panNumber,
                creditLimit = payload.creditLimit,
                outstandingBalance = payload.outstandingBalance,
                isActive = payload.isActive,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        } else {
            SupplierEntity(
                uuid = uuid,
                name = payload.name,
                contactPerson = payload.contactPerson,
                phone = payload.phone,
                email = payload.email,
                address = payload.address,
                panNumber = payload.panNumber,
                creditLimit = payload.creditLimit,
                outstandingBalance = payload.outstandingBalance,
                isActive = payload.isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        }
        if (existing != null) supplierDao.update(entity) else supplierDao.insert(entity)
        Timber.i("Remote supplier applied: $uuid v$remoteVersion")
    }

    private suspend fun applyPayment(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = paymentDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        val payload = serializer.parsePaymentPayload(payloadJson)
        val referenceId = referenceIdFor(payload.type, payload.referenceUuid)
        val entity = if (existing != null) {
            existing.copy(
                type = payload.type,
                referenceId = referenceId ?: existing.referenceId,
                referenceUuid = payload.referenceUuid,
                amount = payload.amount,
                paymentMethod = payload.paymentMethod,
                paymentDate = payload.paymentDate,
                notes = payload.notes,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        } else {
            PaymentEntity(
                uuid = uuid,
                type = payload.type,
                referenceId = referenceId ?: 0L,
                referenceUuid = payload.referenceUuid,
                amount = payload.amount,
                paymentMethod = payload.paymentMethod,
                paymentDate = payload.paymentDate,
                notes = payload.notes,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        }
        paymentDao.insert(entity)
        Timber.i("Remote payment applied: $uuid v$remoteVersion")
    }

    private suspend fun applyLedger(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = ledgerDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        val payload = serializer.parseLedgerPayload(payloadJson)
        val accountId = payload.accountUuid?.let { accountUuid ->
            when (payload.accountType) {
                "CUSTOMER" -> uuidResolver.customerId(accountUuid)
                "SUPPLIER" -> uuidResolver.supplierId(accountUuid)
                else -> null
            }
        }
        val referenceId = payload.referenceType?.let { type ->
            payload.referenceUuid?.let { referenceIdFor(type, it) }
        }
        val entity = if (existing != null) {
            existing.copy(
                accountType = payload.accountType,
                accountId = accountId ?: existing.accountId,
                accountUuid = payload.accountUuid,
                description = payload.description,
                debit = payload.debit,
                credit = payload.credit,
                balance = payload.balance,
                referenceType = payload.referenceType,
                referenceId = referenceId ?: existing.referenceId,
                referenceUuid = payload.referenceUuid,
                entryDate = payload.entryDate,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        } else {
            LedgerEntity(
                uuid = uuid,
                accountType = payload.accountType,
                accountId = accountId,
                accountUuid = payload.accountUuid,
                description = payload.description,
                debit = payload.debit,
                credit = payload.credit,
                balance = payload.balance,
                referenceType = payload.referenceType,
                referenceId = referenceId,
                referenceUuid = payload.referenceUuid,
                entryDate = payload.entryDate,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        }
        ledgerDao.insert(entity)
        Timber.i("Remote ledger applied: $uuid v$remoteVersion")
    }

    private suspend fun applyMedicine(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = medicineDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        if (deletedAt != null && existing != null) {
            medicineDao.softDelete(existing.id, deletedAt)
            return
        }
        val payload = serializer.parseMedicineSyncPayload(payloadJson)
        val entity = if (existing != null) {
            existing.copy(
                brandName = payload.brandName,
                genericName = payload.genericName,
                composition = payload.composition,
                strength = payload.strength,
                dosageForm = payload.dosageForm,
                manufacturer = payload.manufacturer,
                category = payload.category,
                barcode = payload.barcode,
                unit = payload.unit,
                purchasePricePaisa = payload.purchasePricePaisa,
                sellingPricePaisa = payload.sellingPricePaisa,
                mrpPaisa = payload.mrpPaisa,
                vatPercent = payload.vatPercent,
                reorderLevel = payload.reorderLevel,
                requiresPrescription = payload.requiresPrescription,
                controlledSubstance = payload.controlledSubstance,
                scheduleCategory = payload.scheduleCategory,
                isActive = payload.isActive,
                catalogUuid = payload.catalogUuid ?: existing.catalogUuid,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        } else {
            MedicineEntity(
                uuid = uuid,
                catalogUuid = payload.catalogUuid,
                brandName = payload.brandName,
                genericName = payload.genericName,
                composition = payload.composition,
                strength = payload.strength,
                dosageForm = payload.dosageForm,
                manufacturer = payload.manufacturer,
                category = payload.category,
                barcode = payload.barcode,
                unit = payload.unit,
                purchasePricePaisa = payload.purchasePricePaisa,
                sellingPricePaisa = payload.sellingPricePaisa,
                mrpPaisa = payload.mrpPaisa,
                vatPercent = payload.vatPercent,
                reorderLevel = payload.reorderLevel,
                requiresPrescription = payload.requiresPrescription,
                controlledSubstance = payload.controlledSubstance,
                scheduleCategory = payload.scheduleCategory,
                isActive = payload.isActive,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
                deviceId = deviceId,
            )
        }
        if (existing != null) medicineDao.update(entity) else medicineDao.insert(entity)
        Timber.i("Remote medicine applied: $uuid v$remoteVersion")
    }

    private suspend fun applyStockBatch(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = batchDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        if (deletedAt != null && existing != null) {
            batchDao.softDelete(existing.id, deletedAt)
            return
        }
        val payload = serializer.parseStockBatchPayload(payloadJson)
        val medicineId = uuidResolver.medicineId(payload.medicineUuid) ?: run {
            Timber.d("Stock batch skipped — medicine not found: ${payload.medicineUuid}")
            return
        }
        val supplierId = payload.supplierUuid?.let { uuidResolver.supplierId(it) }

        database.withTransaction {
            val batchId = if (existing != null) {
                batchDao.insert(
                    existing.copy(
                        medicineId = medicineId,
                        batchNumber = payload.batchNumber,
                        expiryDate = payload.expiryDate,
                        quantity = payload.quantity,
                        purchasePrice = payload.purchasePrice,
                        sellingPrice = payload.sellingPrice,
                        supplierId = supplierId ?: existing.supplierId,
                        barcode = payload.barcode,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
                existing.id
            } else {
                batchDao.insert(
                    BatchEntity(
                        uuid = uuid,
                        medicineId = medicineId,
                        batchNumber = payload.batchNumber,
                        expiryDate = payload.expiryDate,
                        quantity = payload.quantity,
                        purchasePrice = payload.purchasePrice,
                        sellingPrice = payload.sellingPrice,
                        supplierId = supplierId,
                        barcode = payload.barcode,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = remoteVersion,
                        deviceId = deviceId,
                    )
                )
            }

            val stock = stockDao.getByBatchId(batchId)
            if (stock == null) {
                stockDao.insert(
                    StockEntity(
                        medicineId = medicineId,
                        batchId = batchId,
                        quantity = payload.quantity,
                        damagedQuantity = payload.damagedQuantity,
                        lastUpdated = updatedAt,
                    )
                )
            } else {
                stockDao.update(
                    stock.copy(
                        quantity = payload.quantity,
                        damagedQuantity = payload.damagedQuantity,
                        lastUpdated = updatedAt,
                    )
                )
            }
        }
        Timber.i("Remote stock batch applied: $uuid v$remoteVersion")
    }

    private suspend fun applyStockAdjustment(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deletedAt: Long?,
        deviceId: String?,
    ) {
        val existing = stockAdjustmentDao.getByUuid(uuid)
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return
        if (deletedAt != null && existing != null) {
            stockAdjustmentDao.softDelete(existing.id, deletedAt)
            return
        }
        val payload = serializer.parseStockAdjustmentPayload(payloadJson)
        val medicineId = uuidResolver.medicineId(payload.medicineUuid) ?: run {
            Timber.d("Stock adjustment skipped — medicine not found: ${payload.medicineUuid}")
            return
        }
        val batchId = uuidResolver.batchId(payload.batchUuid) ?: run {
            Timber.d("Stock adjustment skipped — batch not found: ${payload.batchUuid}")
            return
        }

        database.withTransaction {
            val stock = stockDao.getByBatchId(batchId)
            val batch = batchDao.getById(batchId)
            if (stock != null && batch != null) {
                val damagedDelta = when (payload.type) {
                    EntityAdjustmentType.DAMAGE, EntityAdjustmentType.EXPIRED ->
                        (payload.oldQty - payload.newQty).coerceAtLeast(0)
                    else -> 0
                }
                stockDao.update(
                    stock.copy(
                        quantity = payload.newQty,
                        damagedQuantity = if (damagedDelta > 0) {
                            stock.damagedQuantity + damagedDelta
                        } else {
                            stock.damagedQuantity
                        },
                        lastUpdated = updatedAt,
                    )
                )
                batchDao.update(
                    batch.copy(
                        quantity = payload.newQty,
                        updatedAt = updatedAt,
                        syncStatus = SyncStatus.SYNCED,
                        syncVersion = maxOf(batch.syncVersion, remoteVersion),
                        deviceId = deviceId,
                    )
                )
            }

            val entity = if (existing != null) {
                existing.copy(
                    adjustmentNumber = payload.adjustmentNumber,
                    medicineId = medicineId,
                    medicineUuid = payload.medicineUuid,
                    batchId = batchId,
                    batchUuid = payload.batchUuid,
                    type = payload.type,
                    oldQty = payload.oldQty,
                    adjustQty = payload.adjustQty,
                    newQty = payload.newQty,
                    reason = payload.reason,
                    remarks = payload.remarks,
                    updatedAt = updatedAt,
                    deletedAt = deletedAt,
                    syncStatus = SyncStatus.SYNCED,
                    syncVersion = remoteVersion,
                    deviceId = deviceId,
                )
            } else {
                StockAdjustmentEntity(
                    uuid = uuid,
                    adjustmentNumber = payload.adjustmentNumber,
                    medicineId = medicineId,
                    medicineUuid = payload.medicineUuid,
                    batchId = batchId,
                    batchUuid = payload.batchUuid,
                    type = payload.type,
                    oldQty = payload.oldQty,
                    adjustQty = payload.adjustQty,
                    newQty = payload.newQty,
                    reason = payload.reason,
                    remarks = payload.remarks,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    deletedAt = deletedAt,
                    syncStatus = SyncStatus.SYNCED,
                    syncVersion = remoteVersion,
                    deviceId = deviceId,
                )
            }
            stockAdjustmentDao.insert(entity)
        }
        Timber.i("Remote stock adjustment applied: $uuid v$remoteVersion")
    }

    private suspend fun applySettings(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deviceId: String?,
    ) {
        val existing = settingsDao.get()
        if (existing != null && SyncConflictResolver.shouldSkipExisting(
                existing.syncVersion, existing.updatedAt, existing.deviceId,
                remoteVersion, updatedAt, deviceId,
            )
        ) return

        val payload = serializer.parseSettingsPayload(payloadJson)
        val entity = SettingsEntity(
            id = 1,
            uuid = uuid,
            pharmacyUuid = existing?.pharmacyUuid.orEmpty(),
            pharmacyName = payload.pharmacyName,
            pharmacyAddress = payload.pharmacyAddress,
            pharmacyPhone = payload.pharmacyPhone,
            pharmacyEmail = payload.pharmacyEmail,
            panNumber = payload.panNumber,
            vatNumber = payload.vatNumber,
            currency = payload.currency,
            language = payload.language,
            theme = payload.theme,
            printerName = payload.printerName,
            autoBackupEnabled = payload.autoBackupEnabled,
            autoBackupIntervalDays = payload.autoBackupIntervalDays,
            appLockEnabled = payload.appLockEnabled,
            biometricEnabled = payload.biometricEnabled,
            lowStockThreshold = payload.lowStockThreshold,
            expiryAlertDays = payload.expiryAlertDays,
            prescriptionModuleEnabled = payload.prescriptionModuleEnabled,
            requirePrescriptionDetails = payload.requirePrescriptionDetails,
            createdAt = existing?.createdAt ?: createdAt,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED,
            syncVersion = remoteVersion,
            deviceId = deviceId,
        )
        if (existing == null) settingsDao.insert(entity) else settingsDao.update(entity)
        Timber.i("Remote settings applied: $uuid v$remoteVersion")
    }

    private suspend fun applyAuditLog(
        uuid: String,
        remoteVersion: Long,
        payloadJson: String,
        createdAt: Long,
        updatedAt: Long,
        deviceId: String?,
    ) {
        if (auditLogDao.getByUuid(uuid) != null) return

        val payload = serializer.parseAuditLogPayload(payloadJson)
        auditLogDao.insert(
            AuditLogEntity(
                uuid = uuid,
                eventType = payload.eventType,
                entityType = payload.entityType,
                entityUuid = payload.entityUuid,
                description = payload.description,
                oldValue = payload.oldValue,
                newValue = payload.newValue,
                deviceId = deviceId,
                createdAt = payload.createdAt,
                updatedAt = updatedAt,
                syncStatus = SyncStatus.SYNCED,
                syncVersion = remoteVersion,
            )
        )
        Timber.i("Remote audit log applied: $uuid v$remoteVersion")
    }

    private suspend fun referenceIdFor(type: String, referenceUuid: String): Long? = when (type) {
        "SALE" -> uuidResolver.saleId(referenceUuid)
        "PURCHASE" -> uuidResolver.purchaseId(referenceUuid)
        else -> null
    }
}
