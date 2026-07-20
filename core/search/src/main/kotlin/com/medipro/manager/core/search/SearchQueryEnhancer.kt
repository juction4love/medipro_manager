package com.medipro.manager.core.search

enum class SearchMatchKind {
    BARCODE,
    EXACT_BRAND,
    EXACT_GENERIC,
    FTS_PREFIX,
    STARTS_WITH,
    CONTAINS,
    SYNONYM,
    PHONETIC,
    TYPO,
    /** @deprecated Use [FTS_PREFIX] */
    FTS,
}

data class RankedSearchHit<T>(
    val item: T,
    val kind: SearchMatchKind,
    val score: Int,
)

object SearchQueryEnhancer {

    fun normalize(query: String): String = SearchNormalizer.normalize(query)

    fun isBarcodeQuery(query: String): Boolean = SearchNormalizer.isBarcodeQuery(query)

    fun buildFtsQuery(query: String): String? {
        val terms = normalize(query).split(' ').filter { it.isNotBlank() && !SearchNormalizer.isBarcodeQuery(it) }
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "${sanitizeFtsTerm(it)}*" }
    }

    /** Primary FTS only — synonym/phonetic/typo layers run separately when needed. */
    fun buildPrimaryFtsQuery(query: String): String? = buildFtsQuery(query)

    fun buildSynonymFtsQueries(query: String): List<String> {
        val normalized = normalize(query)
        return SynonymDictionary.expandQuery(normalized)
            .drop(1)
            .mapNotNull { buildFtsQuery(it) }
            .distinct()
    }

    fun buildSynonymOrQuery(query: String): String? {
        val tokens = normalize(query).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val orGroups = tokens.map { token ->
            val variants = SynonymDictionary.expandTerm(token)
                .map { sanitizeFtsTerm(it) }
                .distinct()
            variants.joinToString(" OR ") { "$it*" }
        }
        return orGroups.joinToString(" ")
    }

    fun phoneticKey(text: String): String = PhoneticEncoder.encode(text)

    fun phoneticMatches(query: String, candidate: String): Boolean =
        PhoneticEncoder.matches(query, candidate)

    fun typoMatches(query: String, candidate: String): Boolean =
        Levenshtein.isTypoMatch(query, candidate)

    fun scoreTextMatch(query: String, candidate: String): Int {
        val q = normalize(query).lowercase()
        val c = candidate.lowercase()
        return when {
            c == q -> SearchScore.EXACT_BRAND
            c.startsWith(q) -> SearchScore.STARTS_WITH
            c.contains(q) -> SearchScore.CONTAINS
            phoneticMatches(q, c) -> SearchScore.PHONETIC
            typoMatches(q, c) -> SearchScore.TYPO
            else -> 0
        }
    }

    fun rankTypoBucket(
        query: String,
        candidates: List<String>,
        limit: Int = SearchScore.TYPO_BUCKET_LIMIT,
    ): List<Pair<String, Int>> {
        val normalized = normalize(query).lowercase()
        return candidates
            .distinct()
            .mapNotNull { candidate ->
                val lower = candidate.lowercase()
                val score = when {
                    lower == normalized -> SearchScore.EXACT_BRAND
                    lower.startsWith(normalized) -> SearchScore.STARTS_WITH
                    typoMatches(normalized, lower) -> SearchScore.TYPO
                    else -> null
                }
                score?.let { candidate to it }
            }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun suggestDidYouMean(query: String, topBrand: String?, topKind: SearchMatchKind?): String? {
        if (topBrand.isNullOrBlank()) return null
        if (topKind != SearchMatchKind.TYPO && topKind != SearchMatchKind.PHONETIC) return null
        return topBrand.takeIf { !normalize(query).equals(it, ignoreCase = true) }
    }

    fun hasEnoughResults(count: Int): Boolean = count >= SearchScore.ENOUGH_RESULTS

    /** @deprecated Use [buildPrimaryFtsQuery] + layer-specific queries */
    fun buildFtsQueries(query: String): List<Pair<String, SearchMatchKind>> {
        val primary = buildPrimaryFtsQuery(query)
        return buildList {
            if (primary != null) add(primary to SearchMatchKind.FTS_PREFIX)
        }
    }

    fun rankCandidates(
        query: String,
        candidates: List<String>,
    ): List<Pair<String, Int>> {
        val normalized = normalize(query).lowercase()
        return candidates
            .map { candidate ->
                val lower = candidate.lowercase()
                val score = when {
                    lower == normalized -> 100
                    lower.startsWith(normalized) -> 90
                    lower.contains(normalized) -> 80
                    phoneticMatches(normalized, lower) -> 65
                    typoMatches(normalized, lower) -> 50
                    else -> 0
                }
                candidate to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }

    fun mergeRankedHits(
        hits: List<RankedSearchHit<*>>,
        keySelector: (Any?) -> String,
        limit: Int,
    ): List<RankedSearchHit<*>> {
        val bestByKey = linkedMapOf<String, RankedSearchHit<*>>()
        hits.forEach { hit ->
            val key = keySelector(hit.item)
            val existing = bestByKey[key]
            if (existing == null || hit.score > existing.score) {
                bestByKey[key] = hit
            }
        }
        return bestByKey.values.sortedByDescending { it.score }.take(limit)
    }

    private fun sanitizeFtsTerm(term: String): String =
        term.replace("\"", "").replace("*", "")
}
