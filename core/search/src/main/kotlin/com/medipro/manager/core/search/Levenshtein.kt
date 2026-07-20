package com.medipro.manager.core.search

object Levenshtein {

    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(
                    curr[j] + 1,
                    prev[j + 1] + 1,
                    prev[j] + cost,
                )
            }
            prev.indices.forEach { idx -> prev[idx] = curr[idx] }
        }
        return prev[b.length]
    }

    fun maxDistanceForLength(length: Int): Int = when {
        length <= 3 -> 1
        length <= 6 -> 2
        else -> 3
    }

    fun isTypoMatch(query: String, candidate: String): Boolean {
        val q = query.lowercase()
        val c = candidate.lowercase()
        if (q == c || c.contains(q) || q.contains(c)) return true
        val threshold = maxDistanceForLength(q.length)
        return distance(q, c) <= threshold
    }
}
