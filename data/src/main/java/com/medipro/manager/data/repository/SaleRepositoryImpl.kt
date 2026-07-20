package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.IncomeDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleItemDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.entity.IncomeEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SaleItemEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.PostedSaleCancellationNotAllowedException
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.StockBatch
import com.medipro.manager.domain.repository.SaleRepository
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

@Singleton
class SaleRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val customerDao: CustomerDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val incomeDao: IncomeDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : SaleRepository {

    override fun observeSales(): Flow<List<Sale>> =
        saleDao.observeAll().flatMapLatest { sales ->
            flow {
                val result = sales.map { sale ->
                    val items = saleItemDao.getBySaleId(sale.id).map { item ->
                        val medicine = medicineDao.getById(item.medicineId)
                        val batch = batchDao.getById(item.batchId)
                        item.toDomain(
                            medicineName = medicine?.brandName.orEmpty(),
                            batchNumber = batch?.batchNumber.orEmpty(),
                            expiryDate = batch?.expiryDate,
                        )
                    }
                    val customerName = sale.customerId?.let { customerDao.getById(it)?.name }
                    sale.toDomain(items, customerName)
                }
                emit(result)
            }
        }

    override suspend fun getSaleById(id: Long): Sale? {
        val sale = saleDao.getById(id) ?: return null
        val items = saleItemDao.getBySaleId(id).map { item ->
            val medicine = medicineDao.getById(item.medicineId)
            val batch = batchDao.getById(item.batchId)
            item.toDomain(
                medicineName = medicine?.brandName.orEmpty(),
                batchNumber = batch?.batchNumber.orEmpty(),
                expiryDate = batch?.expiryDate,
            )
        }
        val customerName = sale.customerId?.let { customerDao.getById(it)?.name }
        return sale.toDomain(items, customerName)
    }

    override suspend fun getSaleByInvoiceNumber(invoiceNumber: String): Sale? {
        val sale = saleDao.getByInvoiceNumber(invoiceNumber.trim()) ?: return null
        return getSaleById(sale.id)
    }

    override suspend fun getSaleByUuid(uuid: String): Sale? {
        val sale = saleDao.getByUuid(uuid.trim()) ?: return null
        return getSaleById(sale.id)
    }

    override suspend fun resolveSale(invoiceRef: String): Sale? {
        val ref = invoiceRef.trim()
        if (ref.isBlank()) return null
        return if (looksLikeUuid(ref)) {
            getSaleByUuid(ref) ?: getSaleByInvoiceNumber(ref)
        } else {
            getSaleByInvoiceNumber(ref) ?: getSaleByUuid(ref)
        }
    }

    override suspend fun cancelSale(saleId: Long): Result<Unit> = runCatching {
        throw PostedSaleCancellationNotAllowedException()
    }

    private fun looksLikeUuid(value: String): Boolean =
        value.length == 36 && value.count { it == '-' } == 4

    override suspend fun searchMedicinesForSale(query: String): List<Medicine> {
        if (query.isBlank()) return emptyList()
        return medicineDao.search(query).first().map { entity ->
            val qty = stockDao.getTotalQuantity(entity.id) ?: 0
            entity.toDomain(qty)
        }
    }

    override suspend fun getAvailableBatches(medicineId: Long): List<StockBatch> {
        val now = System.currentTimeMillis()
        val batches = batchDao.getAvailableForSale(medicineId, now)
        return batches.mapNotNull { batch ->
            val stock = stockDao.getByBatchId(batch.id) ?: return@mapNotNull null
            if (stock.quantity <= 0) return@mapNotNull null
            StockBatch(
                batchId = batch.id,
                batchUuid = batch.uuid,
                batchNumber = batch.batchNumber,
                expiryDate = batch.expiryDate,
                availableQuantity = stock.quantity,
                sellingPrice = batch.sellingPrice.takeIf { it > 0 } ?: batch.purchasePrice
            )
        }
    }

    override suspend fun generateInvoiceNumber(): String {
        val (start, end) = todayRange()
        val count = saleDao.countForDay(start, end) + 1
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return "INV-$datePart-${count.toString().padStart(6, '0')}"
    }

    override suspend fun createSale(sale: Sale): Long {
        require(sale.items.isNotEmpty()) { "Sale must have at least one item" }

        val affectedBatchIds = mutableSetOf<Long>()

        return database.withTransaction {
            sale.items.forEach { item ->
                val stock = stockDao.getByBatchId(item.batchId)
                    ?: throw IllegalStateException("Stock not found for batch ${item.batchNumber}")
                if (stock.quantity < item.quantity) {
                    throw IllegalStateException(
                        "Insufficient stock for ${item.medicineName}. Available: ${stock.quantity}"
                    )
                }
            }

            val customerUuid = sale.customerId?.let { customerDao.getById(it)?.uuid }
            val saleEntity = sale.toEntity().copy(customerUuid = customerUuid)
            val saleId = saleDao.insert(saleEntity)
            val saleUuid = saleEntity.uuid
            val pharmacyUuid = licenseDao.pharmacyUuid()
            val deviceId = licenseDao.get()?.deviceId
            val itemEntities = sale.items.map { item ->
                val medUuid = item.medicineUuid.ifBlank {
                    medicineDao.getById(item.medicineId)?.uuid.orEmpty()
                }
                val batUuid = item.batchUuid.ifBlank {
                    batchDao.getById(item.batchId)?.uuid.orEmpty()
                }
                require(medUuid.isNotBlank() && batUuid.isNotBlank()) {
                    "Sale item missing medicine/batch UUID"
                }
                item.toEntity(saleId).copy(medicineUuid = medUuid, batchUuid = batUuid)
            }
            saleItemDao.insertAll(itemEntities)

            sale.items.forEach { item ->
                val stock = stockDao.getByBatchId(item.batchId)!!
                stockDao.update(
                    stock.copy(
                        quantity = stock.quantity - item.quantity,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                val batch = batchDao.getById(item.batchId)
                if (batch != null) {
                    batchDao.update(
                        batch.copy(
                            quantity = maxOf(0, batch.quantity - item.quantity),
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = batch.syncVersion + 1,
                        )
                    )
                    affectedBatchIds.add(item.batchId)
                }
            }

            if (sale.paidAmount > 0) {
                paymentDao.insert(
                    PaymentEntity(
                        type = "SALE",
                        referenceId = saleId,
                        referenceUuid = saleUuid,
                        pharmacyUuid = pharmacyUuid,
                        amount = sale.paidAmount,
                        paymentMethod = sale.paymentMethod,
                        notes = "Payment for ${sale.invoiceNumber}",
                        deviceId = deviceId,
                    )
                )
            }

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "SALES",
                    description = "Sale ${sale.invoiceNumber}",
                    credit = sale.totalAmount,
                    referenceType = "SALE",
                    referenceId = saleId,
                    referenceUuid = saleUuid,
                    pharmacyUuid = pharmacyUuid,
                    deviceId = deviceId,
                )
            )

            if (sale.paidAmount > 0) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CASH",
                        description = "Cash received ${sale.invoiceNumber}",
                        debit = sale.paidAmount,
                        referenceType = "SALE",
                        referenceId = saleId,
                        referenceUuid = saleUuid,
                        pharmacyUuid = pharmacyUuid,
                        deviceId = deviceId,
                    )
                )
                incomeDao.insert(
                    IncomeEntity(
                        category = "SALES",
                        description = "Sale ${sale.invoiceNumber}",
                        amount = sale.paidAmount,
                        paymentMethod = sale.paymentMethod
                    )
                )
            }

            val creditAmount = sale.totalAmount - sale.paidAmount
            val customerId = sale.customerId
            if (creditAmount > 0 && customerId != null) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CUSTOMER",
                        accountId = customerId,
                        accountUuid = customerUuid,
                        description = "Credit sale ${sale.invoiceNumber}",
                        debit = creditAmount,
                        referenceType = "SALE",
                        referenceId = saleId,
                        referenceUuid = saleUuid,
                        pharmacyUuid = pharmacyUuid,
                        deviceId = deviceId,
                    )
                )
                val customer = customerDao.getById(customerId)
                if (customer != null) {
                    val updatedCustomer = customer.copy(
                        outstandingBalance = customer.outstandingBalance + creditAmount,
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = com.medipro.manager.core.database.entity.SyncStatus.PENDING,
                        syncVersion = customer.syncVersion + 1,
                    )
                    customerDao.update(updatedCustomer)
                }
            }

            saleId
        }.also { saleId ->
            getSaleById(saleId)?.let { saved ->
                syncEnqueueHelper.enqueueInvoice(saved)
            }
            syncEnqueueHelper.enqueuePaymentsForReference("SALE", saleId)
            syncEnqueueHelper.enqueueLedgerForReference("SALE", saleId)
            saleDao.getById(saleId)?.customerId?.let { syncEnqueueHelper.enqueueCustomerById(it) }
            affectedBatchIds.forEach { syncEnqueueHelper.enqueueStockBatchById(it) }
        }
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
