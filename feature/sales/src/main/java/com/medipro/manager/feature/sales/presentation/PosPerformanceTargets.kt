package com.medipro.manager.feature.sales.presentation

/** POS performance targets for 271K catalog + local inventory. */
object PosPerformanceTargets {
    const val SEARCH_MS = 50L
    const val BARCODE_LOOKUP_MS = 200L
    const val ADD_TO_CART_MS = 30L
    const val CHECKOUT_OPEN_MS = 100L
    const val PDF_GENERATION_MS = 1_000L
}
