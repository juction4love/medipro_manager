package com.medipro.manager.feature.inventory.presentation

import com.medipro.manager.domain.model.StockAdjustmentContext
import com.medipro.manager.domain.model.StockAdjustmentType

data class StockAdjustmentState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val context: StockAdjustmentContext? = null,
    val selectedType: StockAdjustmentType = StockAdjustmentType.MANUAL_CORRECTION,
    val quantityInput: String = "",
    val reason: String = "",
    val remarks: String = "",
    val errorMessage: String? = null,
) {
    val currentQty: Int get() = context?.batch?.sellableQty ?: 0
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

sealed interface StockAdjustmentEvent {
    data class Saved(val adjustmentNumber: String) : StockAdjustmentEvent
    data class ShowError(val message: String) : StockAdjustmentEvent
}
