package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.mapper.toEntity
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : CustomerRepository {

    override fun observeCustomers(): Flow<List<Customer>> =
        customerDao.observeAll().map { customers -> customers.map { it.toDomain() } }

    override suspend fun addCustomer(customer: Customer): Long {
        val now = System.currentTimeMillis()
        val license = licenseDao.get()
        val entity = customer.toEntity().copy(
            pharmacyUuid = licenseDao.pharmacyUuid(),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = 0,
            deviceId = license?.deviceId,
        )
        val id = customerDao.insert(entity)
        customerDao.getById(id)?.let { syncEnqueueHelper.enqueueCustomer(it) }
        return id
    }

    override suspend fun updateCustomer(customer: Customer) {
        val existing = customerDao.getById(customer.id) ?: return
        val now = System.currentTimeMillis()
        val updated = customer.toEntity().copy(
            pharmacyUuid = existing.pharmacyUuid,
            createdAt = existing.createdAt,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = existing.syncVersion + 1,
            deviceId = existing.deviceId ?: licenseDao.get()?.deviceId,
        )
        customerDao.update(updated)
        syncEnqueueHelper.enqueueCustomer(updated)
    }
}
