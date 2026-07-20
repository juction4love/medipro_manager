package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.model.Supplier
import com.medipro.manager.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val supplierDao: SupplierDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : SupplierRepository {

    override fun observeSuppliers(): Flow<List<Supplier>> =
        supplierDao.observeAll().map { suppliers -> suppliers.map { it.toDomain() } }

    override suspend fun addSupplier(supplier: Supplier): Long {
        val now = System.currentTimeMillis()
        val license = licenseDao.get()
        val entity = supplier.toEntity().copy(
            pharmacyUuid = licenseDao.pharmacyUuid(),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = 0,
            deviceId = license?.deviceId,
        )
        val id = supplierDao.insert(entity)
        supplierDao.getById(id)?.let { syncEnqueueHelper.enqueueSupplier(it) }
        return id
    }

    override suspend fun updateSupplier(supplier: Supplier) {
        val existing = supplierDao.getById(supplier.id) ?: return
        val now = System.currentTimeMillis()
        val updated = supplier.toEntity().copy(
            pharmacyUuid = existing.pharmacyUuid,
            createdAt = existing.createdAt,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = existing.syncVersion + 1,
            deviceId = existing.deviceId ?: licenseDao.get()?.deviceId,
        )
        supplierDao.update(updated)
        syncEnqueueHelper.enqueueSupplier(updated)
    }
}
