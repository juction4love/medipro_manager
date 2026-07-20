package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.medipro.manager.core.database.entity.BatchEntity
import com.medipro.manager.core.database.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE medicineId = :medicineId ORDER BY expiryDate ASC")
    fun observeByMedicine(medicineId: Long): Flow<List<BatchEntity>>

    @Query(
        """
        SELECT b.* FROM batches b
        INNER JOIN stock s ON b.id = s.batchId
        WHERE b.medicineId = :medicineId AND s.quantity > 0
        AND b.expiryDate >= :now AND b.deletedAt IS NULL
        ORDER BY b.expiryDate ASC
        """
    )
    suspend fun getAvailableForSale(medicineId: Long, now: Long = System.currentTimeMillis()): List<BatchEntity>

    @Query(
        """
        SELECT b.* FROM batches b
        INNER JOIN stock s ON b.id = s.batchId
        WHERE b.medicineId = :medicineId AND b.deletedAt IS NULL
        ORDER BY b.expiryDate ASC
        """
    )
    suspend fun getBatchesWithStock(medicineId: Long): List<BatchEntity>

    @Query(
        """
        SELECT b.medicineId, b.batchNumber, b.expiryDate
        FROM batches b
        INNER JOIN stock s ON b.id = s.batchId
        WHERE b.medicineId IN (:medicineIds) AND b.deletedAt IS NULL AND s.quantity > 0
        ORDER BY b.medicineId ASC, b.expiryDate ASC
        """
    )
    suspend fun getBatchPreviewsForMedicines(medicineIds: List<Long>): List<MedicineBatchPreviewRow>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getById(id: Long): BatchEntity?

    @Query("SELECT * FROM batches WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): BatchEntity?

    @Query("SELECT * FROM batches WHERE medicineId = :medicineId AND batchNumber = :batchNumber AND deletedAt IS NULL LIMIT 1")
    suspend fun getByMedicineAndBatch(medicineId: Long, batchNumber: String): BatchEntity?

    @Query("SELECT * FROM batches WHERE expiryDate <= :thresholdDate AND quantity > 0 ORDER BY expiryDate ASC")
    fun observeExpiring(thresholdDate: Long): Flow<List<BatchEntity>>

    @Query("SELECT COUNT(*) FROM batches WHERE expiryDate < :now AND quantity > 0")
    suspend fun countExpired(now: Long): Int

    @Query("SELECT COUNT(*) FROM batches WHERE expiryDate <= :threshold AND expiryDate >= :now AND quantity > 0")
    suspend fun countExpiringWithin(threshold: Long, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: BatchEntity): Long

    @Update
    suspend fun update(batch: BatchEntity)

    @Query(
        """
        UPDATE batches SET deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE batches SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface StockDao {
    @Query(
        """
        SELECT s.* FROM stock s
        INNER JOIN medicines m ON s.medicineId = m.id
        WHERE m.isActive = 1
        ORDER BY m.brandName ASC
        """
    )
    fun observeAll(): Flow<List<StockEntity>>

    @Query("SELECT SUM(quantity) FROM stock WHERE medicineId = :medicineId")
    suspend fun getTotalQuantity(medicineId: Long): Int?

    @Query(
        """
        SELECT m.id, m.brandName as name, COALESCE(SUM(s.quantity), 0) as totalQty, m.reorderLevel
        FROM medicines m
        LEFT JOIN stock s ON m.id = s.medicineId
        WHERE m.isActive = 1
        GROUP BY m.id
        HAVING totalQty <= m.reorderLevel
        """
    )
    fun observeLowStock(): Flow<List<LowStockItem>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock")
    suspend fun getTotalUnits(): Int

    @Query(
        """
        SELECT COALESCE(SUM(s.quantity * b.purchasePrice), 0)
        FROM stock s
        INNER JOIN batches b ON s.batchId = b.id
        WHERE s.quantity > 0
        """
    )
    suspend fun getInventoryValue(): Double

    @Query(
        """
        SELECT COUNT(*) FROM medicines m
        WHERE m.isActive = 1 AND m.deletedAt IS NULL AND COALESCE(
            (SELECT SUM(s.quantity) FROM stock s WHERE s.medicineId = m.id), 0
        ) = 0
        """
    )
    suspend fun countOutOfStock(): Int

    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT m.id FROM medicines m
            LEFT JOIN stock s ON m.id = s.medicineId
            WHERE m.isActive = 1 AND m.deletedAt IS NULL
            GROUP BY m.id
            HAVING COALESCE(SUM(s.quantity), 0) <= m.reorderLevel
            AND COALESCE(SUM(s.quantity), 0) > 0
        )
        """
    )
    suspend fun countLowStock(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockEntity): Long

    @Update
    suspend fun update(stock: StockEntity)

    @Query("SELECT * FROM stock WHERE batchId = :batchId LIMIT 1")
    suspend fun getByBatchId(batchId: Long): StockEntity?
}

data class LowStockItem(
    val id: Long,
    val name: String,
    val totalQty: Int,
    val reorderLevel: Int
)

data class MedicineBatchPreviewRow(
    val medicineId: Long,
    val batchNumber: String,
    val expiryDate: Long,
)
