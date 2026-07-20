package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.medipro.manager.core.database.entity.PurchaseEntity
import com.medipro.manager.core.database.entity.PurchaseItemEntity
import com.medipro.manager.core.database.entity.SaleEntity
import com.medipro.manager.core.database.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE purchaseDate >= :startDate AND purchaseDate <= :endDate")
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: Long): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity): Long

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM purchases WHERE purchaseDate >= :start AND purchaseDate <= :end")
    suspend fun getTotalPurchase(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalAmount - paidAmount), 0) FROM purchases WHERE paymentStatus != 'PAID'")
    suspend fun getPendingPayments(): Double

    @Query("SELECT COUNT(*) FROM purchases WHERE paymentStatus != 'PAID'")
    suspend fun countPendingPayments(): Int

    @Query(
        """
        SELECT * FROM purchases
        WHERE deletedAt IS NULL AND invoiceNumber LIKE '%' || :query || '%'
        ORDER BY purchaseDate DESC
        LIMIT :limit
        """
    )
    suspend fun searchByInvoice(query: String, limit: Int = 10): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE deletedAt IS NULL AND invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getByInvoiceNumber(invoiceNumber: String): PurchaseEntity?

    @Query(
        """
        SELECT * FROM purchases
        WHERE deletedAt IS NULL
          AND notes LIKE '%Supplier Bill: ' || :supplierBillNumber || '%'
          AND (:supplierId IS NULL OR supplierId = :supplierId)
        LIMIT 1
        """,
    )
    suspend fun findBySupplierBillNumber(supplierBillNumber: String, supplierId: Long? = null): PurchaseEntity?

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PurchaseEntity>>

    @Query("SELECT COUNT(*) FROM purchases WHERE purchaseDate >= :start AND purchaseDate <= :end")
    suspend fun countForDay(start: Long, end: Long): Int

    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM purchases WHERE purchaseDate >= :start AND purchaseDate <= :end")
    suspend fun getTotalPaidToday(start: Long, end: Long): Double

    @Query("SELECT * FROM purchases WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): PurchaseEntity?

    @Query("UPDATE purchases SET syncStatus = :status, syncVersion = :version, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)

    @Query("UPDATE purchases SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'DELETED', syncVersion = syncVersion + 1 WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())
}

@Dao
interface PurchaseItemDao {
    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId AND deletedAt IS NULL")
    suspend fun getByPurchaseId(purchaseId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseItemEntity>)

    @Query("UPDATE purchase_items SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE purchaseId = :purchaseId AND deletedAt IS NULL")
    suspend fun softDeleteByPurchaseId(purchaseId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT unitPrice FROM purchase_items
        WHERE medicineId = :medicineId AND deletedAt IS NULL
        ORDER BY createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLastUnitPrice(medicineId: Long): Double?
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE saleDate >= :startDate AND saleDate <= :endDate ORDER BY saleDate DESC")
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE saleDate >= :start AND saleDate <= :end")
    suspend fun getTotalSales(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE saleDate >= :start AND saleDate <= :end AND paymentMethod = 'CASH'")
    suspend fun getCashSales(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE saleDate >= :start AND saleDate <= :end AND isCredit = 1")
    suspend fun getCreditSales(start: Long, end: Long): Double

    @Query(
        """
        SELECT * FROM sales
        WHERE deletedAt IS NULL AND (
            invoiceNumber LIKE '%' || :query || '%' OR
            patientName LIKE '%' || :query || '%'
        )
        ORDER BY saleDate DESC
        LIMIT :limit
        """
    )
    suspend fun searchByInvoice(query: String, limit: Int = 10): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE deletedAt IS NULL AND invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getByInvoiceNumber(invoiceNumber: String): SaleEntity?

    @Query("SELECT * FROM sales ORDER BY saleDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SaleEntity>>

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :startOfDay AND saleDate <= :endOfDay")
    suspend fun countForDay(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM sales WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): SaleEntity?

    @Query("UPDATE sales SET syncStatus = :status, syncVersion = :version, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncState(id: Long, status: String, version: Long, updatedAt: Long)

    @Query("UPDATE sales SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'DELETED', syncVersion = syncVersion + 1 WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE sales SET printCount = printCount + 1, lastPrintedAt = :printedAt, updatedAt = :printedAt
        WHERE id = :id AND deletedAt IS NULL
        """
    )
    suspend fun recordPrint(id: Long, printedAt: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM sales WHERE saleDate >= :start AND saleDate <= :end")
    suspend fun getTotalCollectedToday(start: Long, end: Long): Double
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL")
    suspend fun getBySaleId(saleId: Long): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleItemEntity>)

    @Query("UPDATE sale_items SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE saleId = :saleId AND deletedAt IS NULL")
    suspend fun softDeleteBySaleId(saleId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT m.brandName AS name, CAST(SUM(si.quantity) AS INTEGER) AS quantity
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN medicines m ON si.medicineId = m.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        GROUP BY si.medicineId
        ORDER BY quantity DESC
        LIMIT 1
        """
    )
    suspend fun getBestSellingToday(start: Long, end: Long): TopSellingItem?

    @Query(
        """
        SELECT m.category AS category, CAST(SUM(si.quantity) AS INTEGER) AS quantity
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN medicines m ON si.medicineId = m.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        GROUP BY m.category
        ORDER BY quantity DESC
        LIMIT 1
        """
    )
    suspend fun getTopCategoryToday(start: Long, end: Long): TopCategoryItem?
}

data class TopSellingItem(
    val name: String,
    val quantity: Int,
)

data class TopCategoryItem(
    val category: String,
    val quantity: Int,
)
