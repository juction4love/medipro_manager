package com.medipro.manager.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.medipro.manager.core.database.dao.SettingsDao
import com.medipro.manager.data.document.AppVersionProvider
import com.medipro.manager.data.ocr.OcrFeedbackRedactor
import com.medipro.manager.domain.model.OcrFeedbackType
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.repository.OcrFeedbackRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrFeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsDao: SettingsDao,
    private val appVersionProvider: AppVersionProvider,
) : OcrFeedbackRepository {

    override suspend fun isOptInEnabled(): Boolean =
        settingsDao.get()?.ocrFeedbackOptIn == true

    override suspend fun submitAnonymousFeedback(
        bill: ScannedPurchaseBill?,
        feedbackType: OcrFeedbackType,
    ): Result<Unit> = runCatching {
        if (!isOptInEnabled()) {
            throw IllegalStateException("OCR feedback sharing is disabled")
        }
        val normalizedText = OcrFeedbackRedactor.buildNormalizedText(bill)
        if (normalizedText.isBlank()) {
            throw IllegalStateException("No safe OCR sample to share")
        }
        val payload = mapOf(
            "appVersion" to appVersionProvider.versionName,
            "parserName" to (bill?.parserUsed?.takeIf { it.isNotBlank() } ?: "Generic"),
            "supplierDetected" to OcrFeedbackRedactor.detectSupplier(bill),
            "confidence" to OcrFeedbackRedactor.detectConfidence(bill),
            "lineCount" to OcrFeedbackRedactor.detectLineCount(bill),
            "ocrLanguage" to OcrFeedbackRedactor.detectLanguage(bill),
            "normalizedText" to normalizedText,
            "createdAt" to System.currentTimeMillis(),
        )
        firestore.collection(ANONYMOUS_COLLECTION).add(payload).await()
    }

    companion object {
        private const val ANONYMOUS_COLLECTION = "ocr_feedback_anonymous"
    }
}
