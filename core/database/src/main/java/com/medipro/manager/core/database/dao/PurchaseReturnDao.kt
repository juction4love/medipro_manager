package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medipro.manager.core.database.entity.PurchaseReturnEntity
import com.medipro.manager.core.database.entity.PurchaseReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PurchaseReturnEntity): Long

    @Query("SELECT * FROM purchase_returns WHERE id = :id")
    suspend fun getById(id: Long): PurchaseReturnEntity?

    @Query("SELECT * FROM purchase_returns WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): PurchaseReturnEntity?

    @Query("SELECT * FROM purchase_returns WHERE deletedAt IS NULL ORDER BY returnDate DESC")
    fun observeAll(): Flow<List<PurchaseReturnEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(grandTotalPaisa), 0) FROM purchase_returns
        WHERE returnDate >= :start AND returnDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getTotalReturnPaisaForDay(start: Long, end: Long): Long

    @Query(
        """
        SELECT COUNT(*) FROM purchase_returns
        WHERE returnDate >= :start AND returnDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun countForDay(start: Long, end: Long): Int

    @Query(
        """
        UPDATE purchase_returns SET deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1 WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE purchase_returns SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface PurchaseReturnItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseReturnItemEntity>)

    @Query(
        """
        SELECT * FROM purchase_return_items
        WHERE purchaseReturnId = :returnId AND deletedAt IS NULL
        """
    )
    suspend fun getByReturnId(returnId: Long): List<PurchaseReturnItemEntity>

    @Query(
        """
        SELECT COALESCE(SUM(quantity), 0) FROM purchase_return_items pri
        INNER JOIN purchase_returns pr ON pri.purchaseReturnId = pr.id
        WHERE pri.purchaseItemUuid = :purchaseItemUuid
        AND pri.deletedAt IS NULL AND pr.deletedAt IS NULL
        """
    )
    suspend fun getReturnedQtyForPurchaseItem(purchaseItemUuid: String): Int
}
