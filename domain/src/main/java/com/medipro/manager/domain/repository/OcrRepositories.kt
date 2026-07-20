package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.OcrAnalytics
import com.medipro.manager.domain.model.OcrFeedbackType
import com.medipro.manager.domain.model.OcrMedicineAlias
import com.medipro.manager.domain.model.ScannedPurchaseBill
import kotlinx.coroutines.flow.Flow

interface OcrMedicineAliasRepository {
    suspend fun findMedicineId(ocrDescription: String): Long?
    suspend fun saveMapping(ocrDescription: String, medicineId: Long, medicineName: String, medicineUuid: String? = null)
    suspend fun recordHit(ocrDescription: String)
    fun observeAll(): Flow<List<OcrMedicineAlias>>
    suspend fun delete(id: Long)
    suspend fun deleteAll()
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun updateMapping(id: Long, ocrText: String, medicineId: Long, medicineName: String)
}

interface OcrAnalyticsRepository {
    suspend fun getAnalytics(): OcrAnalytics
    suspend fun logScanSession(bill: ScannedPurchaseBill)
    suspend fun recordManualCorrection()
}

interface OcrFeedbackRepository {
    suspend fun isOptInEnabled(): Boolean
    suspend fun submitAnonymousFeedback(
        bill: ScannedPurchaseBill?,
        feedbackType: OcrFeedbackType,
    ): Result<Unit>
}
