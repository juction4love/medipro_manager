package com.medipro.manager.feature.inventory.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.StockAdjustmentType
import com.medipro.manager.domain.usecase.inventory.CreateStockAdjustmentUseCase
import com.medipro.manager.domain.usecase.inventory.GetStockAdjustmentContextUseCase
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
class StockAdjustmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getContext: GetStockAdjustmentContextUseCase,
    private val createAdjustment: CreateStockAdjustmentUseCase,
) : ViewModel() {

    private val batchId: Long = savedStateHandle.get<Long>("batchId") ?: 0L
    private val initialType: String? = savedStateHandle.get<String>("type")

    private val _state = MutableStateFlow(StockAdjustmentState())
    val state: StateFlow<StockAdjustmentState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<StockAdjustmentEvent>()
    val events: SharedFlow<StockAdjustmentEvent> = _events.asSharedFlow()

    init {
        initialType?.takeIf { it.isNotBlank() }?.let { key ->
            _state.update { it.copy(selectedType = StockAdjustmentType.fromKey(key)) }
        }
        loadContext()
    }

    private fun loadContext() {
        if (batchId <= 0) {
            _state.update { it.copy(isLoading = false, errorMessage = "Select a batch first") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getContext(batchId) }
                .onSuccess { ctx ->
                    if (ctx == null) {
                        _state.update { it.copy(isLoading = false, errorMessage = "Batch not found") }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                context = ctx,
                                reason = defaultReason(it.selectedType),
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load batch")
                    }
                }
        }
    }

    fun onTypeSelected(type: StockAdjustmentType) {
        _state.update { it.copy(selectedType = type, reason = defaultReason(type)) }
    }

    fun onQuantityChange(value: String) = _state.update { it.copy(quantityInput = value) }

    fun onReasonChange(value: String) = _state.update { it.copy(reason = value) }

    fun onRemarksChange(value: String) = _state.update { it.copy(remarks = value) }

    fun save() {
        val current = _state.value
        if (!current.canSave || batchId <= 0) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                createAdjustment(
                    batchId = batchId,
                    type = current.selectedType,
                    quantity = current.quantity,
                    reason = current.reason,
                    remarks = current.remarks.takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false) }
                _events.emit(StockAdjustmentEvent.Saved("Adjustment saved"))
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, errorMessage = e.message) }
                _events.emit(StockAdjustmentEvent.ShowError(e.message ?: "Save failed"))
            }
        }
    }

    private fun defaultReason(type: StockAdjustmentType): String = when (type) {
        StockAdjustmentType.DAMAGE -> "Damaged stock"
        StockAdjustmentType.EXPIRED -> "Expired stock"
        StockAdjustmentType.LOST -> "Stock missing during physical verification"
        StockAdjustmentType.PHYSICAL_COUNT -> "Physical count correction"
        StockAdjustmentType.OPENING_STOCK -> "Opening stock entry"
        StockAdjustmentType.FREE_SAMPLE -> "Free sample distribution"
        else -> type.label
    }
}
