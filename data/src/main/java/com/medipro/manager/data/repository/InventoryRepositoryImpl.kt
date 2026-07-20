package com.medipro.manager.data.repository

import androidx.room.withTransaction
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.StockAdjustmentDao
import com.medipro.manager.core.database.dao.StockDao
import com.medipro.manager.core.database.entity.AuditEventType
import com.medipro.manager.core.database.entity.StockAdjustmentEntity
import com.medipro.manager.core.database.entity.StockAdjustmentType as EntityAdjustmentType
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.BatchStockDetail
import com.medipro.manager.domain.model.ExpiryReportRow
import com.medipro.manager.domain.model.InventoryMedicineStock
import com.medipro.manager.domain.model.InventorySummary
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.StockAdjustmentContext
import com.medipro.manager.domain.model.StockAdjustmentType
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val database: MediProDatabase,
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val stockDao: StockDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val licenseDao: LicenseDao,
    private val auditRepository: AuditRepository,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : InventoryRepository {

    override fun observeSummary(): Flow<InventorySummary> =
        stockDao.observeLowStock().flatMapLatest { lowStock ->
            flow {
                emit(buildSummary(System.currentTimeMillis()).copy(lowStockCount = lowStock.size))
            }
        }

    private suspend fun buildSummary(now: Long): InventorySummary {
        val (start, end) = dayRange(0)
        return InventorySummary(
            inventoryValue = stockDao.getInventoryValue(),
            totalStockUnits = stockDao.getTotalUnits(),
            activeMedicineCount = medicineDao.count(),
            lowStockCount = 0,
            outOfStockCount = stockDao.countOutOfStock(),
            nearExpiryCount = batchDao.countExpiringWithin(now + 30L * DAY_MS, now),
            expiredCount = batchDao.countExpired(now),
            todayAdjustments = stockAdjustmentDao.countForDay(start, end),
        )
    }

    override suspend fun searchMedicinesWithStock(query: String): List<InventoryMedicineStock> {
        if (query.isBlank()) return emptyList()
        return medicineDao.search(query).first().mapNotNull { entity ->
            buildMedicineStock(entity.id)?.takeIf { it.totalQty >= 0 }
        }
    }

    override suspend fun getMedicineStock(medicineId: Long): InventoryMedicineStock? =
        buildMedicineStock(medicineId)

    override suspend fun getAdjustmentContext(batchId: Long): StockAdjustmentContext? {
        val batch = batchDao.getById(batchId) ?: return null
        if (batch.deletedAt != null) return null
        val medicine = medicineDao.getById(batch.medicineId) ?: return null
        val stock = stockDao.getByBatchId(batchId) ?: return null
        val now = System.currentTimeMillis()
        return StockAdjustmentContext(
            medicine = medicine.toDomain(stock.quantity),
            batch = batch.toBatchDetail(stock, now),
        )
    }

    override suspend fun createAdjustment(
        batchId: Long,
        type: StockAdjustmentType,
        quantity: Int,
        reason: String,
        remarks: String?,
    ): Long {
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(reason.isNotBlank()) { "Reason is required" }

        val batch = batchDao.getById(batchId)
            ?: throw IllegalStateException("Batch not found")
        val medicine = medicineDao.getById(batch.medicineId)
            ?: throw IllegalStateException("Medicine not found")
        val stock = stockDao.getByBatchId(batchId)
            ?: throw IllegalStateException("Stock not found — adjustment requires an existing batch")

        val pharmacyUuid = licenseDao.get()?.licenseId.orEmpty()
        val deviceId = licenseDao.get()?.deviceId
        val medUuid = medicine.uuid
        val batUuid = batch.uuid

        return database.withTransaction {
            val now = System.currentTimeMillis()
            val oldQty = stock.quantity
            val (newQty, signedAdjust) = computeQuantities(type, oldQty, quantity)

            when (type) {
                StockAdjustmentType.DAMAGE, StockAdjustmentType.EXPIRED -> {
                    require(quantity > 0) { "Enter quantity to mark as ${type.label.lowercase()}" }
                    require(quantity <= oldQty) {
                        "Cannot mark $quantity — only $oldQty sellable units available"
                    }
                    stockDao.update(
                        stock.copy(
                            quantity = newQty,
                            damagedQuantity = stock.damagedQuantity + quantity,
                            lastUpdated = now,
                        )
                    )
                    batchDao.update(
                        batch.copy(
                            quantity = newQty,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = batch.syncVersion + 1,
                        )
                    )
                }
                else -> {
                    require(newQty >= 0) { "Resulting quantity cannot be negative" }
                    stockDao.update(
                        stock.copy(quantity = newQty, lastUpdated = now)
                    )
                    batchDao.update(
                        batch.copy(
                            quantity = newQty,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                            syncVersion = batch.syncVersion + 1,
                        )
                    )
                }
            }

            val adjustmentNumber = generateAdjustmentNumberInternal(now)
            val adjustmentUuid = UUID.randomUUID().toString()
            val entityType = type.name

            val adjustmentId = stockAdjustmentDao.insert(
                StockAdjustmentEntity(
                    uuid = adjustmentUuid,
                    pharmacyUuid = pharmacyUuid,
                    adjustmentNumber = adjustmentNumber,
                    medicineId = medicine.id,
                    medicineUuid = medUuid,
                    batchId = batchId,
                    batchUuid = batUuid,
                    type = entityType,
                    oldQty = oldQty,
                    adjustQty = signedAdjust,
                    newQty = newQty,
                    reason = reason,
                    remarks = remarks,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    syncVersion = 1,
                    deviceId = deviceId,
                )
            )

            adjustmentId
        }.also { adjustmentId ->
            getAdjustmentById(adjustmentId)?.let { saved ->
                auditRepository.log(
                    AuditLog(
                        uuid = UUID.randomUUID().toString(),
                        eventType = auditEventFor(type),
                        entityType = "STOCK_ADJUSTMENT",
                        entityUuid = saved.uuid,
                        description = "${type.label}: ${saved.medicineName} / ${saved.batchNumber} ($saved.adjustQty)",
                        oldValue = saved.oldQty.toString(),
                        newValue = saved.newQty.toString(),
                    )
                )
                syncEnqueueHelper.enqueueStockBatchById(batchId)
                syncEnqueueHelper.enqueueStockAdjustment(saved)
            }
        }
    }

    override fun observeAdjustments(): Flow<List<StockAdjustment>> =
        stockAdjustmentDao.observeAll().flatMapLatest { list ->
            flow { emit(list.mapNotNull { mapAdjustment(it) }) }
        }

    override fun observeDamageAdjustments(): Flow<List<StockAdjustment>> =
        stockAdjustmentDao.observeByType(EntityAdjustmentType.DAMAGE).flatMapLatest { list ->
            flow { emit(list.mapNotNull { mapAdjustment(it) }) }
        }

    override fun observeExpiryReport(): Flow<List<ExpiryReportRow>> =
        batchDao.observeExpiring(System.currentTimeMillis() + 365L * DAY_MS).flatMapLatest { batches ->
            flow {
                emit(
                    batches.mapNotNull { batch ->
                        val medicine = medicineDao.getById(batch.medicineId) ?: return@mapNotNull null
                        val stock = stockDao.getByBatchId(batch.id)
                        ExpiryReportRow(
                            medicineName = medicine.brandName,
                            batchNumber = batch.batchNumber,
                            expiryDate = batch.expiryDate,
                            remainingQty = stock?.quantity ?: batch.quantity,
                        )
                    }
                )
            }
        }

    private suspend fun buildMedicineStock(medicineId: Long): InventoryMedicineStock? {
        val medicine = medicineDao.getById(medicineId) ?: return null
        val now = System.currentTimeMillis()
        val batches = batchDao.getBatchesWithStock(medicineId).mapNotNull { batch ->
            val stock = stockDao.getByBatchId(batch.id) ?: return@mapNotNull null
            batch.toBatchDetail(stock, now)
        }
        val totalQty = batches.sumOf { it.sellableQty }
        return InventoryMedicineStock(
            medicine = medicine.toDomain(totalQty),
            totalQty = totalQty,
            batches = batches,
        )
    }

    private suspend fun mapAdjustment(entity: StockAdjustmentEntity): StockAdjustment? {
        val medicine = medicineDao.getById(entity.medicineId) ?: return null
        val batch = batchDao.getById(entity.batchId) ?: return null
        return entity.toDomain(medicine.brandName, batch.batchNumber)
    }

    private suspend fun getAdjustmentById(id: Long): StockAdjustment? =
        stockAdjustmentDao.getById(id)?.let { mapAdjustment(it) }

    private fun computeQuantities(
        type: StockAdjustmentType,
        oldQty: Int,
        inputQty: Int,
    ): Pair<Int, Int> = when (type) {
        StockAdjustmentType.PHYSICAL_COUNT -> {
            val newQty = inputQty
            newQty to (newQty - oldQty)
        }
        StockAdjustmentType.STOCK_INCREASE,
        StockAdjustmentType.OPENING_STOCK,
        StockAdjustmentType.FREE_SAMPLE -> {
            require(inputQty > 0) { "Enter quantity to add" }
            (oldQty + inputQty) to inputQty
        }
        StockAdjustmentType.STOCK_DECREASE,
        StockAdjustmentType.MANUAL_CORRECTION,
        StockAdjustmentType.LOST -> {
            require(inputQty > 0) { "Enter quantity to remove" }
            (oldQty - inputQty) to -inputQty
        }
        StockAdjustmentType.DAMAGE,
        StockAdjustmentType.EXPIRED -> {
            (oldQty - inputQty) to -inputQty
        }
    }

    private suspend fun generateAdjustmentNumberInternal(now: Long): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val prefix = "SA-$datePart-"
        val count = stockAdjustmentDao.countByNumberPrefix(prefix) + 1
        return prefix + count.toString().padStart(4, '0')
    }

    private fun auditEventFor(type: StockAdjustmentType): String = when (type) {
        StockAdjustmentType.DAMAGE -> AuditEventType.DAMAGE
        StockAdjustmentType.EXPIRED -> AuditEventType.EXPIRED
        StockAdjustmentType.OPENING_STOCK -> AuditEventType.OPENING_STOCK
        StockAdjustmentType.PHYSICAL_COUNT -> AuditEventType.PHYSICAL_COUNT
        else -> AuditEventType.STOCK_ADJUSTMENT
    }

    private fun com.medipro.manager.core.database.entity.BatchEntity.toBatchDetail(
        stock: com.medipro.manager.core.database.entity.StockEntity,
        now: Long,
    ) = BatchStockDetail(
        batchId = id,
        batchUuid = uuid,
        batchNumber = batchNumber,
        expiryDate = expiryDate,
        sellableQty = stock.quantity,
        damagedQty = stock.damagedQuantity,
        purchasePrice = purchasePrice,
        isExpired = expiryDate < now,
    )

    private fun StockAdjustmentEntity.toDomain(medicineName: String, batchNumber: String) =
        StockAdjustment(
            id = id,
            uuid = uuid,
            adjustmentNumber = adjustmentNumber,
            medicineId = medicineId,
            medicineUuid = medicineUuid,
            medicineName = medicineName,
            batchId = batchId,
            batchUuid = batchUuid,
            batchNumber = batchNumber,
            type = StockAdjustmentType.fromKey(type),
            oldQty = oldQty,
            adjustQty = adjustQty,
            newQty = newQty,
            reason = reason,
            remarks = remarks,
            createdAt = createdAt,
            syncVersion = syncVersion,
        )

    private fun dayRange(dayOffset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    companion object {
        private const val DAY_MS = 86_400_000L
    }
}
