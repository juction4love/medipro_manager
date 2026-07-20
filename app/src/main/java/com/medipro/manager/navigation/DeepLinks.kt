package com.medipro.manager.navigation

/**
 * Custom URI scheme for MediPro deep links.
 *
 * Examples:
 * - medipro://invoice/{saleUuid}
 * - medipro://invoice/INV-20260719-000001 (legacy)
 */
object DeepLinks {
    const val SCHEME = "medipro"

    const val HOST_INVOICE = "invoice"
    const val HOST_PURCHASE = "purchase"
    const val HOST_MEDICINE = "medicine"
    const val HOST_CUSTOMER = "customer"
    const val HOST_DASHBOARD = "dashboard"
    const val HOST_SALES = "sales"
    const val HOST_INVENTORY = "inventory"
    const val HOST_REPORTS = "reports"

    const val INVOICE_PATTERN = "$SCHEME://$HOST_INVOICE/{invoiceNumber}"
    const val PURCHASE_INVOICE_PATTERN = "$SCHEME://$HOST_PURCHASE/{invoiceNumber}"
    const val MEDICINE_PATTERN = "$SCHEME://$HOST_MEDICINE/{medicineId}"
    const val CUSTOMER_PATTERN = "$SCHEME://$HOST_CUSTOMER/{customerId}"
    const val DASHBOARD_URI = "$SCHEME://$HOST_DASHBOARD"
    const val SALES_URI = "$SCHEME://$HOST_SALES"
    const val INVENTORY_URI = "$SCHEME://$HOST_INVENTORY"
    const val REPORTS_URI = "$SCHEME://$HOST_REPORTS"

    fun saleInvoice(invoiceRef: String): String =
        "$SCHEME://$HOST_INVOICE/${UriEncoder.encode(invoiceRef)}"

    fun saleInvoiceByUuid(uuid: String): String = saleInvoice(uuid)

    fun purchaseInvoice(invoiceNumber: String): String =
        "$SCHEME://$HOST_PURCHASE/${UriEncoder.encode(invoiceNumber)}"
}

/** Percent-encode path segments for shareable deep links. */
private object UriEncoder {
    fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
        .replace("+", "%20")
}
