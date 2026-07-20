package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.PendingOperation
import com.medipro.manager.domain.model.SyncStatusSnapshot
import kotlinx.coroutines.flow.Flow

interface FirestoreSyncRepository {
    /** Upload one queued operation as a Firestore document (document ID = entityUuid). */
    suspend fun pushOperation(pharmacyId: String, operation: PendingOperation): Result<Unit>

    /** Start real-time listeners for inbound sync. */
    fun startListening(pharmacyId: String, localDeviceId: String?)

    fun stopListening()

    fun observeSyncStatus(): Flow<SyncStatusSnapshot>
}
