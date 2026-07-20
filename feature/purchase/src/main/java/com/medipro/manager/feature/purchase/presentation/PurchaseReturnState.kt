package com.medipro.manager.feature.purchase.presentation

import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseReturnLine
import com.medipro.manager.domain.model.PurchaseReturnReason

data class PurchaseReturnState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val purchase: Purchase? = null,
    val lines: List<PurchaseReturnLine> = emptyList(),
    val selectedReason: PurchaseReturnReason = PurchaseReturnReason.DAMAGED,
    val notes: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val activeLines: List<PurchaseReturnLine> get() = lines.filter { it.returnQty > 0 }
    val subtotal: Double get() = activeLines.sumOf { it.lineSubtotal }
    val vatTotal: Double get() = activeLines.sumOf { it.lineVat }
    val grandTotal: Double get() = subtotal + vatTotal
    val canSave: Boolean get() = activeLines.isNotEmpty() && !isSaving && purchase != null
}

sealed interface PurchaseReturnEvent {
    data class ReturnCompleted(val returnNumber: String) : PurchaseReturnEvent
    data class ShowError(val message: String) : PurchaseReturnEvent
}
