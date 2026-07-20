package com.medipro.manager.feature.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.InventoryMedicineStock
import com.medipro.manager.domain.model.StockAdjustmentType
import com.medipro.manager.domain.usecase.inventory.CreateStockAdjustmentUseCase
import com.medipro.manager.domain.usecase.inventory.GetStockAdjustmentContextUseCase
import com.medipro.manager.domain.usecase.inventory.ObserveDamageAdjustmentsUseCase
import com.medipro.manager.domain.usecase.inventory.ObserveExpiryReportUseCase
import com.medipro.manager.domain.usecase.inventory.ObserveInventorySummaryUseCase
import com.medipro.manager.domain.usecase.inventory.ObserveStockAdjustmentsUseCase
import com.medipro.manager.domain.usecase.inventory.SearchInventoryMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class InventoryViewModel @Inject constructor(
    observeSummary: ObserveInventorySummaryUseCase,
    observeAdjustments: ObserveStockAdjustmentsUseCase,
    private val searchMedicines: SearchInventoryMedicinesUseCase,
    private val getAdjustmentContext: GetStockAdjustmentContextUseCase,
    private val createAdjustment: CreateStockAdjustmentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events: SharedFlow<InventoryEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeSummary().collect { summary ->
                _state.update { it.copy(isLoading = false, summary = summary) }
            }
        }
        viewModelScope.launch {
            observeAdjustments().collect { list ->
                _state.update { it.copy(adjustments = list) }
            }
        }
    }

    fun onTabSelected(tab: InventoryTab) = _state.update { it.copy(selectedTab = tab) }

    fun onSearchQueryChange(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                selectedMedicine = null,
                selectedBatchId = null,
                inlineAdjustment = null,
            )
        }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            runCatching { searchMedicines(query) }
                .onSuccess { results ->
                    _state.update { it.copy(searchResults = results, errorMessage = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun selectMedicine(stock: InventoryMedicineStock) {
        _state.update {
            it.copy(
                selectedMedicine = stock,
                selectedBatchId = null,
                inlineAdjustment = null,
            )
        }
    }

    fun clearSelection() = _state.update {
        it.copy(selectedMedicine = null, selectedBatchId = null, inlineAdjustment = null)
    }

    fun selectBatch(batchId: Long, type: StockAdjustmentType? = null) {
        _state.update {
            it.copy(
                selectedBatchId = batchId,
                inlineAdjustment = InlineAdjustmentDraft(
                    batchId = batchId,
                    selectedType = type ?: StockAdjustmentType.MANUAL_CORRECTION,
                    reason = defaultReason(type ?: StockAdjustmentType.MANUAL_CORRECTION),
                ),
            )
        }
        loadBatchContext(batchId, type)
    }

    fun clearBatchSelection() = _state.update {
        it.copy(selectedBatchId = null, inlineAdjustment = null)
    }

    fun onAdjustmentTypeSelected(type: StockAdjustmentType) {
        _state.update {
            val draft = it.inlineAdjustment ?: return@update it
            it.copy(
                inlineAdjustment = draft.copy(
                    selectedType = type,
                    reason = defaultReason(type),
                ),
            )
        }
    }

    fun onAdjustmentQuantityChange(value: String) {
        _state.update {
            val draft = it.inlineAdjustment ?: return@update it
            it.copy(inlineAdjustment = draft.copy(quantityInput = value))
        }
    }

    fun onAdjustmentReasonChange(value: String) {
        _state.update {
            val draft = it.inlineAdjustment ?: return@update it
            it.copy(inlineAdjustment = draft.copy(reason = value))
        }
    }

    fun onAdjustmentRemarksChange(value: String) {
        _state.update {
            val draft = it.inlineAdjustment ?: return@update it
            it.copy(inlineAdjustment = draft.copy(remarks = value))
        }
    }

    fun saveInlineAdjustment() {
        val current = _state.value
        val draft = current.inlineAdjustment ?: return
        if (!draft.canSave) return

        viewModelScope.launch {
            _state.update {
                it.copy(inlineAdjustment = draft.copy(isSaving = true), errorMessage = null)
            }
            runCatching {
                createAdjustment(
                    batchId = draft.batchId,
                    type = draft.selectedType,
                    quantity = draft.quantity,
                    reason = draft.reason,
                    remarks = draft.remarks.takeIf { it.isNotBlank() },
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        inlineAdjustment = null,
                        selectedBatchId = null,
                        successMessage = "Stock updated",
                    )
                }
                refreshSelectedMedicine()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        inlineAdjustment = draft.copy(isSaving = false),
                        errorMessage = e.message ?: "Adjustment failed",
                    )
                }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }

    fun openAdjustment(batchId: Long, type: String? = null) {
        viewModelScope.launch {
            _events.emit(InventoryEvent.OpenAdjustment(batchId, type))
        }
    }

    fun startAdjustmentFlow() {
        _state.update {
            it.copy(
                selectedTab = InventoryTab.STOCK,
                selectedMedicine = null,
                selectedBatchId = null,
                inlineAdjustment = null,
                searchQuery = "",
                searchResults = emptyList(),
                errorMessage = null,
            )
        }
        viewModelScope.launch { _events.emit(InventoryEvent.FocusStockSearch) }
    }

    private fun loadBatchContext(batchId: Long, type: StockAdjustmentType?) {
        viewModelScope.launch {
            runCatching { getAdjustmentContext(batchId) }
                .onSuccess { ctx ->
                    _state.update { state ->
                        val draft = state.inlineAdjustment ?: return@update state
                        if (ctx == null) {
                            state.copy(
                                inlineAdjustment = null,
                                selectedBatchId = null,
                                errorMessage = "Batch not found",
                            )
                        } else {
                            state.copy(
                                inlineAdjustment = draft.copy(
                                    isLoading = false,
                                    context = ctx,
                                    selectedType = type ?: draft.selectedType,
                                    reason = defaultReason(type ?: draft.selectedType),
                                ),
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            inlineAdjustment = null,
                            selectedBatchId = null,
                            errorMessage = e.message,
                        )
                    }
                }
        }
    }

    private fun refreshSelectedMedicine() {
        val medicine = _state.value.selectedMedicine ?: return
        val query = medicine.medicine.brandName
        viewModelScope.launch {
            runCatching { searchMedicines(query) }
                .onSuccess { results ->
                    val updated = results.find { it.medicine.id == medicine.medicine.id }
                    if (updated != null) {
                        _state.update { it.copy(selectedMedicine = updated) }
                    }
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

@HiltViewModel
class InventoryReportsViewModel @Inject constructor(
    observeDamage: ObserveDamageAdjustmentsUseCase,
    observeExpiry: ObserveExpiryReportUseCase,
) : ViewModel() {
    val damageRows = observeDamage()
    val expiryRows = observeExpiry()
}
