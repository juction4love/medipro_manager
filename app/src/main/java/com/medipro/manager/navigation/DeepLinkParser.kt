package com.medipro.manager.navigation

import android.net.Uri

sealed interface DeepLinkTarget {
    /** UUID or legacy invoice number */
    data class SaleInvoice(val invoiceRef: String) : DeepLinkTarget
    data class PurchaseInvoice(val invoiceNumber: String) : DeepLinkTarget
    data class Medicine(val medicineId: String) : DeepLinkTarget
    data class Customer(val customerId: Long) : DeepLinkTarget
    data class Tab(val route: String) : DeepLinkTarget
}

fun Uri.toDeepLinkTarget(): DeepLinkTarget? {
    if (scheme != DeepLinks.SCHEME) return null
    val host = host ?: return null
    val firstSegment = pathSegments.firstOrNull().orEmpty()
    return when (host) {
        DeepLinks.HOST_INVOICE -> firstSegment.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.SaleInvoice(decode(it)) }
        DeepLinks.HOST_PURCHASE -> firstSegment.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.PurchaseInvoice(decode(it)) }
        DeepLinks.HOST_MEDICINE -> firstSegment.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Medicine(decode(it)) }
        DeepLinks.HOST_CUSTOMER -> firstSegment.toLongOrNull()?.let { DeepLinkTarget.Customer(it) }
        DeepLinks.HOST_DASHBOARD -> DeepLinkTarget.Tab(Routes.DASHBOARD)
        DeepLinks.HOST_SALES -> DeepLinkTarget.Tab(Routes.SALES)
        DeepLinks.HOST_INVENTORY -> DeepLinkTarget.Tab(Routes.INVENTORY)
        DeepLinks.HOST_REPORTS -> DeepLinkTarget.Tab(Routes.REPORTS)
        else -> null
    }
}

private fun decode(value: String): String =
    java.net.URLDecoder.decode(value, Charsets.UTF_8.name())

object DeepLinkHolder {
    var pending: Uri? = null
        private set

    fun store(uri: Uri?) {
        pending = uri?.takeIf { it.scheme == DeepLinks.SCHEME }
    }

    fun consume(): Uri? = pending.also { pending = null }
}
