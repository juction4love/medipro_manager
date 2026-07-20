package com.medipro.manager.data.purchasebill

import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.domain.model.ParsedPurchaseBillLine
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.PurchaseBillLineMatch
import com.medipro.manager.domain.model.PurchaseBillMatchStatus
import com.medipro.manager.domain.repository.OcrMedicineAliasRepository
import com.medipro.manager.domain.repository.PosSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseBillMatcher @Inject constructor(
    private val posSearchRepository: PosSearchRepository,
    private val aliasRepository: OcrMedicineAliasRepository,
    private val medicineDao: MedicineDao,
    private val lineEnricher: PurchaseBillLineEnricher,
) {

    suspend fun matchLines(
        lines: List<ParsedPurchaseBillLine>,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> },
    ): List<PurchaseBillLineMatch> {
        val total = lines.size
        return lines.mapIndexed { index, line ->
            onProgress(index + 1, total)
            enrich(matchLine(line))
        }
    }

    suspend fun matchLine(line: ParsedPurchaseBillLine): PurchaseBillLineMatch {
        if (line.isFreeItem) {
            return PurchaseBillLineMatch(
                parsed = line,
                match = null,
                status = PurchaseBillMatchStatus.FREE_ITEM,
                confidence = 100,
            )
        }

        val aliasMedicineId = aliasRepository.findMedicineId(line.description)
        if (aliasMedicineId != null) {
            aliasRepository.recordHit(line.description)
            val medicine = medicineDao.getById(aliasMedicineId)
            if (medicine != null) {
                return PurchaseBillLineMatch(
                    parsed = line,
                    match = lineEnricher.medicineToSearchResult(medicine),
                    status = PurchaseBillMatchStatus.MATCHED,
                    confidence = 99,
                    matchedViaAlias = true,
                )
            }
        }

        val queries = buildSearchQueries(line.description)
        var best: PosSearchResult? = null
        var bestScore = 0

        for (query in queries) {
            val response = posSearchRepository.search(query)
            val candidate = response.results.firstOrNull() ?: continue
            val adjustedScore = adjustScore(candidate.matchScore, line.description, candidate.brandName)
            if (adjustedScore > bestScore) {
                bestScore = adjustedScore
                best = candidate.copy(matchScore = adjustedScore)
            }
            if (bestScore >= 95) break
        }

        val status = when {
            best == null -> PurchaseBillMatchStatus.UNMATCHED
            bestScore >= 82 -> PurchaseBillMatchStatus.MATCHED
            bestScore >= 55 -> PurchaseBillMatchStatus.NEEDS_REVIEW
            else -> PurchaseBillMatchStatus.UNMATCHED
        }

        return PurchaseBillLineMatch(
            parsed = line,
            match = best,
            status = status,
            confidence = bestScore,
        )
    }

    suspend fun enrich(line: PurchaseBillLineMatch): PurchaseBillLineMatch = lineEnricher.enrich(line)

    private fun buildSearchQueries(description: String): List<String> {
        val cleaned = description
            .replace(Regex("""(?i)\b(TAB|TABS|CAP|CAPS|SYR|INJ|SUSP)\b"""), " ")
            .replace(Regex("""[-–—/]+"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val tokens = cleaned.split(' ').filter { it.length > 1 }
        return buildList {
            add(cleaned)
            if (tokens.size > 2) add(tokens.take(3).joinToString(" "))
            if (tokens.isNotEmpty()) add(tokens.first())
        }.distinct().filter { it.length >= 2 }
    }

    private fun adjustScore(baseScore: Int, rawDescription: String, brandName: String): Int {
        val desc = rawDescription.uppercase()
        val brand = brandName.uppercase()
        var score = baseScore
        if (brand.isNotBlank() && desc.contains(brand.take(minOf(brand.length, 6)))) score += 8
        val descTokens = desc.split(Regex("\\W+")).filter { it.length > 2 }
        val brandTokens = brand.split(Regex("\\W+")).filter { it.length > 2 }
        val overlap = descTokens.intersect(brandTokens.toSet()).size
        score += overlap * 5
        return score.coerceIn(0, 100)
    }
}
