package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SaleItemDao
import com.medipro.manager.core.database.dao.SaleReturnDao
import com.medipro.manager.core.database.dao.SaleReturnItemDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.entity.AuditEventType
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.SaleReturnEntity
import com.medipro.manager.core.database.entity.SaleReturnItemEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.SaleReturn
import com.medipro.manager.domain.model.SaleReturnContext
import com.medipro.manager.domain.model.SaleReturnItem
import com.medipro.manager.domain.model.SaleReturnLine
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.SaleReturnRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToLong

@Singleton
class SaleReturnRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val saleReturnDao: SaleReturnDao,
    private val saleReturnItemDao: SaleReturnItemDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val customerDao: CustomerDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val licenseDao: LicenseDao,
    private val auditRepository: AuditRepository,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : SaleReturnRepository {

    override suspend fun getReturnContext(saleId: Long): SaleReturnContext? {
        val sale = saleDao.getById(saleId) ?: return null
        if (sale.deletedAt != null) return null

        val items = saleItemDao.getBySaleId(saleId)
        val customerName = sale.customerId?.let { customerDao.getById(it)?.name }
        val saleDomain = sale.toDomain(
            items.map { item ->
                val medicine = medicineDao.getById(item.medicineId)
                val batch = batchDao.getById(item.batchId)
                item.toDomain(
                    medicineName = medicine?.brandName.orEmpty(),
                    batchNumber = batch?.batchNumber.orEmpty(),
                )
            },
            customerName,
        )

        val lines = items.map { item ->
            val medicine = medicineDao.getById(item.medicineId)
            val batch = batchDao.getById(item.batchId)
            val alreadyReturned = saleReturnItemDao.getReturnedQtyForSaleItem(item.uuid)

            SaleReturnLine(
                saleItemId = item.id,
                invoiceItemUuid = item.uuid,
                medicineId = item.medicineId,
                medicineUuid = item.medicineUuid,
                medicineName = medicine?.brandName.orEmpty(),
                batchId = item.batchId,
                batchUuid = item.batchUuid,
                batchNumber = batch?.batchNumber.orEmpty(),
                soldQty = item.quantity,
                alreadyReturnedQty = alreadyReturned,
                unitPrice = item.unitPrice,
                discount = item.discount,
                vatPercent = item.vatPercent,
            )
        }

        return SaleReturnContext(sale = saleDomain, lines = lines)
    }

    override suspend fun createSaleReturn(
        saleId: Long,
        reason: String,
        lines: List<SaleReturnLine>,
        notes: String?,
    ): Long {
        val activeLines = lines.filter { it.returnQty > 0 }
        require(activeLines.isNotEmpty()) { "Select at least one item to return" }

        val sale = saleDao.getById(saleId)
            ?: throw IllegalStateException("Sale not found")
        val pharmacyUuid = licenseDao.get()?.licenseId.orEmpty()
        val deviceId = licenseDao.get()?.deviceId

        return database.withTransaction {
            val now = System.currentTimeMillis()
            activeLines.forEach { line ->
                require(line.returnQty <= line.maxReturnableQty) {
                    "Return qty ${line.returnQty} exceeds max ${line.maxReturnableQty} for ${line.medicineName}"
                }
            }

            val subtotalPaisa = activeLines.sumOf { (it.lineSubtotal * 100).roundToLong() }
            val vatPaisa = activeLines.sumOf { (it.lineVat * 100).roundToLong() }
            val grandTotalPaisa = subtotalPaisa + vatPaisa
            val returnNumber = generateReturnNumberInternal(now)
            val returnUuid = UUID.randomUUID().toString()

            val returnId = saleReturnDao.insert(
                SaleReturnEntity(
                    uuid = returnUuid,
                    pharmacyUuid = pharmacyUuid,
                    saleId = saleId,
                    invoiceUuid = sale.uuid,
                    customerId = sale.customerId,
                    customerUuid = sale.customerUuid,
                    returnNumber = returnNumber,
                    reason = reason,
                    returnDate = now,
                    subtotalPaisa = subtotalPaisa,
                    vatPaisa = vatPaisa,
                    grandTotalPaisa = grandTotalPaisa,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    syncVersion = 1,
                    deviceId = deviceId,
                )
            )

            val itemEntities = activeLines.map { line ->
                val pricePaisa = (line.unitPrice * 100).roundToLong()
                SaleReturnItemEntity(
                    saleReturnId = returnId,
                    saleReturnUuid = returnUuid,
                    saleItemId = line.saleItemId,
                    invoiceItemUuid = line.invoiceItemUuid,
                    medicineId = line.medicineId,
                    medicineUuid = line.medicineUuid,
                    batchId = line.batchId,
                    batchUuid = line.batchUuid,
                    quantity = line.returnQty,
                    sellingPricePaisa = pricePaisa,
                    amountPaisa = pricePaisa * line.returnQty,
                    pharmacyUuid = pharmacyUuid,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = deviceId,
                )
            }
            saleReturnItemDao.insertAll(itemEntities)

            activeLines.forEach { line ->
                val stock = stockDao.getByBatchId(line.batchId)
                    ?: throw IllegalStateException("Stock not found for batch ${line.batchNumber}")
                stockDao.update(
                    stock.copy(
                        quantity = stock.quantity + line.returnQty,
                        lastUpdated = now,
                    )
                )
                batchDao.getById(line.batchId)?.let { batch ->
                    batchDao.update(
                        batch.copy(
                            quantity = batch.quantity + line.returnQty,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = batch.syncVersion + 1,
                        )
                    )
                }
            }

            val returnTotal = grandTotalPaisa / 100.0
            val saleTotal = sale.totalAmount.coerceAtLeast(0.01)
            val ratio = returnTotal / saleTotal
            val cashRefund = min(sale.paidAmount * ratio, returnTotal)
            val customerReduction = returnTotal - cashRefund

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "SALES",
                    description = "Sales return $returnNumber",
                    debit = returnTotal,
                    referenceType = "SALE_RETURN",
                    referenceId = returnId,
                    referenceUuid = returnUuid,
                    entryDate = now,
                    pharmacyUuid = pharmacyUuid,
                )
            )

            val customerId = sale.customerId
            if (customerReduction > 0 && customerId != null) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CUSTOMER",
                        accountId = customerId,
                        accountUuid = sale.customerUuid,
                        description = "Return credit $returnNumber",
                        credit = customerReduction,
                        referenceType = "SALE_RETURN",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        entryDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
                customerDao.getById(customerId)?.let { customer ->
                    customerDao.update(
                        customer.copy(
                            outstandingBalance = maxOf(0.0, customer.outstandingBalance - customerReduction),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = customer.syncVersion + 1,
                        )
                    )
                }
            }

            if (cashRefund > 0) {
                paymentDao.insert(
                    PaymentEntity(
                        type = "REFUND",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        amount = cashRefund,
                        paymentMethod = sale.paymentMethod,
                        notes = "Refund for $returnNumber",
                        paymentDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CASH",
                        description = "Refund $returnNumber",
                        credit = cashRefund,
                        referenceType = "SALE_RETURN",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        entryDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
            }

            returnId
        }.also { returnId ->
            getSaleReturnById(returnId)?.let { saved ->
                auditRepository.log(
                    AuditLog(
                        uuid = UUID.randomUUID().toString(),
                        eventType = AuditEventType.SALE_RETURN_CREATED,
                        entityType = "SALE_RETURN",
                        entityUuid = saved.uuid,
                        description = "Sales return ${saved.returnNumber}: ${saved.reason}",
                        newValue = saved.grandTotal.toString(),
                    )
                )
                syncEnqueueHelper.enqueueSaleReturn(saved)
            }
        }
    }

    override suspend fun generateReturnNumber(): String =
        generateReturnNumberInternal(System.currentTimeMillis())

    override fun observeSaleReturns(): Flow<List<SaleReturn>> =
        saleReturnDao.observeAll().flatMapLatest { returns ->
            flow {
                emit(
                    returns.map { entity ->
                        val items = saleReturnItemDao.getByReturnId(entity.id).map { item ->
                            val medicine = medicineDao.getById(item.medicineId)
                            val batch = batchDao.getById(item.batchId)
                            SaleReturnItem(
                                id = item.id,
                                uuid = item.uuid,
                                saleItemId = item.saleItemId,
                                invoiceItemUuid = item.invoiceItemUuid,
                                medicineId = item.medicineId,
                                medicineUuid = item.medicineUuid,
                                medicineName = medicine?.brandName.orEmpty(),
                                batchId = item.batchId,
                                batchUuid = item.batchUuid,
                                batchNumber = batch?.batchNumber.orEmpty(),
                                quantity = item.quantity,
                                sellingPricePaisa = item.sellingPricePaisa,
                                amountPaisa = item.amountPaisa,
                            )
                        }
                        SaleReturn(
                            id = entity.id,
                            uuid = entity.uuid,
                            saleId = entity.saleId,
                            invoiceUuid = entity.invoiceUuid,
                            customerId = entity.customerId,
                            customerUuid = entity.customerUuid,
                            returnNumber = entity.returnNumber,
                            reason = entity.reason,
                            returnDate = entity.returnDate,
                            subtotalPaisa = entity.subtotalPaisa,
                            discountPaisa = entity.discountPaisa,
                            vatPaisa = entity.vatPaisa,
                            grandTotalPaisa = entity.grandTotalPaisa,
                            notes = entity.notes,
                            items = items,
                            syncVersion = entity.syncVersion,
                        )
                    }
                )
            }
        }

    private suspend fun getSaleReturnById(id: Long): SaleReturn? {
        val entity = saleReturnDao.getById(id) ?: return null
        val items = saleReturnItemDao.getByReturnId(id).map { item ->
            SaleReturnItem(
                id = item.id,
                uuid = item.uuid,
                saleItemId = item.saleItemId,
                invoiceItemUuid = item.invoiceItemUuid,
                medicineId = item.medicineId,
                medicineUuid = item.medicineUuid,
                batchId = item.batchId,
                batchUuid = item.batchUuid,
                quantity = item.quantity,
                sellingPricePaisa = item.sellingPricePaisa,
                amountPaisa = item.amountPaisa,
            )
        }
        return SaleReturn(
            id = entity.id,
            uuid = entity.uuid,
            saleId = entity.saleId,
            invoiceUuid = entity.invoiceUuid,
            customerId = entity.customerId,
            customerUuid = entity.customerUuid,
            returnNumber = entity.returnNumber,
            reason = entity.reason,
            returnDate = entity.returnDate,
            subtotalPaisa = entity.subtotalPaisa,
            discountPaisa = entity.discountPaisa,
            vatPaisa = entity.vatPaisa,
            grandTotalPaisa = entity.grandTotalPaisa,
            notes = entity.notes,
            items = items,
            syncVersion = entity.syncVersion,
        )
    }

    private suspend fun generateReturnNumberInternal(now: Long): String {
        val (start, end) = dayRange(now, 0)
        val count = saleReturnDao.countForDay(start, end) + 1
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        return "SR-$datePart-${count.toString().padStart(4, '0')}"
    }

    private fun dayRange(now: Long, offsetDays: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
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
