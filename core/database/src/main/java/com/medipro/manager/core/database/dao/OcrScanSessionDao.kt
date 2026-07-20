package com.medipro.manager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.medipro.manager.core.database.entity.OcrScanSessionEntity

@Dao
interface OcrScanSessionDao {
    @Insert
    suspend fun insert(session: OcrScanSessionEntity): Long

    @Query("SELECT COUNT(*) FROM ocr_scan_sessions WHERE scannedAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query(
        """
        SELECT AVG(CASE WHEN totalLines > 0 THEN (matchedLines * 100.0 / totalLines) ELSE 0 END)
        FROM ocr_scan_sessions
        WHERE scannedAt >= :since AND totalLines > 0
        """,
    )
    suspend fun averageAccuracySince(since: Long): Double?

    @Query("SELECT COALESCE(SUM(manualCorrections), 0) FROM ocr_scan_sessions WHERE scannedAt >= :since")
    suspend fun sumManualCorrectionsSince(since: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(aliasMatchedLines * 45 + (matchedLines - aliasMatchedLines) * 25), 0)
        FROM ocr_scan_sessions
        WHERE scannedAt >= :since
        """,
    )
    suspend fun sumSavedSecondsSince(since: Long): Int

    @Query(
        """
        SELECT id FROM ocr_scan_sessions
        WHERE scannedAt >= :since
        ORDER BY scannedAt DESC
        LIMIT 1
        """,
    )
    suspend fun latestSessionIdSince(since: Long): Long?

    @Query("UPDATE ocr_scan_sessions SET manualCorrections = manualCorrections + 1 WHERE id = :id")
    suspend fun incrementManualCorrections(id: Long)

    @Query("SELECT * FROM ocr_scan_sessions ORDER BY scannedAt DESC LIMIT 1")
    suspend fun getLatest(): OcrScanSessionEntity?

    @Query("DELETE FROM ocr_scan_sessions WHERE scannedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
