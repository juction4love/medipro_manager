package com.medipro.manager.data.purchasebill

import com.medipro.manager.domain.model.ParsedPurchaseBillLine
import com.medipro.manager.domain.model.Supplier
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

data class ParsedWholesaleBill(
    val supplierName: String?,
    val invoiceNumber: String?,
    val invoiceDate: String?,
    val lines: List<ParsedPurchaseBillLine>,
    val netTotal: Double?,
    val warnings: List<String>,
)

/**
 * Heuristic parser for Nepal wholesale pharmacy invoices (Surya Medico, Asian, etc.).
 * Works on ML Kit OCR plain text — tolerant of spacing and comma formatting.
 */
object NepalWholesaleBillParser {

    private val invoiceNoPattern = Regex("""(?i)invoice\s*no\.?\s*[:.]?\s*([A-Z0-9][A-Z0-9\-/]+)""")
    private val invoiceNoFallbackPattern = Regex("""(?i)(?:bill|inv)\s*[:.#]?\s*([A-Z0-9]{4,}[A-Z0-9\-/]*)""")
    private val adDatePattern = Regex("""(20\d{2})[/.-](\d{1,2})[/.-](\d{1,2})""")
    private val bsDatePattern = Regex("""(20[78]\d)[/.-](\d{1,2})[/.-](\d{1,2})""")
    private val batchPattern = Regex("""\b([A-Z]{2,5}[-/]?\d{3,8})\b""")
    private val expiryPattern = Regex("""(20\d{2})[/.-](\d{1,2})""")
    private val packPattern = Regex("""\b(\d+\s*[xX]\s*\d+)\b""")
    private val hsCodePattern = Regex("""\b(3004\d{0,4})\b""")
    private val freeLinePattern = Regex("""(?i)(?:-\s*do\s*-|-do-|^\s*\d+\s+.*\bFREE\b)""")
    private val freeQtyPattern = Regex("""(?i)(\d+)\s*FREE""")
    private val serialStartPattern = Regex("""^\s*(\d{1,3})\b""")
    private val totalPattern = Regex("""(?i)(?:net\s+)?total|in words|due date|above\s+\d+|total dues""")

    fun peekSupplierName(ocrText: String): String? =
        extractSupplierName(normalizeLines(ocrText))

    fun parse(ocrText: String): ParsedWholesaleBill {
        val warnings = mutableListOf<String>()
        val normalized = normalizeLines(ocrText)

        val fullText = normalized.joinToString("\n")
        val supplierName = extractSupplierName(normalized)
        val invoiceNumber = invoiceNoPattern.find(fullText)?.groupValues?.get(1)?.trim()
            ?: invoiceNoFallbackPattern.find(fullText)?.groupValues?.get(1)?.trim()
        val invoiceDate = extractInvoiceDate(fullText)
        val netTotal = extractNetTotal(fullText)

        val tableStart = normalized.indexOfFirst { line ->
            line.contains("ITEM", ignoreCase = true) &&
                (line.contains("DESCRIPTION", ignoreCase = true) || line.contains("BATCH", ignoreCase = true))
        }.let { if (it >= 0) it + 1 else findFirstItemLine(normalized) }

        val tableLines = if (tableStart >= 0) {
            normalized.drop(tableStart).takeWhile { !totalPattern.containsMatchIn(it) }
        } else {
            warnings.add("Item table header not found — scanning all lines")
            normalized.filter { serialStartPattern.containsMatchIn(it) }
        }

        val lines = mutableListOf<ParsedPurchaseBillLine>()
        var lastPaidLine: ParsedPurchaseBillLine? = null

        tableLines.forEach { line ->
            if (freeLinePattern.containsMatchIn(line)) {
                parseFreeLine(line, lastPaidLine)?.let { lines.add(it) }
                return@forEach
            }
            parsePaidLine(line)?.let { parsed ->
                lines.add(parsed)
                lastPaidLine = parsed
            }
        }

        if (lines.isEmpty()) {
            warnings.add("No line items detected — check photo quality or lighting")
        }

        return ParsedWholesaleBill(
            supplierName = supplierName,
            invoiceNumber = invoiceNumber,
            invoiceDate = invoiceDate,
            lines = lines,
            netTotal = netTotal,
            warnings = warnings,
        )
    }

    fun matchSupplier(name: String?, suppliers: List<Supplier>): Supplier? {
        if (name.isNullOrBlank() || suppliers.isEmpty()) return null
        val target = normalizeName(name)
        return suppliers
            .map { supplier -> supplier to nameSimilarity(target, normalizeName(supplier.name)) }
            .filter { it.second >= 0.55 }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun normalizeLines(ocrText: String): List<String> =
        ocrText
            .replace('\u00A0', ' ')
            .replace(Regex("[ \t]+"), " ")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

    internal fun parsePaidLine(line: String): ParsedPurchaseBillLine? {
        val serial = serialStartPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (totalPattern.containsMatchIn(line)) return null

        val cleaned = line.replace(",", "")
        val decimals = Regex("""(\d+\.\d{2})""").findAll(cleaned).map { it.value.toDouble() }.toList()
        if (decimals.size < 3) return null

        val mrp = decimals.last()
        val amount = decimals[decimals.lastIndex - 1]
        val rate = decimals[decimals.lastIndex - 2]

        val beforeRate = cleaned.substringBefore(String.format(Locale.US, "%.2f", rate)).trimEnd()
        val qty = Regex("""(\d+)\s*$""").find(beforeRate)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (qty <= 0 || rate <= 0) return null

        val expiry = expiryPattern.find(line)?.value ?: return null
        val batch = findBatch(line) ?: return null
        val pack = packPattern.find(line)?.groupValues?.get(1)?.replace(" ", "")
        val hsCode = hsCodePattern.find(line)?.groupValues?.get(1)

        val description = extractDescription(line, serial, hsCode, pack, batch, expiry)

        if (description.length < 2) return null

        return ParsedPurchaseBillLine(
            serialNumber = serial,
            hsCode = hsCode,
            description = description,
            pack = pack,
            batchNumber = batch,
            expiryRaw = expiry,
            quantity = qty,
            unitPrice = rate,
            amount = amount,
            mrp = mrp,
        )
    }

    internal fun parseFreeLine(line: String, parent: ParsedPurchaseBillLine?): ParsedPurchaseBillLine? {
        val parentLine = parent ?: return null
        val qty = freeQtyPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val batch = findBatch(line) ?: parentLine.batchNumber
        val expiry = expiryPattern.find(line)?.value ?: parentLine.expiryRaw
        return ParsedPurchaseBillLine(
            serialNumber = null,
            description = parentLine.description,
            batchNumber = batch,
            expiryRaw = expiry,
            quantity = qty,
            unitPrice = 0.0,
            amount = 0.0,
            mrp = parentLine.mrp,
            isFreeItem = true,
        )
    }

    internal fun parseExpiryToIso(raw: String): String? {
        val match = expiryPattern.find(raw.trim()) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12) ?: return null
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun extractSupplierName(lines: List<String>): String? {
        val header = lines.take(12)
        val invoiceIdx = header.indexOfFirst { it.contains("INVOICE", ignoreCase = true) }
        val searchArea = if (invoiceIdx > 0) header.take(invoiceIdx) else header.take(4)
        return searchArea.firstOrNull { line ->
            line.length in 6..60 &&
                (
                    line.contains("MEDICO", ignoreCase = true) ||
                        line.contains("PHARMA", ignoreCase = true) ||
                        line.contains("DISTRIBUT", ignoreCase = true) ||
                        line.contains("PVT", ignoreCase = true) ||
                        line.contains("LTD", ignoreCase = true)
                    ) &&
                !line.contains("M/S", ignoreCase = true)
        }?.trim()
    }

    private fun extractInvoiceDate(text: String): String? {
        adDatePattern.findAll(text).mapNotNull { match ->
            formatAdDate(match.groupValues[1], match.groupValues[2], match.groupValues[3])
        }.firstOrNull()?.let { return it }

        bsDatePattern.find(text)?.let { match ->
            formatAdDate(match.groupValues[1], match.groupValues[2], match.groupValues[3])?.let { return it }
        }
        return null
    }

    private fun formatAdDate(yearStr: String, monthStr: String, dayStr: String): String? = runCatching {
        val year = yearStr.toInt()
        val month = monthStr.toInt().coerceIn(1, 12)
        val day = dayStr.toInt().coerceIn(1, 28)
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }.getOrNull()

    private fun extractNetTotal(text: String): Double? {
        val netMatch = Regex("""(?i)net\s+total\s*[:.]?\s*([\d,]+\.?\d*)""").find(text)
        if (netMatch != null) return netMatch.groupValues[1].replace(",", "").toDoubleOrNull()
        return Regex("""(?i)total\s*[:.]?\s*([\d,]+\.?\d*)""")
            .findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .lastOrNull()
    }

    private fun findFirstItemLine(lines: List<String>): Int =
        lines.indexOfFirst { line ->
            serialStartPattern.containsMatchIn(line) &&
                expiryPattern.containsMatchIn(line) &&
                batchPattern.containsMatchIn(line)
        }

    private fun findBatch(line: String): String? {
        val candidates = batchPattern.findAll(line).map { it.groupValues[1] }.toList()
        return candidates.firstOrNull { token ->
            !expiryPattern.matches(token) && !packPattern.matches(token)
        }
    }

    private fun extractDescription(
        line: String,
        serial: Int,
        hsCode: String?,
        pack: String?,
        batch: String,
        expiry: String,
    ): String {
        val batchIdx = line.indexOf(batch)
        var head = if (batchIdx > 0) line.substring(0, batchIdx) else line
        head = head.replaceFirst(Regex("""^\s*$serial\b\s*"""), "")
        if (hsCode != null) head = head.replace(hsCode, " ")
        if (pack != null) head = head.replace(pack, " ")
        head = head.replace(expiry, " ")
        return head.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalizeName(value: String): String =
        value.uppercase(Locale.US)
            .replace(Regex("""[^A-Z0-9 ]"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun nameSimilarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.9
        val aTokens = a.split(' ').filter { it.length > 2 }.toSet()
        val bTokens = b.split(' ').filter { it.length > 2 }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
        val overlap = aTokens.intersect(bTokens).size
        return overlap.toDouble() / max(aTokens.size, bTokens.size)
    }
}
