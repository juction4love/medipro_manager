package com.medipro.manager.feature.inventory.presentation

import com.medipro.manager.domain.model.BatchStockDetail
import com.medipro.manager.domain.model.InventoryMedicineStock
import com.medipro.manager.domain.model.InventorySummary
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.StockAdjustmentContext
import com.medipro.manager.domain.model.StockAdjustmentType

data class InlineAdjustmentDraft(
    val batchId: Long,
    val isLoading: Boolean = true,
    val context: StockAdjustmentContext? = null,
    val selectedType: StockAdjustmentType = StockAdjustmentType.MANUAL_CORRECTION,
    val quantityInput: String = "",
    val reason: String = "",
    val remarks: String = "",
    val isSaving: Boolean = false,
) {
    val quantity: Int get() = quantityInput.toIntOrNull() ?: 0

    val previewNewQty: Int?
        get() {
            val ctx = context ?: return null
            val old = ctx.batch.sellableQty
            val qty = quantity
            return when (selectedType) {
                StockAdjustmentType.PHYSICAL_COUNT -> qty
                StockAdjustmentType.STOCK_INCREASE,
                StockAdjustmentType.OPENING_STOCK,
                StockAdjustmentType.FREE_SAMPLE -> old + qty
                StockAdjustmentType.STOCK_DECREASE,
                StockAdjustmentType.MANUAL_CORRECTION,
                StockAdjustmentType.DAMAGE,
                StockAdjustmentType.EXPIRED,
                StockAdjustmentType.LOST -> old - qty
            }
        }

    val canSave: Boolean
        get() = context != null && reason.isNotBlank() && quantityInput.isNotBlank() &&
            !isSaving && (previewNewQty ?: -1) >= 0
}

data class InventoryState(
    val isLoading: Boolean = true,
    val summary: InventorySummary? = null,
    val searchQuery: String = "",
    val searchResults: List<InventoryMedicineStock> = emptyList(),
    val selectedMedicine: InventoryMedicineStock? = null,
    val selectedBatchId: Long? = null,
    val inlineAdjustment: InlineAdjustmentDraft? = null,
    val adjustments: List<StockAdjustment> = emptyList(),
    val selectedTab: InventoryTab = InventoryTab.STOCK,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

enum class InventoryTab(val label: String) {
    STOCK("Stock"),
    HISTORY("Adjustments"),
    REPORTS("Reports"),
}

sealed interface InventoryEvent {
    data class OpenAdjustment(val batchId: Long, val type: String? = null) : InventoryEvent
    data object FocusStockSearch : InventoryEvent
}
