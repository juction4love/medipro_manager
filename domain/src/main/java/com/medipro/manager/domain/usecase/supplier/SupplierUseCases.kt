package com.medipro.manager.domain.usecase.supplier

import com.medipro.manager.domain.model.Supplier
import com.medipro.manager.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSuppliersUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<List<Supplier>> = repository.observeSuppliers()
}

class AddSupplierUseCase @Inject constructor(
    private val repository: SupplierRepository,
) {
    suspend operator fun invoke(supplier: Supplier): Long = repository.addSupplier(supplier)
}
