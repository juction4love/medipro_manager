package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medipro.manager.core.database.entity.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StockAdjustmentEntity): Long

    @Query("SELECT * FROM stock_adjustments WHERE id = :id")
    suspend fun getById(id: Long): StockAdjustmentEntity?

    @Query("SELECT * FROM stock_adjustments WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): StockAdjustmentEntity?

    @Query("SELECT * FROM stock_adjustments WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StockAdjustmentEntity>>

    @Query(
        """
        SELECT * FROM stock_adjustments
        WHERE deletedAt IS NULL AND type = :type
        ORDER BY createdAt DESC
        """
    )
    fun observeByType(type: String): Flow<List<StockAdjustmentEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM stock_adjustments
        WHERE createdAt >= :start AND createdAt <= :end AND deletedAt IS NULL
        """
    )
    suspend fun countForDay(start: Long, end: Long): Int

    @Query(
        """
        SELECT COUNT(*) FROM stock_adjustments
        WHERE adjustmentNumber LIKE :prefix || '%' AND deletedAt IS NULL
        """
    )
    suspend fun countByNumberPrefix(prefix: String): Int

    @Query(
        """
        UPDATE stock_adjustments SET deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1 WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE stock_adjustments SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}
