package com.medipro.manager.domain.usecase.inventory

import com.medipro.manager.domain.model.InventoryMedicineStock
import com.medipro.manager.domain.model.InventorySummary
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.StockAdjustmentContext
import com.medipro.manager.domain.model.StockAdjustmentType
import com.medipro.manager.domain.model.ExpiryReportRow
import com.medipro.manager.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveInventorySummaryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke(): Flow<InventorySummary> = repository.observeSummary()
}

class SearchInventoryMedicinesUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(query: String): List<InventoryMedicineStock> =
        repository.searchMedicinesWithStock(query)
}

class GetMedicineStockUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(medicineId: Long): InventoryMedicineStock? =
        repository.getMedicineStock(medicineId)
}

class GetStockAdjustmentContextUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(batchId: Long): StockAdjustmentContext? =
        repository.getAdjustmentContext(batchId)
}

class CreateStockAdjustmentUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(
        batchId: Long,
        type: StockAdjustmentType,
        quantity: Int,
        reason: String,
        remarks: String? = null,
    ): Long = repository.createAdjustment(batchId, type, quantity, reason, remarks)
}

class ObserveStockAdjustmentsUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke(): Flow<List<StockAdjustment>> = repository.observeAdjustments()
}

class ObserveDamageAdjustmentsUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke(): Flow<List<StockAdjustment>> = repository.observeDamageAdjustments()
}

class ObserveExpiryReportUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke(): Flow<List<ExpiryReportRow>> = repository.observeExpiryReport()
}
