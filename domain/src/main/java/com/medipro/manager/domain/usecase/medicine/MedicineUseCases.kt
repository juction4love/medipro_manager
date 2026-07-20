package com.medipro.manager.domain.usecase.medicine

import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMedicinesUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    operator fun invoke(): Flow<List<Medicine>> = repository.observeMedicines()
}

class SearchMedicinesUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    operator fun invoke(query: String): Flow<List<Medicine>> = repository.searchMedicines(query)
}

class AddMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(medicine: Medicine): Long = repository.addMedicine(medicine)
}

class UpdateMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(medicine: Medicine) = repository.updateMedicine(medicine)
}

class DeleteMedicineUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteMedicine(id)
}

class GetMedicineByBarcodeUseCase @Inject constructor(
    private val repository: MedicineRepository
) {
    suspend operator fun invoke(barcode: String): Medicine? = repository.getMedicineByBarcode(barcode)
}
