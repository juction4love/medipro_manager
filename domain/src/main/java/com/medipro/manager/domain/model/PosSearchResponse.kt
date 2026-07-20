package com.medipro.manager.domain.model

data class PosSearchResponse(
    val results: List<PosSearchResult>,
    val didYouMean: String? = null,
)
