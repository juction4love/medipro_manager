package com.medipro.manager.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEnhancementTest {

    @Test
    fun phonetic_matches_common_misspellings() {
        assertTrue(PhoneticEncoder.matches("parasetamol", "paracetamol"))
        assertTrue(PhoneticEncoder.matches("citrizine", "cetirizine"))
    }

    @Test
    fun synonym_expands_paracetamol_aliases() {
        val expanded = SynonymDictionary.expandQuery("pcm")
        assertTrue(expanded.any { it.contains("paracetamol") || it.contains("pcm") })
    }

    @Test
    fun typo_correction_allows_small_edit_distance() {
        assertTrue(SearchQueryEnhancer.typoMatches("paracetmol", "paracetamol"))
        assertTrue(SearchQueryEnhancer.typoMatches("ibuprfen", "ibuprofen"))
    }

    @Test
    fun fts_query_builder_includes_prefix_wildcard() {
        val query = SearchQueryEnhancer.buildFtsQuery("para 500")
        assertTrue(query?.contains("para*") == true)
        assertTrue(query?.contains("500*") == true)
    }

    @Test
    fun barcode_query_detects_digits_only() {
        assertTrue(SearchQueryEnhancer.isBarcodeQuery("8901234567890"))
        assertFalse(SearchQueryEnhancer.isBarcodeQuery("para500"))
    }

    @Test
    fun enough_results_threshold_is_30() {
        assertFalse(SearchQueryEnhancer.hasEnoughResults(29))
        assertTrue(SearchQueryEnhancer.hasEnoughResults(30))
    }

    @Test
    fun ranking_scores_follow_production_order() {
        assertEquals(SearchScore.EXACT_BRAND, SearchScore.forKind(SearchMatchKind.EXACT_BRAND))
        assertEquals(SearchScore.TYPO, SearchScore.forKind(SearchMatchKind.TYPO))
        assertTrue(SearchScore.EXACT_BRAND > SearchScore.SYNONYM)
        assertTrue(SearchScore.SYNONYM > SearchScore.TYPO)
    }

    @Test
    fun normalizer_collapses_strength_tokens() {
        assertEquals("650mg", SearchNormalizer.normalizeToken("650 mg"))
    }

    @Test
    fun unrelated_terms_do_not_phonetically_match() {
        assertFalse(PhoneticEncoder.matches("metformin", "cetirizine"))
    }
}
