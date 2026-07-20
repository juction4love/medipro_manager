package com.medipro.manager.core.search

/**
 * Normalizes POS search input — collapses strength variants (650mg, 650 mg)
 * and prepares tokens for synonym / composition matching.
 */
object SearchNormalizer {

    private val STRENGTH_PATTERN = Regex("""(\d+(?:\.\d+)?)\s*(mg|mcg|g|ml|iu|%)?""", RegexOption.IGNORE_CASE)
    private val NON_ALNUM = Regex("[^a-z0-9\\s.]")

    fun normalize(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    fun normalizeToken(token: String): String {
        val lower = token.lowercase()
        STRENGTH_PATTERN.find(lower)?.let { match ->
            val value = match.groupValues[1]
            val unit = match.groupValues[2].ifBlank { "mg" }
            return "$value$unit"
        }
        return lower.replace(NON_ALNUM, "")
    }

    fun tokenize(raw: String): List<String> =
        normalize(raw)
            .split(' ')
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }

    fun isBarcodeQuery(query: String): Boolean {
        val trimmed = query.trim()
        return trimmed.length >= 4 && trimmed.all { it.isDigit() }
    }

    fun typoBucketPrefix(query: String): String {
        val first = tokenize(query).firstOrNull() ?: return query.take(3).lowercase()
        return first.take(3).lowercase().ifBlank { query.take(3).lowercase() }
    }

    /** Expands shorthand generic queries (pcm, 650) into searchable canonical terms. */
    fun expandGenericTokens(tokens: List<String>): List<String> {
        val expanded = linkedSetOf<String>()
        tokens.forEach { token ->
            expanded.add(token)
            SynonymDictionary.expandTerm(token).forEach { expanded.add(it) }
        }
        return expanded.toList()
    }
}
