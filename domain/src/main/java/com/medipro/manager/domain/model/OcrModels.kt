package com.medipro.manager.domain.model

data class OcrMedicineAlias(
    val id: Long,
    val ocrText: String,
    val medicineId: Long,
    val medicineName: String,
    val hitCount: Int,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class OcrLastScan(
    val scannedAt: Long,
    val supplierName: String?,
    val accuracyPercent: Int,
)

data class OcrAnalytics(
    val todayBillsScanned: Int = 0,
    val averageAccuracyPercent: Int = 0,
    val learnedAliasesCount: Int = 0,
    val manualCorrectionsToday: Int = 0,
    val savedTimeMinutes: Int = 0,
    val lastScan: OcrLastScan? = null,
) {
    fun savedTimeLabel(): String {
        if (savedTimeMinutes <= 0) return "0 min"
        val hours = savedTimeMinutes / 60
        val mins = savedTimeMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours} hr ${mins} min"
            hours > 0 -> "${hours} hr"
            else -> "${mins} min"
        }
    }
}

enum class OcrFeedbackType {
    PARSE_FAIL,
    UNKNOWN_FORMAT,
    MANUAL_IMPROVE,
}
