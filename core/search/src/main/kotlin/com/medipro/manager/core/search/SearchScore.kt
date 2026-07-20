package com.medipro.manager.core.search

object SearchScore {
    const val EXACT_BRAND = 100
    const val EXACT_GENERIC = 90
    const val FTS_PREFIX = 85
    const val STARTS_WITH = 80
    const val CONTAINS = 70
    const val SYNONYM = 60
    const val PHONETIC = 50
    const val TYPO = 40
    const val BARCODE = 100

    const val ENOUGH_RESULTS = 30
    const val TYPO_BUCKET_LIMIT = 150
    const val CACHE_MAX_ENTRIES = 200

    fun forKind(kind: SearchMatchKind): Int = when (kind) {
        SearchMatchKind.BARCODE -> BARCODE
        SearchMatchKind.EXACT_BRAND -> EXACT_BRAND
        SearchMatchKind.EXACT_GENERIC -> EXACT_GENERIC
        SearchMatchKind.FTS_PREFIX -> FTS_PREFIX
        SearchMatchKind.STARTS_WITH -> STARTS_WITH
        SearchMatchKind.CONTAINS -> CONTAINS
        SearchMatchKind.SYNONYM -> SYNONYM
        SearchMatchKind.PHONETIC -> PHONETIC
        SearchMatchKind.TYPO -> TYPO
        SearchMatchKind.FTS -> FTS_PREFIX
    }
}
