package com.medipro.manager.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.OcrMedicineAlias
import com.medipro.manager.domain.usecase.ocr.DeleteAllOcrAliasesUseCase
import com.medipro.manager.domain.usecase.ocr.DeleteOcrAliasUseCase
import com.medipro.manager.domain.usecase.ocr.ObserveOcrAliasesUseCase
import com.medipro.manager.domain.usecase.ocr.SetOcrAliasEnabledUseCase
import com.medipro.manager.domain.usecase.ocr.UpdateOcrAliasUseCase
import com.medipro.manager.domain.usecase.purchase.SearchMedicinesForPurchaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrLearningUiState(
    val allAliases: List<OcrMedicineAlias> = emptyList(),
    val searchQuery: String = "",
    val editingAlias: OcrMedicineAlias? = null,
    val editOcrText: String = "",
    val editMedicineQuery: String = "",
    val editMedicineResults: List<Medicine> = emptyList(),
    val selectedMedicineId: Long? = null,
    val selectedMedicineName: String = "",
    val isSaving: Boolean = false,
    val showResetConfirm: Boolean = false,
    val isResetting: Boolean = false,
    val message: String? = null,
) {
    val visibleAliases: List<OcrMedicineAlias>
        get() {
            val q = searchQuery.trim()
            if (q.isBlank()) return allAliases
            return allAliases.filter {
                it.ocrText.contains(q, ignoreCase = true) ||
                    it.medicineName.contains(q, ignoreCase = true)
            }
        }
}

@HiltViewModel
class OcrLearningViewModel @Inject constructor(
    observeOcrAliases: ObserveOcrAliasesUseCase,
    private val deleteOcrAlias: DeleteOcrAliasUseCase,
    private val deleteAllOcrAliases: DeleteAllOcrAliasesUseCase,
    private val setOcrAliasEnabled: SetOcrAliasEnabledUseCase,
    private val updateOcrAlias: UpdateOcrAliasUseCase,
    private val searchMedicines: SearchMedicinesForPurchaseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OcrLearningUiState())
    val state: StateFlow<OcrLearningUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeOcrAliases().collect { aliases ->
                _state.update { it.copy(allAliases = aliases) }
            }
        }
    }

    fun updateSearchQuery(query: String) = _state.update { it.copy(searchQuery = query) }

    fun deleteAlias(id: Long) {
        viewModelScope.launch {
            runCatching { deleteOcrAlias(id) }
                .onSuccess { _state.update { it.copy(message = "Mapping deleted") } }
                .onFailure { e -> _state.update { it.copy(message = e.message) } }
        }
    }

    fun toggleEnabled(alias: OcrMedicineAlias) {
        viewModelScope.launch {
            runCatching { setOcrAliasEnabled(alias.id, !alias.isEnabled) }
                .onSuccess {
                    _state.update {
                        it.copy(message = if (alias.isEnabled) "Mapping disabled" else "Mapping enabled")
                    }
                }
        }
    }

    fun startEdit(alias: OcrMedicineAlias) {
        _state.update {
            it.copy(
                editingAlias = alias,
                editOcrText = alias.ocrText,
                editMedicineQuery = alias.medicineName,
                selectedMedicineId = alias.medicineId,
                selectedMedicineName = alias.medicineName,
                editMedicineResults = emptyList(),
            )
        }
    }

    fun dismissEdit() = _state.update {
        it.copy(
            editingAlias = null,
            editOcrText = "",
            editMedicineQuery = "",
            editMedicineResults = emptyList(),
            selectedMedicineId = null,
            selectedMedicineName = "",
        )
    }

    fun onEditOcrTextChange(value: String) = _state.update { it.copy(editOcrText = value) }

    fun onEditMedicineQueryChange(query: String) {
        _state.update { it.copy(editMedicineQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(editMedicineResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            runCatching { searchMedicines(query) }
                .onSuccess { results -> _state.update { it.copy(editMedicineResults = results) } }
        }
    }

    fun selectMedicine(medicine: Medicine) = _state.update {
        it.copy(
            selectedMedicineId = medicine.id,
            selectedMedicineName = medicine.brandName,
            editMedicineQuery = medicine.brandName,
            editMedicineResults = emptyList(),
        )
    }

    fun saveEdit() {
        val alias = _state.value.editingAlias ?: return
        val medicineId = _state.value.selectedMedicineId ?: return
        val ocrText = _state.value.editOcrText.trim()
        if (ocrText.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching {
                updateOcrAlias(
                    id = alias.id,
                    ocrText = ocrText,
                    medicineId = medicineId,
                    medicineName = _state.value.selectedMedicineName,
                )
            }.onSuccess {
                dismissEdit()
                _state.update { it.copy(isSaving = false, message = "Mapping updated") }
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, message = e.message) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun showResetConfirm() = _state.update { it.copy(showResetConfirm = true) }

    fun dismissResetConfirm() = _state.update { it.copy(showResetConfirm = false) }

    fun confirmResetLearning() {
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            runCatching { deleteAllOcrAliases() }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isResetting = false,
                            showResetConfirm = false,
                            message = "All OCR learned aliases deleted",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isResetting = false, message = e.message ?: "Reset failed")
                    }
                }
        }
    }
}
