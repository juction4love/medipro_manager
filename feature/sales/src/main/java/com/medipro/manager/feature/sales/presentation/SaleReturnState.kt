package com.medipro.manager.feature.sales.presentation

import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleReturnLine
import com.medipro.manager.domain.model.SaleReturnReason

data class SaleReturnState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val sale: Sale? = null,
    val lines: List<SaleReturnLine> = emptyList(),
    val selectedReason: SaleReturnReason = SaleReturnReason.CUSTOMER_RETURN,
    val notes: String = "",
    val errorMessage: String? = null,
) {
    val activeLines: List<SaleReturnLine> get() = lines.filter { it.returnQty > 0 }
    val subtotal: Double get() = activeLines.sumOf { it.lineSubtotal }
    val vatTotal: Double get() = activeLines.sumOf { it.lineVat }
    val grandTotal: Double get() = subtotal + vatTotal
    val canSave: Boolean get() = activeLines.isNotEmpty() && !isSaving && sale != null
}

sealed interface SaleReturnEvent {
    data class ReturnCompleted(val returnNumber: String) : SaleReturnEvent
    data class ShowError(val message: String) : SaleReturnEvent
}
