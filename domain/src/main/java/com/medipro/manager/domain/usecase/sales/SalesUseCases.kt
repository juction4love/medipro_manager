package com.medipro.manager.domain.usecase.sales

import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.StockBatch
import com.medipro.manager.domain.repository.MedicineRepository
import com.medipro.manager.domain.repository.PosSearchRepository
import com.medipro.manager.domain.repository.SaleRepository
import javax.inject.Inject

class SearchPosMedicinesUseCase @Inject constructor(
    private val posSearchRepository: PosSearchRepository,
) {
    suspend operator fun invoke(query: String) = posSearchRepository.search(query)
}

class FindPosAlternativesUseCase @Inject constructor(
    private val posSearchRepository: PosSearchRepository,
) {
    suspend operator fun invoke(medicineId: Long) = posSearchRepository.findAlternatives(medicineId)
}

class LookupPosBarcodeUseCase @Inject constructor(
    private val posSearchRepository: PosSearchRepository,
) {
    suspend operator fun invoke(barcode: String) = posSearchRepository.lookupBarcode(barcode)
}

class SearchMedicinesForSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(query: String): List<Medicine> =
        saleRepository.searchMedicinesForSale(query)
}

class GetMedicineByBarcodeForSaleUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    suspend operator fun invoke(barcode: String): Medicine? =
        medicineRepository.getMedicineByBarcode(barcode)
}

class GetAvailableBatchesUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(medicineId: Long): List<StockBatch> =
        saleRepository.getAvailableBatches(medicineId)
}

class GenerateInvoiceNumberUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(): String = saleRepository.generateInvoiceNumber()
}

class CreateSaleUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(sale: Sale): Long = saleRepository.createSale(sale)
}

class ObserveSalesUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    operator fun invoke() = saleRepository.observeSales()
}

class GetSaleByIdUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(id: Long): Sale? = saleRepository.getSaleById(id)
}

class GetSaleByInvoiceNumberUseCase @Inject constructor(
    private val saleRepository: SaleRepository,
) {
    suspend operator fun invoke(invoiceNumber: String): Sale? =
        saleRepository.getSaleByInvoiceNumber(invoiceNumber.trim())
}
