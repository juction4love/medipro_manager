package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.AuditLog
import com.medipro.manager.domain.model.PendingOperation
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    suspend fun log(event: AuditLog)
    fun observeRecent(limit: Int = 100): Flow<List<AuditLog>>
}

interface SyncQueueRepository {
    suspend fun enqueue(operation: PendingOperation)
    fun observePending(): Flow<List<PendingOperation>>
    fun observePendingCount(): Flow<Int>
    suspend fun getPending(limit: Int = 50): List<PendingOperation>
    suspend fun countPending(): Int
    suspend fun markCompleted(uuid: String)
    suspend fun markFailed(uuid: String, error: String)
}
