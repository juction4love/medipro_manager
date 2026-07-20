package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.model.Supplier
import kotlinx.coroutines.flow.Flow

interface AccountingRepository {
    fun observeCustomersWithDue(): Flow<List<Customer>>
    fun observeSuppliersWithDue(): Flow<List<Supplier>>

    suspend fun recordCustomerReceipt(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String? = null,
    ): String

    suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String? = null,
    ): String
}
