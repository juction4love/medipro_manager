package com.medipro.manager.domain.model

enum class PurchaseBillMatchStatus {
    MATCHED,
    NEEDS_REVIEW,
    UNMATCHED,
    FREE_ITEM,
}

data class ParsedPurchaseBillLine(
    val serialNumber: Int? = null,
    val hsCode: String? = null,
    val description: String,
    val pack: String? = null,
    val batchNumber: String,
    val expiryRaw: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double,
    val mrp: Double,
    val isFreeItem: Boolean = false,
) {
    fun expiryDateMillis(): Long? {
        val match = Regex("""(20\d{2})[/.-](\d{1,2})""").find(expiryRaw.trim()) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return null
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

enum class MrpUpdateChoice {
    UPDATE,
    KEEP,
}

data class BillScanProgress(
    val stage: String,
    val current: Int = 0,
    val total: Int = 0,
) {
    val progressFraction: Float
        get() = if (total <= 0) 0f else (current.toFloat() / total).coerceIn(0f, 1f)

    fun displayLabel(): String =
        if (total > 0) "$stage $current / $total" else stage
}

data class PurchaseBillLineMatch(
    val parsed: ParsedPurchaseBillLine,
    val match: PosSearchResult?,
    val status: PurchaseBillMatchStatus,
    val confidence: Int = 0,
    val expiryNeedsReview: Boolean = parsed.expiryNeedsManualReview(),
    val matchedViaAlias: Boolean = false,
    val databaseMrp: Double? = null,
    val mrpChanged: Boolean = false,
    val previousUnitPrice: Double? = null,
    val costIncreasePercent: Double? = null,
    val nearExpiry: Boolean = false,
    val isControlled: Boolean = false,
)

data class ScannedPurchaseBill(
    val supplierName: String?,
    val matchedSupplierId: Long? = null,
    val matchedSupplierName: String? = null,
    val invoiceNumber: String?,
    val invoiceDate: String?,
    val lines: List<PurchaseBillLineMatch>,
    val netTotal: Double? = null,
    val rawOcrText: String? = null,
    val parseWarnings: List<String> = emptyList(),
    val parserUsed: String = "Generic",
    val pageCount: Int = 1,
    val duplicateBill: DuplicateSupplierBill? = null,
    val sourceImageUris: List<String> = emptyList(),
) {
    val matchedCount: Int get() = lines.count { it.status == PurchaseBillMatchStatus.MATCHED }
    val reviewCount: Int get() = lines.count { it.status == PurchaseBillMatchStatus.NEEDS_REVIEW }
    val unmatchedCount: Int get() = lines.count { it.status == PurchaseBillMatchStatus.UNMATCHED }
    val freeItemCount: Int get() = lines.count { it.status == PurchaseBillMatchStatus.FREE_ITEM }
    val controlledCount: Int get() = lines.count { it.isControlled }
    val itemCount: Int get() = lines.size
    val supplierFound: Boolean get() = matchedSupplierId != null

    fun estimatedImportTotal(selectedIndexes: Set<Int>): Double =
        lines.mapIndexed { index, line ->
            if (index in selectedIndexes && !line.parsed.isFreeItem) line.parsed.amount else 0.0
        }.sum()
}

/** Prefill for creating a medicine from an unmatched OCR line. */
data class OcrMedicineDraft(
    val brandName: String,
    val genericName: String = "",
    val strength: String = "",
    val dosageForm: String = "Tablet",
    val manufacturer: String = "",
    val unit: String = "pcs",
    val mrp: Double = 0.0,
    val purchasePrice: Double = 0.0,
)

fun ParsedPurchaseBillLine.toMedicineDraft(): OcrMedicineDraft {
    val desc = description.trim()
    val strengthMatch = Regex("""(\d+(?:\.\d+)?\s*(?:MG|GM|ML|MCG|IU|%)?)""", RegexOption.IGNORE_CASE).find(desc)
    val dosageForm = when {
        desc.contains("TAB", ignoreCase = true) -> "Tablet"
        desc.contains("CAP", ignoreCase = true) -> "Capsule"
        desc.contains("SYR", ignoreCase = true) || desc.contains("SUSP", ignoreCase = true) -> "Syrup"
        desc.contains("INJ", ignoreCase = true) -> "Injection"
        desc.contains("DROP", ignoreCase = true) -> "Drops"
        else -> "Tablet"
    }
    val packUnit = pack?.let { if (it.contains("x", ignoreCase = true)) "Strip" else "pcs" } ?: "pcs"
    return OcrMedicineDraft(
        brandName = desc,
        strength = strengthMatch?.value?.trim().orEmpty(),
        dosageForm = dosageForm,
        unit = packUnit,
        mrp = mrp,
        purchasePrice = unitPrice,
    )
}

fun ParsedPurchaseBillLine.isNearExpiry(): Boolean {
    val expiryMs = expiryDateMillis() ?: return false
    val oneMonthMs = 30L * 24 * 60 * 60 * 1000
    return expiryMs <= System.currentTimeMillis() + oneMonthMs
}

fun ParsedPurchaseBillLine.expiryNeedsManualReview(): Boolean {
    val match = Regex("""(20\d{2})[/.-](\d{1,2})""").find(expiryRaw.trim()) ?: return true
    val year = match.groupValues[1].toIntOrNull() ?: return true
    val month = match.groupValues[2].toIntOrNull() ?: return true
    if (month !in 1..12) return true
    val now = java.util.Calendar.getInstance()
    val currentYear = now.get(java.util.Calendar.YEAR)
    if (year < currentYear) return true
    if (year > currentYear + 10) return true
    return false
}

data class DuplicateSupplierBill(
    val purchaseId: Long,
    val internalInvoiceNumber: String,
    val supplierBillNumber: String,
)
