package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReportDao {

    @Query(
        """
        SELECT COALESCE(SUM(totalAmount), 0) FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getSalesTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(vatAmount), 0) FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getSalesVatTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(discount), 0) FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getSalesDiscountTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(totalAmount), 0) FROM purchases
        WHERE purchaseDate >= :start AND purchaseDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getPurchaseTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(vatAmount), 0) FROM purchases
        WHERE purchaseDate >= :start AND purchaseDate <= :end AND deletedAt IS NULL
        """
    )
    suspend fun getPurchaseVatTotal(start: Long, end: Long): Double

    @Query(
        """
        SELECT (saleDate / 86400000) * 86400000 AS dayMillis,
               COALESCE(SUM(totalAmount), 0) AS amount,
               COUNT(*) AS count
        FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND deletedAt IS NULL
        GROUP BY dayMillis
        ORDER BY dayMillis ASC
        """
    )
    suspend fun getDailySales(start: Long, end: Long): List<DailyAggregateRow>

    @Query(
        """
        SELECT (purchaseDate / 86400000) * 86400000 AS dayMillis,
               COALESCE(SUM(totalAmount), 0) AS amount,
               COUNT(*) AS count
        FROM purchases
        WHERE purchaseDate >= :start AND purchaseDate <= :end AND deletedAt IS NULL
        GROUP BY dayMillis
        ORDER BY dayMillis ASC
        """
    )
    suspend fun getDailyPurchases(start: Long, end: Long): List<DailyAggregateRow>

    @Query(
        """
        SELECT m.brandName AS name, CAST(SUM(si.quantity) AS INTEGER) AS quantity,
               COALESCE(SUM(si.totalPrice), 0) AS amount, m.genericName AS subtitle
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN medicines m ON si.medicineId = m.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        AND s.deletedAt IS NULL AND si.deletedAt IS NULL
        GROUP BY si.medicineId
        ORDER BY quantity DESC
        LIMIT :limit
        """
    )
    suspend fun getMedicineWiseSales(start: Long, end: Long, limit: Int = 20): List<RankedAggregateRow>

    @Query(
        """
        SELECT m.category AS name, CAST(SUM(si.quantity) AS INTEGER) AS quantity,
               COALESCE(SUM(si.totalPrice), 0) AS amount, NULL AS subtitle
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN medicines m ON si.medicineId = m.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        AND s.deletedAt IS NULL AND si.deletedAt IS NULL
        GROUP BY m.category
        ORDER BY amount DESC
        """
    )
    suspend fun getCategoryWiseSales(start: Long, end: Long): List<RankedAggregateRow>

    @Query(
        """
        SELECT m.manufacturer AS name, CAST(SUM(si.quantity) AS INTEGER) AS quantity,
               COALESCE(SUM(si.totalPrice), 0) AS amount, NULL AS subtitle
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN medicines m ON si.medicineId = m.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        AND s.deletedAt IS NULL AND si.deletedAt IS NULL AND m.manufacturer != ''
        GROUP BY m.manufacturer
        ORDER BY amount DESC
        LIMIT :limit
        """
    )
    suspend fun getManufacturerWiseSales(start: Long, end: Long, limit: Int = 20): List<RankedAggregateRow>

    @Query(
        """
        SELECT paymentMethod AS name, COUNT(*) AS quantity,
               COALESCE(SUM(totalAmount), 0) AS amount, NULL AS subtitle
        FROM sales
        WHERE saleDate >= :start AND saleDate <= :end AND deletedAt IS NULL
        GROUP BY paymentMethod
        ORDER BY amount DESC
        """
    )
    suspend fun getSalesByPaymentMethod(start: Long, end: Long): List<RankedAggregateRow>

    @Query(
        """
        SELECT c.name AS name, COUNT(s.id) AS quantity,
               COALESCE(SUM(s.totalAmount), 0) AS amount, NULL AS subtitle
        FROM sales s
        INNER JOIN customers c ON s.customerId = c.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end AND s.deletedAt IS NULL
        GROUP BY s.customerId
        ORDER BY amount DESC
        LIMIT :limit
        """
    )
    suspend fun getTopCustomers(start: Long, end: Long, limit: Int = 10): List<RankedAggregateRow>

    @Query(
        """
        SELECT name, 0 AS quantity, outstandingBalance AS amount, phone AS subtitle
        FROM customers
        WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0
        ORDER BY outstandingBalance DESC
        LIMIT :limit
        """
    )
    suspend fun getCreditCustomers(limit: Int = 20): List<RankedAggregateRow>

    @Query(
        """
        SELECT s.name AS name, COUNT(p.id) AS quantity,
               COALESCE(SUM(p.totalAmount), 0) AS amount, NULL AS subtitle
        FROM purchases p
        INNER JOIN suppliers s ON p.supplierId = s.id
        WHERE p.purchaseDate >= :start AND p.purchaseDate <= :end AND p.deletedAt IS NULL
        GROUP BY p.supplierId
        ORDER BY amount DESC
        LIMIT :limit
        """
    )
    suspend fun getSupplierWisePurchase(start: Long, end: Long, limit: Int = 20): List<RankedAggregateRow>

    @Query(
        """
        SELECT name, 0 AS quantity, outstandingBalance AS amount, phone AS subtitle
        FROM suppliers
        WHERE isActive = 1 AND deletedAt IS NULL AND outstandingBalance > 0
        ORDER BY outstandingBalance DESC
        LIMIT :limit
        """
    )
    suspend fun getSuppliersWithDue(limit: Int = 20): List<RankedAggregateRow>

    @Query(
        """
        SELECT COALESCE(SUM(si.quantity * (si.unitPrice - b.purchasePrice)), 0)
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        INNER JOIN batches b ON si.batchId = b.id
        WHERE s.saleDate >= :start AND s.saleDate <= :end
        AND s.deletedAt IS NULL AND si.deletedAt IS NULL
        """
    )
    suspend fun getGrossProfitEstimate(start: Long, end: Long): Double

    @Query(
        """
        SELECT m.brandName AS name, CAST(SUM(sri.quantity) AS INTEGER) AS quantity,
               COALESCE(SUM(sri.amountPaisa), 0) / 100.0 AS amount, NULL AS subtitle
        FROM sale_return_items sri
        INNER JOIN sale_returns sr ON sri.saleReturnId = sr.id
        INNER JOIN medicines m ON sri.medicineId = m.id
        WHERE sr.returnDate >= :start AND sr.returnDate <= :end AND sr.deletedAt IS NULL
        GROUP BY sri.medicineId
        ORDER BY quantity DESC
        LIMIT :limit
        """
    )
    suspend fun getMostReturnedMedicines(start: Long, end: Long, limit: Int = 10): List<RankedAggregateRow>

    @Query(
        """
        SELECT eventType AS name, COUNT(*) AS quantity, 0.0 AS amount, NULL AS subtitle
        FROM audit_logs
        WHERE createdAt >= :start AND createdAt <= :end AND deletedAt IS NULL
        GROUP BY eventType
        ORDER BY quantity DESC
        """
    )
    suspend fun getAuditByEventType(start: Long, end: Long): List<RankedAggregateRow>

    @Query(
        """
        SELECT m.brandName AS name,
               CAST(COALESCE(SUM(st.quantity), 0) AS INTEGER) AS quantity,
               COALESCE(SUM(st.quantity * b.purchasePrice), 0) AS amount,
               m.category AS subtitle
        FROM medicines m
        INNER JOIN stock st ON m.id = st.medicineId AND st.quantity > 0
        INNER JOIN batches b ON st.batchId = b.id
        WHERE m.isActive = 1 AND m.deletedAt IS NULL
        GROUP BY m.id
        ORDER BY amount DESC
        LIMIT :limit
        """
    )
    suspend fun getTopInventoryValue(limit: Int = 15): List<RankedAggregateRow>

    @Query(
        """
        SELECT m.brandName AS name, CAST(COALESCE(SUM(st.quantity), 0) AS INTEGER) AS quantity,
               COALESCE(SUM(si.quantity), 0) AS amount, m.category AS subtitle
        FROM medicines m
        INNER JOIN stock st ON m.id = st.medicineId AND st.quantity > 0
        LEFT JOIN sale_items si ON si.medicineId = m.id AND si.deletedAt IS NULL
        LEFT JOIN sales s ON si.saleId = s.id
            AND s.saleDate >= :start AND s.saleDate <= :end AND s.deletedAt IS NULL
        WHERE m.isActive = 1 AND m.deletedAt IS NULL
        GROUP BY m.id
        ORDER BY amount ASC
        LIMIT :limit
        """
    )
    suspend fun getSlowMovingMedicines(start: Long, end: Long, limit: Int = 15): List<RankedAggregateRow>

    @Query(
        """
        SELECT m.brandName AS name, CAST(COALESCE(SUM(si.quantity), 0) AS INTEGER) AS quantity,
               COALESCE(SUM(si.totalPrice), 0) AS amount, m.genericName AS subtitle
        FROM medicines m
        LEFT JOIN sale_items si ON si.medicineId = m.id AND si.deletedAt IS NULL
        LEFT JOIN sales s ON si.saleId = s.id
            AND s.saleDate >= :start AND s.saleDate <= :end AND s.deletedAt IS NULL
        WHERE m.isActive = 1 AND m.deletedAt IS NULL
        GROUP BY m.id
        HAVING quantity > 0
        ORDER BY quantity ASC
        LIMIT :limit
        """
    )
    suspend fun getLeastSellingMedicines(start: Long, end: Long, limit: Int = 10): List<RankedAggregateRow>
}

data class DailyAggregateRow(
    val dayMillis: Long,
    val amount: Double,
    val count: Int,
)

data class RankedAggregateRow(
    val name: String,
    val quantity: Int,
    val amount: Double,
    val subtitle: String?,
)
