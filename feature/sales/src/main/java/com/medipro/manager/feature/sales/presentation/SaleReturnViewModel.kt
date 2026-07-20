package com.medipro.manager.feature.sales.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.SaleReturnReason
import com.medipro.manager.domain.usecase.sales.CreateSaleReturnUseCase
import com.medipro.manager.domain.usecase.sales.GetSaleReturnContextUseCase
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
class SaleReturnViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getReturnContext: GetSaleReturnContextUseCase,
    private val createSaleReturn: CreateSaleReturnUseCase,
) : ViewModel() {

    private val saleId: Long = savedStateHandle.get<Long>("saleId") ?: 0L

    private val _state = MutableStateFlow(SaleReturnState())
    val state: StateFlow<SaleReturnState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SaleReturnEvent>()
    val events: SharedFlow<SaleReturnEvent> = _events.asSharedFlow()

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getReturnContext(saleId) }
                .onSuccess { context ->
                    if (context == null) {
                        _state.update { it.copy(isLoading = false, errorMessage = "Invoice not found") }
                    } else {
                        _state.update {
                            it.copy(isLoading = false, sale = context.sale, lines = context.lines)
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load invoice")
                    }
                }
        }
    }

    fun onReasonSelected(reason: SaleReturnReason) {
        _state.update { it.copy(selectedReason = reason) }
    }

    fun onNotesChange(notes: String) = _state.update { it.copy(notes = notes) }

    fun onReturnQtyChange(invoiceItemUuid: String, qty: Int) {
        _state.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.invoiceItemUuid == invoiceItemUuid) {
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
                createSaleReturn(
                    saleId = saleId,
                    reason = current.selectedReason.label,
                    lines = current.lines,
                    notes = current.notes.takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false) }
                _events.emit(SaleReturnEvent.ReturnCompleted("SR saved"))
            }.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Return failed")
                }
                _events.emit(SaleReturnEvent.ShowError(e.message ?: "Return failed"))
            }
        }
    }
}
