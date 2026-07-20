package com.medipro.manager.core.search

/**
 * Lightweight phonetic key for brand/generic matching across transliteration and spelling variants.
 * Handles common Nepal pharmacy inputs (e.g. parasetamol/paracetamol, citrizine/cetirizine).
 */
object PhoneticEncoder {

    private val MULTI_CHAR_REPLACEMENTS = listOf(
        "ph" to "f",
        "gh" to "g",
        "kh" to "k",
        "dh" to "d",
        "bh" to "b",
        "th" to "t",
        "sh" to "s",
        "ch" to "c",
        "ck" to "k",
        "sch" to "s",
        "tion" to "n",
        "sion" to "n",
    )

    private val SIMILAR_CONSONANTS = mapOf(
        'c' to 'k',
        'q' to 'k',
        'x' to 'k',
        'z' to 's',
        'v' to 'f',
        'w' to 'v',
        'j' to 'g',
        'y' to 'i',
    )

    fun encode(text: String): String {
        if (text.isBlank()) return ""

        var normalized = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
        if (normalized.isBlank()) return ""

        MULTI_CHAR_REPLACEMENTS.forEach { (from, to) ->
            normalized = normalized.replace(from, to)
        }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return tokens.joinToString(" ") { encodeToken(it) }.trim()
    }

    fun matches(query: String, candidate: String): Boolean {
        val queryKey = encode(query)
        if (queryKey.isBlank()) return false
        val candidateKey = encode(candidate)
        if (candidateKey.isBlank()) return false
        if (candidateKey == queryKey) return true

        val queryTokens = queryKey.split(' ').filter { it.isNotBlank() }
        val candidateTokens = candidateKey.split(' ').filter { it.isNotBlank() }
        return queryTokens.all { qToken ->
            candidateTokens.any { cToken ->
                cToken.startsWith(qToken) || qToken.startsWith(cToken) ||
                    Levenshtein.distance(qToken, cToken) <= Levenshtein.maxDistanceForLength(qToken.length)
            }
        }
    }

    private fun encodeToken(token: String): String {
        if (token.isEmpty()) return ""

        val builder = StringBuilder()
        builder.append(token.first())

        var previous = normalizeChar(token.first())
        for (index in 1 until token.length) {
            val ch = normalizeChar(token[index])
            if (ch in VOWELS) continue
            if (ch == previous) continue
            builder.append(ch)
            previous = ch
        }
        return builder.toString()
    }

    private fun normalizeChar(ch: Char): Char {
        val lower = ch.lowercaseChar()
        return SIMILAR_CONSONANTS[lower] ?: lower
    }

    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
}
