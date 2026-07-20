package com.medipro.manager.data.sync

import com.medipro.manager.core.database.dao.BatchDao
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SupplierDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges local Room FK ids and global UUIDs for cloud sync.
 * Upload: local id → uuid. Download: uuid → local id.
 */
@Singleton
class SyncUuidResolver @Inject constructor(
    private val medicineDao: MedicineDao,
    private val batchDao: BatchDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
) {
    suspend fun medicineUuid(medicineId: Long): String? =
        medicineDao.getById(medicineId)?.uuid

    suspend fun batchUuid(batchId: Long): String? =
        batchDao.getById(batchId)?.uuid

    suspend fun customerUuid(customerId: Long): String? =
        customerDao.getById(customerId)?.uuid

    suspend fun supplierUuid(supplierId: Long): String? =
        supplierDao.getById(supplierId)?.uuid

    suspend fun medicineId(medicineUuid: String): Long? =
        medicineDao.getByUuid(medicineUuid)?.id

    suspend fun batchId(batchUuid: String): Long? =
        batchDao.getByUuid(batchUuid)?.id

    suspend fun customerId(customerUuid: String): Long? =
        customerDao.getByUuid(customerUuid)?.id

    suspend fun supplierId(supplierUuid: String): Long? =
        supplierDao.getByUuid(supplierUuid)?.id

    suspend fun saleId(saleUuid: String): Long? =
        saleDao.getByUuid(saleUuid)?.id

    suspend fun purchaseId(purchaseUuid: String): Long? =
        purchaseDao.getByUuid(purchaseUuid)?.id
}
