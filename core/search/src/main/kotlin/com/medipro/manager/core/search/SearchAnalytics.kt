package com.medipro.manager.core.search

data class SearchAnalyticsSnapshot(
    val topQueries: List<Pair<String, Long>>,
    val failedQueries: List<Pair<String, Long>>,
    val typoQueries: List<Pair<String, Long>>,
    val barcodeScans: Long,
    val slowSearchCount: Long,
    val averageSearchMs: Double,
)

class SearchAnalytics {
    private val queryCounts = linkedMapOf<String, Long>()
    private val failedCounts = linkedMapOf<String, Long>()
    private val typoCounts = linkedMapOf<String, Long>()
    private var barcodeScans: Long = 0
    private val searchDurationsMs = mutableListOf<Long>()

    @Synchronized
    fun recordSearch(
        query: String,
        resultCount: Int,
        didYouMean: String? = null,
        matchKind: SearchMatchKind? = null,
        durationMs: Long? = null,
    ) {
        val key = query.lowercase()
        queryCounts[key] = (queryCounts[key] ?: 0) + 1
        if (resultCount == 0) {
            failedCounts[key] = (failedCounts[key] ?: 0) + 1
        }
        if (matchKind == SearchMatchKind.TYPO || didYouMean != null) {
            typoCounts[key] = (typoCounts[key] ?: 0) + 1
        }
        durationMs?.let { searchDurationsMs.add(it) }
    }

    @Synchronized
    fun recordBarcodeScan() {
        barcodeScans++
    }

    @Synchronized
    fun snapshot(limit: Int = 20): SearchAnalyticsSnapshot = SearchAnalyticsSnapshot(
        topQueries = topEntries(queryCounts, limit),
        failedQueries = topEntries(failedCounts, limit),
        typoQueries = topEntries(typoCounts, limit),
        barcodeScans = barcodeScans,
        slowSearchCount = searchDurationsMs.count { it > 50 }.toLong(),
        averageSearchMs = if (searchDurationsMs.isEmpty()) 0.0 else searchDurationsMs.average(),
    )

    private fun topEntries(map: Map<String, Long>, limit: Int): List<Pair<String, Long>> =
        map.entries.map { it.key to it.value }.sortedByDescending { it.second }.take(limit)
}
