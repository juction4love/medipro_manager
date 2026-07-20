package com.medipro.manager.data.repository

import android.content.Context
import com.medipro.manager.core.database.MediProDatabase
import com.medipro.manager.core.database.dao.BackupHistoryDao
import com.medipro.manager.core.database.entity.BackupHistoryEntity
import com.medipro.manager.core.security.BackupFileCrypto
import com.medipro.manager.data.backup.BackupRestoreHelper
import com.medipro.manager.data.mapper.toDomain
import com.medipro.manager.data.sync.SyncScheduler
import com.medipro.manager.domain.model.BackupRecord
import com.medipro.manager.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MediProDatabase,
    private val backupHistoryDao: BackupHistoryDao,
    private val backupFileCrypto: BackupFileCrypto,
    private val syncScheduler: SyncScheduler,
) : BackupRepository {

    override suspend fun createBackup(password: CharArray, backupType: String): Result<BackupRecord> =
        withContext(Dispatchers.IO) {
            try {
                runCatching {
                    require(password.isNotEmpty()) { "Backup password is required" }

                    val sourceDb = context.getDatabasePath(MediProDatabase.DATABASE_NAME)
                    require(sourceDb.exists()) { "Database not found" }

                    val tempDir = File(context.cacheDir, "backup_staging").apply { mkdirs() }
                    val stagingDb = File(tempDir, "medipro_staging.db")

                    database.openHelper.writableDatabase.use { _ ->
                        BackupRestoreHelper.checkpointAndCopyDatabase(sourceDb, stagingDb)
                    }

                    val compressed = BackupRestoreHelper.gzipCompress(stagingDb)
                    val encrypted = backupFileCrypto.encrypt(compressed, password)

                    val backupsDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
                    val fileName = BackupRestoreHelper.buildBackupFileName()
                    val outputFile = File(backupsDir, fileName)
                    outputFile.writeBytes(encrypted)

                    stagingDb.delete()
                    File(stagingDb.path + "-wal").delete()
                    File(stagingDb.path + "-shm").delete()

                    val entity = BackupHistoryEntity(
                        fileName = fileName,
                        filePath = outputFile.absolutePath,
                        fileSize = outputFile.length(),
                        isEncrypted = true,
                        backupType = backupType,
                    )
                    backupHistoryDao.insert(entity)

                    backupHistoryDao.getLatest()?.toDomain()
                        ?: BackupRecord(
                            fileName = fileName,
                            filePath = outputFile.absolutePath,
                            fileSize = outputFile.length(),
                            isEncrypted = true,
                            backupType = backupType,
                        )
                }.onFailure { Timber.e(it, "Backup failed") }
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun restoreBackup(filePath: String, password: CharArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                runCatching {
                    require(password.isNotEmpty()) { "Backup password is required" }

                    val backupFile = File(filePath)
                    require(backupFile.exists()) { "Backup file not found" }

                    val encrypted = backupFile.readBytes()
                    val compressed = backupFileCrypto.decrypt(encrypted, password)

                    val tempDir = File(context.cacheDir, "restore_staging").apply { mkdirs() }
                    val restoredDb = File(tempDir, "medipro_restored.db")
                    BackupRestoreHelper.gzipDecompressToFile(compressed, restoredDb)
                    BackupRestoreHelper.verifyIntegrity(restoredDb)
                    BackupRestoreHelper.prepareRestoredDatabase(restoredDb)

                    database.close()

                    val targetDb = context.getDatabasePath(MediProDatabase.DATABASE_NAME)
                    BackupRestoreHelper.replaceDatabase(restoredDb, targetDb)

                    restoredDb.delete()
                    File(restoredDb.path + "-wal").delete()
                    File(restoredDb.path + "-shm").delete()

                    syncScheduler.schedulePush()
                    Timber.i("Database restored from ${backupFile.name}")
                }.onFailure { Timber.e(it, "Restore failed") }
            } finally {
                password.fill('\u0000')
            }
        }

    override fun observeBackupHistory(): Flow<List<BackupRecord>> =
        backupHistoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLatestBackup(): BackupRecord? =
        backupHistoryDao.getLatest()?.toDomain()
}
