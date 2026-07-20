package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.entity.BatchEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.StockEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : PurchaseRepository {

    override fun observePurchases(): Flow<List<Purchase>> =
        purchaseDao.observeAll().flatMapLatest { purchases ->
            flow {
                val result = purchases.map { purchase ->
                    val items = purchaseItemDao.getByPurchaseId(purchase.id).map { item ->
                        val medicine = medicineDao.getById(item.medicineId)
                        item.toDomain(medicine?.brandName.orEmpty())
                    }
                    val supplierName = purchase.supplierId?.let { supplierDao.getById(it)?.name }
                    purchase.toDomain(items, supplierName)
                }
                emit(result)
            }
        }

    override suspend fun getPurchaseById(id: Long): Purchase? {
        val purchase = purchaseDao.getById(id) ?: return null
        val items = purchaseItemDao.getByPurchaseId(id).map { item ->
            val medicine = medicineDao.getById(item.medicineId)
            item.toDomain(medicine?.brandName.orEmpty())
        }
        val supplierName = purchase.supplierId?.let { supplierDao.getById(it)?.name }
        return purchase.toDomain(items, supplierName)
    }

    override suspend fun getPurchaseByInvoiceNumber(invoiceNumber: String): Purchase? {
        val purchase = purchaseDao.getByInvoiceNumber(invoiceNumber.trim()) ?: return null
        return getPurchaseById(purchase.id)
    }

    override suspend fun findBySupplierBillNumber(supplierBillNumber: String, supplierId: Long?): Purchase? {
        val trimmed = supplierBillNumber.trim()
        if (trimmed.isBlank()) return null
        val purchase = purchaseDao.findBySupplierBillNumber(trimmed, supplierId) ?: return null
        return getPurchaseById(purchase.id)
    }

    override suspend fun searchMedicinesForPurchase(query: String): List<Medicine> {
        if (query.isBlank()) return emptyList()
        val ftsQuery = buildFtsQuery(query.trim())
        val entities = if (ftsQuery != null) {
            medicineDao.searchFts(ftsQuery).first()
        } else {
            medicineDao.search(query.trim()).first()
        }
        return entities.map { entity ->
            val qty = stockDao.getTotalQuantity(entity.id) ?: 0
            entity.toDomain(qty)
        }
    }

    override suspend fun generatePurchaseInvoiceNumber(): String {
        val (start, end) = todayRange()
        val count = purchaseDao.countForDay(start, end) + 1
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return "P-$datePart-${count.toString().padStart(4, '0')}"
    }

    override suspend fun createPurchase(purchase: Purchase): Long {
        require(purchase.items.isNotEmpty()) { "Purchase must have at least one item" }

        val affectedBatchIds = mutableSetOf<Long>()
        val affectedMedicineIds = mutableSetOf<Long>()

        return database.withTransaction {
            val now = System.currentTimeMillis()
            val supplierUuid = purchase.supplierId?.let { supplierDao.getById(it)?.uuid }
            val purchaseEntity = purchase.toEntity().copy(supplierUuid = supplierUuid)
            val purchaseId = purchaseDao.insert(purchaseEntity)
            val purchaseUuid = purchaseEntity.uuid
            val pharmacyUuid = licenseDao.pharmacyUuid()
            val deviceId = licenseDao.get()?.deviceId

            purchase.items.forEach { item ->
                val medicine = medicineDao.getById(item.medicineId)
                val medicineUuid = item.medicineUuid.ifBlank { medicine?.uuid.orEmpty() }
                val batchSelling = item.sellingPrice.takeIf { it > 0 }
                    ?: medicine?.let { it.sellingPricePaisa / 100.0 }
                    ?: item.unitPrice
                val existingBatch = batchDao.getByMedicineAndBatch(item.medicineId, item.batchNumber)
                val batchEntity = if (existingBatch == null) {
                    BatchEntity(
                        medicineId = item.medicineId,
                        batchNumber = item.batchNumber,
                        expiryDate = item.expiryDate,
                        quantity = item.quantity,
                        purchasePrice = item.unitPrice,
                        sellingPrice = batchSelling,
                        supplierId = purchase.supplierId,
                        createdAt = now,
                        updatedAt = now,
                    )
                } else {
                    existingBatch.copy(
                        quantity = existingBatch.quantity + item.quantity,
                        purchasePrice = item.unitPrice,
                        sellingPrice = batchSelling,
                        expiryDate = item.expiryDate,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                        syncVersion = existingBatch.syncVersion + 1,
                    )
                }
                val batchId = if (existingBatch == null) {
                    batchDao.insert(batchEntity)
                } else {
                    batchDao.update(batchEntity)
                    existingBatch.id
                }
                affectedBatchIds.add(batchId)
                val batchUuid = batchDao.getById(batchId)?.uuid.orEmpty()

                purchaseItemDao.insertAll(
                    listOf(
                        item.toEntity(purchaseId).copy(
                            medicineUuid = medicineUuid,
                            batchUuid = batchUuid,
                        )
                    )
                )

                val stock = stockDao.getByBatchId(batchId)
                if (stock == null) {
                    stockDao.insert(
                        StockEntity(
                            medicineId = item.medicineId,
                            batchId = batchId,
                            quantity = item.quantity,
                            lastUpdated = now,
                        )
                    )
                } else {
                    stockDao.update(
                        stock.copy(
                            quantity = stock.quantity + item.quantity,
                            lastUpdated = now,
                        )
                    )
                }

                if (medicine != null) {
                    affectedMedicineIds.add(medicine.id)
                    medicineDao.update(
                        medicine.copy(
                            purchasePricePaisa = (item.unitPrice * 100).roundToLong(),
                            sellingPricePaisa = item.sellingPrice.takeIf { it > 0 }
                                ?.let { (it * 100).roundToLong() }
                                ?: medicine.sellingPricePaisa,
                            mrpPaisa = item.mrp.takeIf { it > 0 }
                                ?.let { (it * 100).roundToLong() }
                                ?: medicine.mrpPaisa,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = medicine.syncVersion + 1,
                        )
                    )
                }
            }

            if (purchase.paidAmount > 0) {
                paymentDao.insert(
                    PaymentEntity(
                        type = "PURCHASE",
                        referenceId = purchaseId,
                        referenceUuid = purchaseUuid,
                        pharmacyUuid = pharmacyUuid,
                        amount = purchase.paidAmount,
                        paymentMethod = purchase.paymentMethod,
                        notes = "Payment for ${purchase.invoiceNumber}",
                        deviceId = deviceId,
                    )
                )
            }

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "PURCHASE",
                    description = "Purchase ${purchase.invoiceNumber}",
                    debit = purchase.totalAmount,
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    referenceUuid = purchaseUuid,
                    pharmacyUuid = pharmacyUuid,
                    deviceId = deviceId,
                )
            )

            if (purchase.paidAmount > 0) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CASH",
                        description = "Cash paid ${purchase.invoiceNumber}",
                        credit = purchase.paidAmount,
                        referenceType = "PURCHASE",
                        referenceId = purchaseId,
                        referenceUuid = purchaseUuid,
                        pharmacyUuid = pharmacyUuid,
                        deviceId = deviceId,
                    )
                )
            }

            val creditAmount = purchase.totalAmount - purchase.paidAmount
            val supplierId = purchase.supplierId
            if (creditAmount > 0 && supplierId != null) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "SUPPLIER",
                        accountId = supplierId,
                        accountUuid = supplierUuid,
                        description = "Credit purchase ${purchase.invoiceNumber}",
                        credit = creditAmount,
                        referenceType = "PURCHASE",
                        referenceId = purchaseId,
                        referenceUuid = purchaseUuid,
                        pharmacyUuid = pharmacyUuid,
                        deviceId = deviceId,
                    )
                )
                val supplier = supplierDao.getById(supplierId)
                if (supplier != null) {
                    supplierDao.update(
                        supplier.copy(
                            outstandingBalance = supplier.outstandingBalance + creditAmount,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = supplier.syncVersion + 1,
                        )
                    )
                }
            }

            purchaseId
        }.also { purchaseId ->
            getPurchaseById(purchaseId)?.let { saved ->
                syncEnqueueHelper.enqueuePurchase(saved)
            }
            syncEnqueueHelper.enqueuePaymentsForReference("PURCHASE", purchaseId)
            syncEnqueueHelper.enqueueLedgerForReference("PURCHASE", purchaseId)
            purchaseDao.getById(purchaseId)?.supplierId?.let { syncEnqueueHelper.enqueueSupplierById(it) }
            affectedMedicineIds.forEach { syncEnqueueHelper.enqueueMedicineById(it) }
            affectedBatchIds.forEach { syncEnqueueHelper.enqueueStockBatchById(it) }
        }
    }

    private fun buildFtsQuery(input: String): String? {
        val tokens = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "${it.replace("\"", "")}*" }
    }

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val end = calendar.timeInMillis - 1
        return start to end
    }
}
