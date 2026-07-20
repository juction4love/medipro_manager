package com.medipro.manager.data.backup

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal object BackupRestoreHelper {

    private val SYNC_TABLES = listOf(
        "medicines",
        "customers",
        "suppliers",
        "batches",
        "purchases",
        "purchase_items",
        "sales",
        "sale_items",
        "payments",
        "ledger",
        "stock_adjustments",
        "purchase_returns",
        "purchase_return_items",
        "sale_returns",
        "sale_return_items",
        "settings",
        "audit_logs",
    )

    fun checkpointAndCopyDatabase(sourceDb: File, destDb: File) {
        SQLiteDatabase.openDatabase(
            sourceDb.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
        }
        sourceDb.copyTo(destDb, overwrite = true)
        copyIfExists(File(sourceDb.path + "-wal"), File(destDb.path + "-wal"))
        copyIfExists(File(sourceDb.path + "-shm"), File(destDb.path + "-shm"))

        SQLiteDatabase.openDatabase(
            destDb.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.execSQL("DELETE FROM pending_operations")
            markSyncPending(db)
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
        }
    }

    fun gzipCompress(input: File): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        GZIPOutputStream(output).use { gzip ->
            FileInputStream(input).use { it.copyTo(gzip) }
        }
        return output.toByteArray()
    }

    fun gzipDecompressToFile(compressed: ByteArray, outputFile: File) {
        GZIPInputStream(compressed.inputStream()).use { gzip ->
            FileOutputStream(outputFile).use { out -> gzip.copyTo(out) }
        }
    }

    fun prepareRestoredDatabase(dbFile: File) {
        SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { db ->
            db.execSQL("DELETE FROM pending_operations")
            markSyncPending(db)
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
        }
    }

    fun verifyIntegrity(dbFile: File) {
        SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { db ->
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst()) { "integrity_check returned no rows" }
                val result = cursor.getString(0)
                require(result.equals("ok", ignoreCase = true)) {
                    "Database integrity check failed: $result"
                }
            }
        }
    }

    fun replaceDatabase(restoredDb: File, targetDb: File) {
        deleteDatabaseFiles(targetDb)
        restoredDb.copyTo(targetDb, overwrite = true)
        copyIfExists(File(restoredDb.path + "-wal"), File(targetDb.path + "-wal"))
        copyIfExists(File(restoredDb.path + "-shm"), File(targetDb.path + "-shm"))
    }

    fun deleteDatabaseFiles(dbFile: File) {
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    fun buildBackupFileName(now: Long = System.currentTimeMillis()): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
        return "MediPro_Backup_${formatter.format(java.util.Date(now))}.medipro"
    }

    private fun markSyncPending(db: SQLiteDatabase) {
        SYNC_TABLES.forEach { table ->
            runCatching {
                db.execSQL(
                    "UPDATE `$table` SET syncStatus = 'PENDING' WHERE syncStatus IS NOT NULL AND syncStatus != 'DELETED'"
                )
            }
        }
    }

    private fun copyIfExists(source: File, dest: File) {
        if (source.exists()) {
            source.copyTo(dest, overwrite = true)
        } else {
            dest.delete()
        }
    }
}
