package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.AuditLogDao
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.core.database.dao.PendingOperationDao
import com.medipro.manager.core.database.entity.AuditLogEntity
import com.medipro.manager.core.database.entity.PendingOperationEntity
import com.medipro.manager.core.database.entity.SyncStatus
import com.medipro.manager.data.sync.SyncEnqueueHelper
import com.medipro.manager.data.sync.pharmacyUuid
import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.PendingOperation
import com.medipro.manager.domain.repository.AuditRepository
import com.medipro.manager.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val licenseDao: LicenseDao,
    private val syncEnqueueHelper: SyncEnqueueHelper,
) : AuditRepository {

    override suspend fun log(event: AuditLog) {
        val now = System.currentTimeMillis()
        val license = licenseDao.get()
        val entity = AuditLogEntity(
            uuid = event.uuid.ifBlank { UUID.randomUUID().toString() },
            pharmacyUuid = licenseDao.pharmacyUuid(),
            eventType = event.eventType,
            entityType = event.entityType,
            entityUuid = event.entityUuid,
            description = event.description,
            oldValue = event.oldValue,
            newValue = event.newValue,
            deviceId = license?.deviceId,
            createdAt = event.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            syncVersion = 1,
        )
        auditLogDao.insert(entity)
        syncEnqueueHelper.enqueueAuditLog(entity)
    }

    override fun observeRecent(limit: Int): Flow<List<AuditLog>> =
        auditLogDao.observeRecent(limit).map { logs ->
            logs.map { it.toDomain() }
        }

    private fun AuditLogEntity.toDomain() = AuditLog(
        uuid = uuid,
        eventType = eventType,
        entityType = entityType,
        entityUuid = entityUuid,
        description = description,
        oldValue = oldValue,
        newValue = newValue,
        createdAt = createdAt
    )
}

@Singleton
class SyncQueueRepositoryImpl @Inject constructor(
    private val pendingOperationDao: PendingOperationDao
) : SyncQueueRepository {

    override suspend fun enqueue(operation: PendingOperation) {
        pendingOperationDao.insert(
            PendingOperationEntity(
                uuid = operation.uuid,
                operationType = operation.operationType,
                entityType = operation.entityType,
                entityUuid = operation.entityUuid,
                payloadJson = operation.payloadJson,
                status = operation.status,
                retryCount = operation.retryCount,
                createdAt = operation.createdAt
            )
        )
    }

    override fun observePending(): Flow<List<PendingOperation>> =
        pendingOperationDao.observePending().map { ops ->
            ops.map { it.toDomain() }
        }

    override suspend fun getPending(limit: Int): List<PendingOperation> =
        pendingOperationDao.getPending(limit).map { it.toDomain() }

    override fun observePendingCount(): Flow<Int> =
        pendingOperationDao.observePendingCount()

    override suspend fun countPending(): Int = pendingOperationDao.countPending()

    override suspend fun markCompleted(uuid: String) {
        pendingOperationDao.delete(uuid)
    }

    override suspend fun markFailed(uuid: String, error: String) {
        val pending = pendingOperationDao.getPending(500).find { it.uuid == uuid } ?: return
        pendingOperationDao.update(
            pending.copy(
                status = "PENDING",
                retryCount = pending.retryCount + 1,
                lastError = error,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun PendingOperationEntity.toDomain() = PendingOperation(
        uuid = uuid,
        operationType = operationType,
        entityType = entityType,
        entityUuid = entityUuid,
        payloadJson = payloadJson,
        status = status,
        retryCount = retryCount,
        createdAt = createdAt
    )
}
