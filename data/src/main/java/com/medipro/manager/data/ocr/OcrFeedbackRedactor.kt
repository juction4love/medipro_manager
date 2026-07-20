package com.medipro.manager.data.ocr

import com.medipro.manager.data.purchasebill.OcrTextNormalizer
import com.medipro.manager.domain.model.ScannedPurchaseBill

object OcrFeedbackRedactor {
    private val phonePattern = Regex("""\b9[78]\d{8}\b""")
    private val panPattern = Regex("""\b\d{9}\b""")
    private val sensitiveLinePattern = Regex(
        """(?i)(invoice|bill\s*no|pan|vat|mobile|phone|contact|customer|buyer|total|grand|amount|subtotal|net\s*amount|pharmacy|chemist|drug\s*store|m/s|ms\.|p\.?v\.?t|ltd\.?|pvt\.?)\s*[:#]?\s*\S+""",
    )
    private val mostlyNumericLine = Regex("""^[\d\s.,:/\-+%Rs]+$""", RegexOption.IGNORE_CASE)
    private val devanagariPattern = Regex("""[\u0900-\u097F]""")

    fun buildNormalizedText(bill: ScannedPurchaseBill?): String {
        if (bill == null) return ""
        if (bill.lines.isNotEmpty()) {
            return bill.lines
                .map { OcrTextNormalizer.normalize(it.parsed.description) }
                .filter { it.length in 3..120 }
                .distinct()
                .take(30)
                .joinToString("\n")
        }
        return extractMedicineLinesFromOcr(bill.rawOcrText)
    }

    fun detectSupplier(bill: ScannedPurchaseBill?): String? =
        bill?.matchedSupplierName?.trim()?.takeIf { it.isNotBlank() }
            ?: bill?.supplierName?.trim()?.takeIf { it.isNotBlank() }

    fun detectConfidence(bill: ScannedPurchaseBill?): Int {
        if (bill == null || bill.lines.isEmpty()) return 0
        return bill.lines.map { it.confidence }.average().toInt().coerceIn(0, 100)
    }

    fun detectLineCount(bill: ScannedPurchaseBill?): Int =
        bill?.lines?.size ?: extractMedicineLinesFromOcr(bill?.rawOcrText).lines().count { it.isNotBlank() }

    fun detectLanguage(bill: ScannedPurchaseBill?): String {
        val sample = buildString {
            append(bill?.rawOcrText.orEmpty())
            bill?.lines?.forEach { append(' ').append(it.parsed.description) }
        }
        if (sample.isBlank()) return "en"
        val hasDevanagari = devanagariPattern.containsMatchIn(sample)
        val hasLatin = sample.any { it.isLetter() && it.code < 128 }
        return when {
            hasDevanagari && hasLatin -> "mixed"
            hasDevanagari -> "ne"
            else -> "en"
        }
    }

    private fun extractMedicineLinesFromOcr(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.lines()
            .map { it.trim() }
            .filter { line -> line.length in 4..120 }
            .filterNot { isSensitiveLine(it) }
            .map { sanitizeLine(it) }
            .filter { it.length in 3..120 }
            .filterNot { mostlyNumericLine.matches(it) }
            .map { OcrTextNormalizer.normalize(it) }
            .filter { it.length in 3..120 }
            .distinct()
            .take(25)
            .joinToString("\n")
    }

    private fun isSensitiveLine(line: String): Boolean {
        if (phonePattern.containsMatchIn(line)) return true
        if (panPattern.containsMatchIn(line) && line.length <= 24) return true
        if (sensitiveLinePattern.containsMatchIn(line)) return true
        return false
    }

    private fun sanitizeLine(line: String): String =
        line
            .replace(phonePattern, "")
            .replace(panPattern, "")
            .replace(Regex("""\b\d{1,3}(?:,\d{3})*(?:\.\d+)?\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
