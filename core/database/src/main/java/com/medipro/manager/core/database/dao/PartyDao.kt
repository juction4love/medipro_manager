package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medipro.manager.core.database.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isActive = 1 AND deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Query(
        """
        UPDATE suppliers SET isActive = 0, deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT * FROM suppliers
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            name LIKE '%' || :query || '%' OR
            phone LIKE '%' || :query || '%' OR
            contactPerson LIKE '%' || :query || '%' OR
            panNumber LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 10): List<SupplierEntity>

    @Query("SELECT COUNT(*) FROM suppliers WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0")
    suspend fun countWithOutstanding(): Int

    @Query("SELECT COALESCE(SUM(outstandingBalance), 0) FROM suppliers WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0")
    suspend fun getTotalOutstanding(): Double

    @Query(
        """
        UPDATE suppliers SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isActive = 1 AND deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<com.medipro.manager.core.database.entity.CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): com.medipro.manager.core.database.entity.CustomerEntity?

    @Query("SELECT * FROM customers WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): com.medipro.manager.core.database.entity.CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: com.medipro.manager.core.database.entity.CustomerEntity): Long

    @Update
    suspend fun update(customer: com.medipro.manager.core.database.entity.CustomerEntity)

    @Query(
        """
        UPDATE customers SET isActive = 0, deletedAt = :deletedAt, updatedAt = :deletedAt,
        syncStatus = 'DELETED', syncVersion = syncVersion + 1
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT * FROM customers
        WHERE isActive = 1 AND deletedAt IS NULL AND (
            name LIKE '%' || :query || '%' OR
            phone LIKE '%' || :query || '%' OR
            email LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 10): List<com.medipro.manager.core.database.entity.CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0")
    suspend fun countWithOutstanding(): Int

    @Query("SELECT COALESCE(SUM(outstandingBalance), 0) FROM customers WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0")
    suspend fun getTotalOutstanding(): Double

    @Query(
        """
        SELECT COUNT(DISTINCT customerId) FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND customerId IS NOT NULL AND deletedAt IS NULL
        """
    )
    suspend fun countDistinctCustomersForDay(start: Long, end: Long): Int

    @Query(
        """
        UPDATE customers SET syncStatus = :status, syncVersion = :version,
        updatedAt = :updatedAt WHERE id = :id
        """
    )
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)
}
