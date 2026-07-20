package com.medipro.manager.domain.model

/** Generated invoice PDF — single master output for print, share, and WhatsApp. */
data class InvoiceDocument(
    val invoiceNumber: String,
    val filePath: String,
    val savedToDocuments: Boolean,
)
