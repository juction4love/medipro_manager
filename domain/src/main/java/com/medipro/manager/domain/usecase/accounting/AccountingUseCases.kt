package com.medipro.manager.domain.usecase.accounting

import com.medipro.manager.domain.repository.AccountingRepository
import javax.inject.Inject

class ObserveCustomersWithDueUseCase @Inject constructor(
    private val repository: AccountingRepository,
) {
    operator fun invoke() = repository.observeCustomersWithDue()
}

class ObserveSuppliersWithDueUseCase @Inject constructor(
    private val repository: AccountingRepository,
) {
    operator fun invoke() = repository.observeSuppliersWithDue()
}

class RecordCustomerReceiptUseCase @Inject constructor(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String? = null,
    ) = repository.recordCustomerReceipt(customerId, amount, paymentMethod, notes)
}

class RecordSupplierPaymentUseCase @Inject constructor(
    private val repository: AccountingRepository,
) {
    suspend operator fun invoke(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String? = null,
    ) = repository.recordSupplierPayment(supplierId, amount, paymentMethod, notes)
}
