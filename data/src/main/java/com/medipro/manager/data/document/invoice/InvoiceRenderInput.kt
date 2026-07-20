package com.medipro.manager.data.document.invoice

import com.medipro.manager.domain.model.PharmacySettings
import com.medipro.manager.domain.model.Sale

data class InvoiceRenderInput(
    val sale: Sale,
    val settings: PharmacySettings,
    val pharmacistName: String? = null,
    val watermark: String? = null,
    val generatedAt: Long = System.currentTimeMillis(),
)
