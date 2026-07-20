package com.medipro.manager.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medipro.manager.core.security.BackupPassphraseStore
import com.medipro.manager.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val passphraseStore: BackupPassphraseStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val passphrase = passphraseStore.getPassphrase()
        if (passphrase == null) {
            Timber.w("Auto backup skipped — no passphrase configured")
            return Result.success()
        }
        return try {
            backupRepository.createBackup(passphrase, backupType = "AUTO")
                .fold(
                    onSuccess = {
                        Timber.i("Auto backup created: ${it.fileName}")
                        Result.success()
                    },
                    onFailure = {
                        Timber.e(it, "Auto backup failed")
                        Result.retry()
                    },
                )
        } finally {
            passphrase.fill('\u0000')
        }
    }
}
