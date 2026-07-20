package com.medipro.manager.domain.usecase.ocr

import com.medipro.manager.domain.model.OcrAnalytics
import com.medipro.manager.domain.model.OcrFeedbackType
import com.medipro.manager.domain.model.OcrMedicineAlias
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.repository.OcrAnalyticsRepository
import com.medipro.manager.domain.repository.OcrFeedbackRepository
import com.medipro.manager.domain.repository.OcrMedicineAliasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOcrAliasesUseCase @Inject constructor(
    private val repository: OcrMedicineAliasRepository,
) {
    operator fun invoke(): Flow<List<OcrMedicineAlias>> = repository.observeAll()
}

class DeleteOcrAliasUseCase @Inject constructor(
    private val repository: OcrMedicineAliasRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

class DeleteAllOcrAliasesUseCase @Inject constructor(
    private val repository: OcrMedicineAliasRepository,
) {
    suspend operator fun invoke() = repository.deleteAll()
}

class SetOcrAliasEnabledUseCase @Inject constructor(
    private val repository: OcrMedicineAliasRepository,
) {
    suspend operator fun invoke(id: Long, enabled: Boolean) = repository.setEnabled(id, enabled)
}

class UpdateOcrAliasUseCase @Inject constructor(
    private val repository: OcrMedicineAliasRepository,
) {
    suspend operator fun invoke(id: Long, ocrText: String, medicineId: Long, medicineName: String) =
        repository.updateMapping(id, ocrText, medicineId, medicineName)
}

class GetOcrAnalyticsUseCase @Inject constructor(
    private val repository: OcrAnalyticsRepository,
) {
    suspend operator fun invoke(): OcrAnalytics = repository.getAnalytics()
}

class LogOcrScanSessionUseCase @Inject constructor(
    private val repository: OcrAnalyticsRepository,
) {
    suspend operator fun invoke(bill: ScannedPurchaseBill) = repository.logScanSession(bill)
}

class RecordOcrManualCorrectionUseCase @Inject constructor(
    private val repository: OcrAnalyticsRepository,
) {
    suspend operator fun invoke() = repository.recordManualCorrection()
}

class SubmitOcrFeedbackUseCase @Inject constructor(
    private val repository: OcrFeedbackRepository,
) {
    suspend operator fun invoke(
        bill: ScannedPurchaseBill?,
        feedbackType: OcrFeedbackType,
    ): Result<Unit> = repository.submitAnonymousFeedback(bill, feedbackType)
}

class IsOcrFeedbackOptInUseCase @Inject constructor(
    private val repository: OcrFeedbackRepository,
) {
    suspend operator fun invoke(): Boolean = repository.isOptInEnabled()
}
