package com.medipro.manager.data.search

import com.medipro.manager.core.database.dao.MedicineDao
import com.medipro.manager.core.database.entity.MedicineEntity
import com.medipro.manager.core.search.PhoneticEncoder
import com.medipro.manager.core.search.SearchMatchKind
import com.medipro.manager.core.search.SearchNormalizer
import com.medipro.manager.core.search.SearchQueryEnhancer
import com.medipro.manager.core.search.SearchScore
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredMedicine(
    val entity: MedicineEntity,
    val score: Int,
    val kind: SearchMatchKind,
)

@Singleton
class InventorySearchEngine @Inject constructor(
    private val medicineDao: MedicineDao,
) {
    suspend fun search(query: String, limit: Int = 50): List<ScoredMedicine> {
        val normalized = SearchQueryEnhancer.normalize(query)
        if (normalized.length < 2) return emptyList()

        if (SearchQueryEnhancer.isBarcodeQuery(normalized)) {
            val barcodeHit = medicineDao.getByBarcode(normalized)
            return if (barcodeHit != null) {
                listOf(ScoredMedicine(barcodeHit, SearchScore.BARCODE, SearchMatchKind.BARCODE))
            } else {
                emptyList()
            }
        }

        val ranked = linkedMapOf<Long, ScoredMedicine>()

        fun add(items: List<MedicineEntity>, kind: SearchMatchKind, baseScore: Int) {
            items.forEachIndexed { index, entity ->
                val score = baseScore - index.coerceAtMost(10)
                val existing = ranked[entity.id]
                if (existing == null || score > existing.score) {
                    ranked[entity.id] = ScoredMedicine(entity, score, kind)
                }
            }
        }

        add(medicineDao.searchExactBrand(normalized), SearchMatchKind.EXACT_BRAND, SearchScore.EXACT_BRAND)
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        add(medicineDao.searchExactGeneric(normalized), SearchMatchKind.EXACT_GENERIC, SearchScore.EXACT_GENERIC)
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        SearchQueryEnhancer.buildPrimaryFtsQuery(normalized)?.let { ftsQuery ->
            add(medicineDao.searchFtsOnce(ftsQuery, limit), SearchMatchKind.FTS_PREFIX, SearchScore.FTS_PREFIX)
        }
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        add(medicineDao.searchStartsWith(normalized, limit), SearchMatchKind.STARTS_WITH, SearchScore.STARTS_WITH)
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        add(medicineDao.searchContains(normalized, limit), SearchMatchKind.CONTAINS, SearchScore.CONTAINS)
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        SearchNormalizer.expandGenericTokens(SearchNormalizer.tokenize(normalized)).forEach { token ->
            add(medicineDao.searchByCompositionToken(token, limit), SearchMatchKind.CONTAINS, SearchScore.CONTAINS)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)
        }

        SearchQueryEnhancer.buildSynonymFtsQueries(normalized).forEach { ftsQuery ->
            add(medicineDao.searchFtsOnce(ftsQuery, limit), SearchMatchKind.SYNONYM, SearchScore.SYNONYM)
            if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)
        }

        add(searchPhonetic(normalized, limit), SearchMatchKind.PHONETIC, SearchScore.PHONETIC)
        if (SearchQueryEnhancer.hasEnoughResults(ranked.size)) return finalize(ranked, limit)

        add(searchTypoBucket(normalized, limit), SearchMatchKind.TYPO, SearchScore.TYPO)
        return finalize(ranked, limit)
    }

    private suspend fun searchPhonetic(query: String, limit: Int): List<MedicineEntity> {
        val prefix = SearchNormalizer.typoBucketPrefix(query)
        if (prefix.length < 2) return emptyList()
        return medicineDao.searchByPrefix(prefix, limit = 120).filter { entity ->
            PhoneticEncoder.matches(query, entity.brandName) ||
                PhoneticEncoder.matches(query, entity.genericName)
        }.take(limit)
    }

    private suspend fun searchTypoBucket(query: String, limit: Int): List<MedicineEntity> {
        val prefix = SearchNormalizer.typoBucketPrefix(query)
        if (prefix.length < 2) return emptyList()
        val candidates = medicineDao.searchByPrefix(prefix, limit = SearchScore.TYPO_BUCKET_LIMIT)
        val rankedNames = SearchQueryEnhancer.rankTypoBucket(
            query,
            candidates.flatMap { listOf(it.brandName, it.genericName) },
        )
        return rankedNames.mapNotNull { (name, _) ->
            candidates.firstOrNull {
                it.brandName.equals(name, ignoreCase = true) ||
                    it.genericName.equals(name, ignoreCase = true)
            }
        }.distinctBy { it.id }.take(limit)
    }

    private fun finalize(ranked: LinkedHashMap<Long, ScoredMedicine>, limit: Int): List<ScoredMedicine> =
        ranked.values.sortedByDescending { it.score }.take(limit)
}
