package com.medipro.manager.domain.model

enum class GlobalSearchCategory {
    MEDICINE,
    CUSTOMER,
    SUPPLIER,
    SALE_INVOICE,
    PURCHASE_INVOICE,
}

data class GlobalSearchResult(
    val id: Long,
    val category: GlobalSearchCategory,
    val title: String,
    val subtitle: String,
)

data class GlobalSearchResponse(
    val medicines: List<GlobalSearchResult> = emptyList(),
    val customers: List<GlobalSearchResult> = emptyList(),
    val suppliers: List<GlobalSearchResult> = emptyList(),
    val sales: List<GlobalSearchResult> = emptyList(),
    val purchases: List<GlobalSearchResult> = emptyList(),
) {
    val isEmpty: Boolean =
        medicines.isEmpty() &&
            customers.isEmpty() &&
            suppliers.isEmpty() &&
            sales.isEmpty() &&
            purchases.isEmpty()

    val totalCount: Int =
        medicines.size + customers.size + suppliers.size + sales.size + purchases.size
}
