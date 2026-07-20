package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.LedgerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PaymentDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.PurchaseItemDao
import com.medipro.manager.core.database.dao.PurchaseReturnDao
import com.medipro.manager.core.database.dao.PurchaseReturnItemDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.AuditEventType
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.LedgerEntity
import com.medipro.manager.core.database.entity.PaymentEntity
import com.medipro.manager.core.database.entity.PurchaseReturnEntity
import com.medipro.manager.core.database.entity.PurchaseReturnItemEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.PurchaseReturn
import com.medipro.manager.domain.model.PurchaseReturnContext
import com.medipro.manager.domain.model.PurchaseReturnItem
import com.medipro.manager.domain.model.PurchaseReturnLine
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.PurchaseReturnRepository
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
class PurchaseReturnRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val purchaseReturnItemDao: PurchaseReturnItemDao,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val supplierDao: SupplierDao,
    private val paymentDao: PaymentDao,
    private val ledgerDao: LedgerDao,
    private val licenseDao: LicenseDao,
    private val auditRepository: AuditRepository,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : PurchaseReturnRepository {

    override suspend fun getReturnContext(purchaseId: Long): PurchaseReturnContext? {
        val purchase = purchaseDao.getById(purchaseId) ?: return null
        if (purchase.deletedAt != null) return null

        val items = purchaseItemDao.getByPurchaseId(purchaseId)
        val supplierName = purchase.supplierId?.let { supplierDao.getById(it)?.name }
        val purchaseDomain = purchase.toDomain(
            items.map { item ->
                val medicine = medicineDao.getById(item.medicineId)
                item.toDomain(medicine?.brandName.orEmpty())
            },
            supplierName,
        )

        val lines = items.mapNotNull { item ->
            val batchUuid = item.batchUuid ?: return@mapNotNull null
            val batch = batchDao.getByUuid(batchUuid) ?: batchDao.getByMedicineAndBatch(
                item.medicineId,
                item.batchNumber,
            ) ?: return@mapNotNull null
            val stock = stockDao.getByBatchId(batch.id)?.quantity ?: 0
            val alreadyReturned = purchaseReturnItemDao.getReturnedQtyForPurchaseItem(item.uuid)
            val medicine = medicineDao.getById(item.medicineId)

            PurchaseReturnLine(
                purchaseItemId = item.id,
                purchaseItemUuid = item.uuid,
                medicineId = item.medicineId,
                medicineUuid = item.medicineUuid,
                medicineName = medicine?.brandName.orEmpty(),
                batchId = batch.id,
                batchUuid = batch.uuid,
                batchNumber = item.batchNumber,
                purchasedQty = item.quantity,
                alreadyReturnedQty = alreadyReturned,
                currentBatchStock = stock,
                unitPrice = item.unitPrice,
                vatPercent = item.vatPercent,
            )
        }

        return PurchaseReturnContext(purchase = purchaseDomain, lines = lines)
    }

    override suspend fun createPurchaseReturn(
        purchaseId: Long,
        reason: String,
        lines: List<PurchaseReturnLine>,
        notes: String?,
    ): Long {
        val activeLines = lines.filter { it.returnQty > 0 }
        require(activeLines.isNotEmpty()) { "Select at least one item to return" }

        val purchase = purchaseDao.getById(purchaseId)
            ?: throw IllegalStateException("Purchase not found")
        val pharmacyUuid = licenseDao.get()?.licenseId.orEmpty()
        val deviceId = licenseDao.get()?.deviceId

        return database.withTransaction {
            val now = System.currentTimeMillis()
            activeLines.forEach { line ->
                require(line.returnQty <= line.maxReturnableQty) {
                    "Return qty ${line.returnQty} exceeds max ${line.maxReturnableQty} for ${line.medicineName}"
                }
                val stock = stockDao.getByBatchId(line.batchId)
                    ?: throw IllegalStateException("Stock not found for batch ${line.batchNumber}")
                if (stock.quantity < line.returnQty) {
                    throw IllegalStateException(
                        "Insufficient stock for ${line.medicineName}. Available: ${stock.quantity}"
                    )
                }
            }

            val subtotalPaisa = activeLines.sumOf { (it.lineSubtotal * 100).roundToLong() }
            val vatPaisa = activeLines.sumOf { (it.lineVat * 100).roundToLong() }
            val grandTotalPaisa = subtotalPaisa + vatPaisa
            val returnNumber = generateReturnNumberInternal(now)
            val returnUuid = UUID.randomUUID().toString()

            val returnId = purchaseReturnDao.insert(
                PurchaseReturnEntity(
                    uuid = returnUuid,
                    pharmacyUuid = pharmacyUuid,
                    purchaseId = purchaseId,
                    purchaseUuid = purchase.uuid,
                    supplierId = purchase.supplierId,
                    supplierUuid = purchase.supplierUuid,
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
                val costPaisa = (line.unitPrice * 100).roundToLong()
                PurchaseReturnItemEntity(
                    purchaseReturnId = returnId,
                    purchaseReturnUuid = returnUuid,
                    purchaseItemId = line.purchaseItemId,
                    purchaseItemUuid = line.purchaseItemUuid,
                    medicineId = line.medicineId,
                    medicineUuid = line.medicineUuid,
                    batchId = line.batchId,
                    batchUuid = line.batchUuid,
                    quantity = line.returnQty,
                    costPricePaisa = costPaisa,
                    amountPaisa = costPaisa * line.returnQty,
                    pharmacyUuid = pharmacyUuid,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = deviceId,
                )
            }
            purchaseReturnItemDao.insertAll(itemEntities)

            activeLines.forEach { line ->
                val stock = stockDao.getByBatchId(line.batchId)!!
                stockDao.update(
                    stock.copy(
                        quantity = stock.quantity - line.returnQty,
                        lastUpdated = now,
                    )
                )
                val batch = batchDao.getById(line.batchId)
                if (batch != null) {
                    batchDao.update(
                        batch.copy(
                            quantity = maxOf(0, batch.quantity - line.returnQty),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = batch.syncVersion + 1,
                        )
                    )
                }
            }

            val returnTotal = grandTotalPaisa / 100.0
            val purchaseTotal = purchase.totalAmount.coerceAtLeast(0.01)
            val ratio = returnTotal / purchaseTotal
            val cashRefund = min(purchase.paidAmount * ratio, returnTotal)
            val supplierReduction = returnTotal - cashRefund

            ledgerDao.insert(
                LedgerEntity(
                    accountType = "PURCHASE",
                    description = "Purchase return $returnNumber",
                    credit = returnTotal,
                    referenceType = "PURCHASE_RETURN",
                    referenceId = returnId,
                    referenceUuid = returnUuid,
                    entryDate = now,
                    pharmacyUuid = pharmacyUuid,
                )
            )

            val supplierId = purchase.supplierId
            if (supplierReduction > 0 && supplierId != null) {
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "SUPPLIER",
                        accountId = supplierId,
                        accountUuid = purchase.supplierUuid,
                        description = "Return credit $returnNumber",
                        debit = supplierReduction,
                        referenceType = "PURCHASE_RETURN",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        entryDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
                supplierDao.getById(supplierId)?.let { supplier ->
                    supplierDao.update(
                        supplier.copy(
                            outstandingBalance = maxOf(0.0, supplier.outstandingBalance - supplierReduction),
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = supplier.syncVersion + 1,
                        )
                    )
                }
            }

            if (cashRefund > 0) {
                paymentDao.insert(
                    PaymentEntity(
                        type = "PURCHASE_RETURN",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        amount = cashRefund,
                        paymentMethod = purchase.paymentMethod,
                        notes = "Refund for $returnNumber",
                        paymentDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
                ledgerDao.insert(
                    LedgerEntity(
                        accountType = "CASH",
                        description = "Refund $returnNumber",
                        debit = cashRefund,
                        referenceType = "PURCHASE_RETURN",
                        referenceId = returnId,
                        referenceUuid = returnUuid,
                        entryDate = now,
                        pharmacyUuid = pharmacyUuid,
                    )
                )
            }

            returnId
        }.also { returnId ->
            val saved = getPurchaseReturnById(returnId) ?: return@also
            auditRepository.log(
                AuditLog(
                    uuid = UUID.randomUUID().toString(),
                    eventType = AuditEventType.PURCHASE_RETURN,
                    entityType = "PURCHASE_RETURN",
                    entityUuid = saved.uuid,
                    description = "Purchase return ${saved.returnNumber}: ${saved.reason}",
                    newValue = saved.grandTotal.toString(),
                )
            )
            syncEnqueueHelper.enqueuePurchaseReturn(saved)
        }
    }

    override suspend fun generateReturnNumber(): String =
        generateReturnNumberInternal(System.currentTimeMillis())

    override fun observePurchaseReturns(): Flow<List<PurchaseReturn>> =
        purchaseReturnDao.observeAll().flatMapLatest { returns ->
            flow {
                emit(
                    returns.map { entity ->
                        val items = purchaseReturnItemDao.getByReturnId(entity.id).map { item ->
                            val medicine = medicineDao.getById(item.medicineId)
                            val batch = batchDao.getById(item.batchId)
                            PurchaseReturnItem(
                                id = item.id,
                                uuid = item.uuid,
                                purchaseItemId = item.purchaseItemId,
                                purchaseItemUuid = item.purchaseItemUuid,
                                medicineId = item.medicineId,
                                medicineUuid = item.medicineUuid,
                                medicineName = medicine?.brandName.orEmpty(),
                                batchId = item.batchId,
                                batchUuid = item.batchUuid,
                                batchNumber = batch?.batchNumber.orEmpty(),
                                quantity = item.quantity,
                                costPricePaisa = item.costPricePaisa,
                                amountPaisa = item.amountPaisa,
                            )
                        }
                        PurchaseReturn(
                            id = entity.id,
                            uuid = entity.uuid,
                            purchaseId = entity.purchaseId,
                            purchaseUuid = entity.purchaseUuid,
                            supplierId = entity.supplierId,
                            supplierUuid = entity.supplierUuid,
                            returnNumber = entity.returnNumber,
                            reason = entity.reason,
                            returnDate = entity.returnDate,
                            subtotalPaisa = entity.subtotalPaisa,
                            vatPaisa = entity.vatPaisa,
                            discountPaisa = entity.discountPaisa,
                            grandTotalPaisa = entity.grandTotalPaisa,
                            notes = entity.notes,
                            items = items,
                            syncVersion = entity.syncVersion,
                        )
                    }
                )
            }
        }

    private suspend fun getPurchaseReturnById(id: Long): PurchaseReturn? {
        val entity = purchaseReturnDao.getById(id) ?: return null
        val items = purchaseReturnItemDao.getByReturnId(id).map { item ->
            PurchaseReturnItem(
                id = item.id,
                uuid = item.uuid,
                purchaseItemId = item.purchaseItemId,
                purchaseItemUuid = item.purchaseItemUuid,
                medicineId = item.medicineId,
                medicineUuid = item.medicineUuid,
                batchId = item.batchId,
                batchUuid = item.batchUuid,
                quantity = item.quantity,
                costPricePaisa = item.costPricePaisa,
                amountPaisa = item.amountPaisa,
            )
        }
        return PurchaseReturn(
            id = entity.id,
            uuid = entity.uuid,
            purchaseId = entity.purchaseId,
            purchaseUuid = entity.purchaseUuid,
            supplierId = entity.supplierId,
            supplierUuid = entity.supplierUuid,
            returnNumber = entity.returnNumber,
            reason = entity.reason,
            returnDate = entity.returnDate,
            subtotalPaisa = entity.subtotalPaisa,
            vatPaisa = entity.vatPaisa,
            discountPaisa = entity.discountPaisa,
            grandTotalPaisa = entity.grandTotalPaisa,
            notes = entity.notes,
            items = items,
            syncVersion = entity.syncVersion,
        )
    }

    private suspend fun generateReturnNumberInternal(now: Long): String {
        val (start, end) = dayRange(now, 0)
        val count = purchaseReturnDao.countForDay(start, end) + 1
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        return "PR-$datePart-${count.toString().padStart(4, '0')}"
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
