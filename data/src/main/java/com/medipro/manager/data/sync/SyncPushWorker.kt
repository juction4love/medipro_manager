package com.medipro.manager.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medipro.manager.core.database.dao.LicenseDao
import com.medipro.manager.domain.repository.FirestoreSyncRepository
import com.medipro.manager.domain.repository.SyncQueueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncPushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueue: SyncQueueRepository,
    private val firestoreSync: FirestoreSyncRepository,
    private val licenseDao: LicenseDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pharmacyId = licenseDao.get()?.licenseId
        if (pharmacyId.isNullOrBlank()) {
            Timber.d("Sync push skipped — no active license/pharmacy")
            return Result.success()
        }

        val pending = syncQueue.getPending(limit = 25)
            .sortedBy { SyncEnqueueHelper.ENTITY_PRIORITY[it.entityType] ?: 99 }
        if (pending.isEmpty()) return Result.success()

        var failures = 0
        pending.forEach { operation ->
            val result = firestoreSync.pushOperation(pharmacyId, operation)
            if (result.isSuccess) {
                syncQueue.markCompleted(operation.uuid)
            } else {
                failures++
                syncQueue.markFailed(operation.uuid, result.exceptionOrNull()?.message ?: "Upload failed")
            }
        }

        return if (failures > 0) Result.retry() else Result.success()
    }
}
