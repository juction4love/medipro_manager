package com.medipro.manager.data.repository

import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.database.dao.CustomerDao
import com.medipro.manager.core.database.dao.PurchaseDao
import com.medipro.manager.core.database.dao.SaleDao
import com.medipro.manager.core.database.dao.SupplierDao
import com.medipro.manager.data.search.InventorySearchEngine
import com.medipro.manager.domain.model.GlobalSearchCategory
import com.medipro.manager.domain.model.GlobalSearchResponse
import com.medipro.manager.domain.model.GlobalSearchResult
import com.medipro.manager.domain.repository.GlobalSearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class GlobalSearchRepositoryImpl @Inject constructor(
    private val inventorySearchEngine: InventorySearchEngine,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
) : GlobalSearchRepository {

    override suspend fun search(query: String): GlobalSearchResponse {
        val trimmed = query.trim()
        if (trimmed.length < 2) return GlobalSearchResponse()

        return coroutineScope {
            val medicinesDeferred = async {
                inventorySearchEngine.search(trimmed, limit = 15).map { scored ->
                    val medicine = scored.entity
                    GlobalSearchResult(
                        id = medicine.id,
                        category = GlobalSearchCategory.MEDICINE,
                        title = medicine.brandName,
                        subtitle = buildString {
                            append(medicine.genericName)
                            if (medicine.strength.isNotBlank()) append(" · ${medicine.strength}")
                            medicine.barcode?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        },
                    )
                }
            }
            val customersDeferred = async {
                customerDao.search(trimmed).map { customer ->
                    GlobalSearchResult(
                        id = customer.id,
                        category = GlobalSearchCategory.CUSTOMER,
                        title = customer.name,
                        subtitle = listOfNotNull(
                            customer.phone?.takeIf { it.isNotBlank() },
                            customer.email?.takeIf { it.isNotBlank() },
                            if (customer.outstandingBalance > 0) {
                                "Due ${FormatUtils.formatCurrency(customer.outstandingBalance)}"
                            } else {
                                null
                            },
                        ).joinToString(" · ").ifBlank { "Customer" },
                    )
                }
            }
            val suppliersDeferred = async {
                supplierDao.search(trimmed).map { supplier ->
                    GlobalSearchResult(
                        id = supplier.id,
                        category = GlobalSearchCategory.SUPPLIER,
                        title = supplier.name,
                        subtitle = listOfNotNull(
                            supplier.phone?.takeIf { it.isNotBlank() },
                            supplier.contactPerson?.takeIf { it.isNotBlank() },
                            if (supplier.outstandingBalance > 0) {
                                "Due ${FormatUtils.formatCurrency(supplier.outstandingBalance)}"
                            } else {
                                null
                            },
                        ).joinToString(" · ").ifBlank { "Supplier" },
                    )
                }
            }
            val salesDeferred = async {
                saleDao.searchByInvoice(trimmed).map { sale ->
                    GlobalSearchResult(
                        id = sale.id,
                        category = GlobalSearchCategory.SALE_INVOICE,
                        title = sale.invoiceNumber,
                        subtitle = buildString {
                            append(FormatUtils.formatDate(sale.saleDate))
                            append(" · ${FormatUtils.formatCurrency(sale.totalAmount)}")
                            sale.patientName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                        },
                    )
                }
            }
            val purchasesDeferred = async {
                purchaseDao.searchByInvoice(trimmed).map { purchase ->
                    GlobalSearchResult(
                        id = purchase.id,
                        category = GlobalSearchCategory.PURCHASE_INVOICE,
                        title = purchase.invoiceNumber,
                        subtitle = buildString {
                            append(FormatUtils.formatDate(purchase.purchaseDate))
                            append(" · ${FormatUtils.formatCurrency(purchase.totalAmount)}")
                        },
                    )
                }
            }

            GlobalSearchResponse(
                medicines = medicinesDeferred.await(),
                customers = customersDeferred.await(),
                suppliers = suppliersDeferred.await(),
                sales = salesDeferred.await(),
                purchases = purchasesDeferred.await(),
            )
        }
    }
}
