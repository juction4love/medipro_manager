package com.medipro.manager.domain.usecase.purchase

import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LookupMedicineBarcodeForPurchaseUseCase @Inject constructor(
    private val medicineRepository: com.medipro.manager.domain.repository.MedicineRepository,
) {
    suspend operator fun invoke(barcode: String) = medicineRepository.getMedicineByBarcode(barcode)
}

class SearchMedicinesForPurchaseUseCase @Inject constructor(
    private val repository: PurchaseRepository
) {
    suspend operator fun invoke(query: String): List<Medicine> =
        repository.searchMedicinesForPurchase(query)
}

class GeneratePurchaseInvoiceNumberUseCase @Inject constructor(
    private val repository: PurchaseRepository
) {
    suspend operator fun invoke(): String = repository.generatePurchaseInvoiceNumber()
}

class CreatePurchaseUseCase @Inject constructor(
    private val repository: PurchaseRepository
) {
    suspend operator fun invoke(purchase: Purchase): Long = repository.createPurchase(purchase)
}

class GetPurchaseByIdUseCase @Inject constructor(
    private val repository: PurchaseRepository
) {
    suspend operator fun invoke(id: Long): Purchase? = repository.getPurchaseById(id)
}

class ObservePurchasesUseCase @Inject constructor(
    private val repository: PurchaseRepository
) {
    operator fun invoke(): Flow<List<Purchase>> = repository.observePurchases()
}
