package com.medipro.manager.data.repository

import com.medipro.manager.core.database.dao.OcrMedicineAliasDao
import com.medipro.manager.core.database.dao.OcrScanSessionDao
import com.medipro.manager.core.database.entity.OcrScanSessionEntity
import com.medipro.manager.domain.model.OcrAnalytics
import com.medipro.manager.domain.model.OcrLastScan
import com.medipro.manager.domain.model.PurchaseBillMatchStatus
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.repository.OcrAnalyticsRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class OcrAnalyticsRepositoryImpl @Inject constructor(
    private val sessionDao: OcrScanSessionDao,
    private val aliasDao: OcrMedicineAliasDao,
) : OcrAnalyticsRepository {

    override suspend fun getAnalytics(): OcrAnalytics {
        purgeOldSessions()
        val startOfDay = startOfTodayMillis()
        val sevenDaysAgo = startOfDay - 7L * 24 * 60 * 60 * 1000
        val accuracy = sessionDao.averageAccuracySince(sevenDaysAgo)?.roundToInt() ?: 0
        val savedSeconds = sessionDao.sumSavedSecondsSince(startOfDay)
        val latest = sessionDao.getLatest()
        return OcrAnalytics(
            todayBillsScanned = sessionDao.countSince(startOfDay),
            averageAccuracyPercent = accuracy.coerceIn(0, 100),
            learnedAliasesCount = aliasDao.countEnabled(),
            manualCorrectionsToday = sessionDao.sumManualCorrectionsSince(startOfDay),
            savedTimeMinutes = (savedSeconds / 60.0).roundToInt(),
            lastScan = latest?.toLastScan(),
        )
    }

    override suspend fun logScanSession(bill: ScannedPurchaseBill) {
        if (bill.lines.isEmpty()) return
        purgeOldSessions()
        val matched = bill.lines.count {
            it.status == PurchaseBillMatchStatus.MATCHED || it.status == PurchaseBillMatchStatus.NEEDS_REVIEW
        }
        val aliasMatched = bill.lines.count { it.matchedViaAlias }
        val avgConfidence = bill.lines.map { it.confidence }.average().roundToInt()
        sessionDao.insert(
            OcrScanSessionEntity(
                scannedAt = System.currentTimeMillis(),
                pageCount = bill.pageCount,
                totalLines = bill.lines.size,
                matchedLines = matched,
                aliasMatchedLines = aliasMatched,
                manualCorrections = 0,
                avgConfidence = avgConfidence,
                parserUsed = bill.parserUsed,
                supplierName = bill.matchedSupplierName ?: bill.supplierName,
            ),
        )
    }

    override suspend fun recordManualCorrection() {
        val startOfDay = startOfTodayMillis()
        val latestId = sessionDao.latestSessionIdSince(startOfDay)
        if (latestId != null) {
            sessionDao.incrementManualCorrections(latestId)
        } else {
            sessionDao.insert(
                OcrScanSessionEntity(
                    scannedAt = System.currentTimeMillis(),
                    totalLines = 0,
                    matchedLines = 0,
                    aliasMatchedLines = 0,
                    manualCorrections = 1,
                ),
            )
        }
    }

    private suspend fun purgeOldSessions() {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 60 * 60 * 1000
        sessionDao.deleteOlderThan(cutoff)
    }

    private fun OcrScanSessionEntity.toLastScan(): OcrLastScan {
        val accuracy = if (totalLines > 0) {
            (matchedLines * 100.0 / totalLines).roundToInt().coerceIn(0, 100)
        } else {
            avgConfidence.coerceIn(0, 100)
        }
        return OcrLastScan(
            scannedAt = scannedAt,
            supplierName = supplierName,
            accuracyPercent = accuracy,
        )
    }

    private fun startOfTodayMillis(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    companion object {
        const val RETENTION_DAYS = 180
    }
}
