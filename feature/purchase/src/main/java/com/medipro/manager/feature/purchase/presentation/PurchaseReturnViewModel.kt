package com.medipro.manager.feature.purchase.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.PurchaseReturnReason
import com.medipro.manager.domain.usecase.purchase.CreatePurchaseReturnUseCase
import com.medipro.manager.domain.usecase.purchase.GetPurchaseReturnContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseReturnViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReturnContext: GetPurchaseReturnContextUseCase,
    private val createPurchaseReturn: CreatePurchaseReturnUseCase,
) : ViewModel() {

    private val purchaseId: Long = savedStateHandle.get<Long>("purchaseId") ?: 0L

    private val _state = MutableStateFlow(PurchaseReturnState())
    val state: StateFlow<PurchaseReturnState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseReturnEvent>()
    val events: SharedFlow<PurchaseReturnEvent> = _events.asSharedFlow()

    init {
        loadContext()
    }

    fun loadContext() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getReturnContext(purchaseId) }
                .onSuccess { context ->
                    if (context == null) {
                        _state.update { it.copy(isLoading = false, errorMessage = "Purchase not found") }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                purchase = context.purchase,
                                lines = context.lines,
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load purchase")
                    }
                }
        }
    }

    fun onReasonSelected(reason: PurchaseReturnReason) {
        _state.update { it.copy(selectedReason = reason) }
    }

    fun onNotesChange(notes: String) = _state.update { it.copy(notes = notes) }

    fun onReturnQtyChange(purchaseItemUuid: String, qty: Int) {
        _state.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.purchaseItemUuid == purchaseItemUuid) {
                        line.copy(returnQty = qty.coerceIn(0, line.maxReturnableQty))
                    } else line
                }
            )
        }
    }

    fun saveReturn() {
        val current = _state.value
        if (!current.canSave) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                createPurchaseReturn(
                    purchaseId = purchaseId,
                    reason = current.selectedReason.label,
                    lines = current.lines,
                    notes = current.notes.takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false, successMessage = "Return saved") }
                _events.emit(PurchaseReturnEvent.ReturnCompleted("PR saved"))
            }.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Return failed")
                }
                _events.emit(PurchaseReturnEvent.ShowError(e.message ?: "Return failed"))
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
