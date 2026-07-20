package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medipro.manager.core.database.entity.SaleReturnEntity
import com.medipro.manager.core.database.entity.SaleReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SaleReturnEntity): Long

    @Query("SELECT * FROM sale_returns WHERE id = :id")
    suspend fun getById(id: Long): SaleReturnEntity?

    @Query("SELECT * FROM sale_returns WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): SaleReturnEntity?

    @Query("SELECT * FROM sale_returns WHERE deletedAt IS NULL ORDER BY returnDate DESC")
    fun observeAll(): Flow<List<SaleReturnEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(grandTotalPaisa), 0) FROM sale_returns
        WHERE returnDate >= :start AND returnDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getTotalReturnPaisaForDay(start: Long, end: Long): Long

    @Query(
        """
        SELECT COUNT(*) FROM sale_returns
        WHERE returnDate >= :start AND returnDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun countForDay(start: Long, end: Long): Int

    @Query(
        """
        UPDATE sale_returns SET deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1 WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE sale_returns SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface SaleReturnItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleReturnItemEntity>)

    @Query(
        """
        SELECT * FROM sale_return_items
        WHERE saleReturnId = :returnId AND deletedAt IS NULL
        """
    )
    suspend fun getByReturnId(returnId: Long): List<SaleReturnItemEntity>

    @Query(
        """
        SELECT COALESCE(SUM(quantity), 0) FROM sale_return_items sri
        INNER JOIN sale_returns sr ON sri.saleReturnId = sr.id
        WHERE sri.invoiceItemUuid = :invoiceItemUuid
        AND sri.deletedAt IS NULL AND sr.deletedAt IS NULL
        """
    )
    suspend fun getReturnedQtyForSaleItem(invoiceItemUuid: String): Int
}
