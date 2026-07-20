package com.medipro.manager.domain.usecase.customer

import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCustomersUseCase @Inject constructor(
    private val repository: CustomerRepository
) {
    operator fun invoke(): Flow<List<Customer>> = repository.observeCustomers()
}
