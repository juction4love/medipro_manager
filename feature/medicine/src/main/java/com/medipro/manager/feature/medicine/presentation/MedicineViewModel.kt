package com.medipro.manager.feature.medicine.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.usecase.medicine.AddMedicineUseCase
import com.medipro.manager.domain.usecase.medicine.DeleteMedicineUseCase
import com.medipro.manager.domain.usecase.medicine.ObserveMedicinesUseCase
import com.medipro.manager.domain.usecase.medicine.SearchMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val observeMedicines: ObserveMedicinesUseCase,
    private val searchMedicines: SearchMedicinesUseCase,
    private val addMedicine: AddMedicineUseCase,
    private val deleteMedicine: DeleteMedicineUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MedicineState())
    val state: StateFlow<MedicineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MedicineEvent>()
    val events: SharedFlow<MedicineEvent> = _events.asSharedFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery.flatMapLatest { query ->
                if (query.isBlank()) observeMedicines() else searchMedicines(query)
            }.collect { medicines ->
                _state.update { it.copy(isLoading = false, medicines = medicines) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleAddDialog(show: Boolean) {
        _state.update { it.copy(showAddDialog = show) }
    }

    fun addMedicine(medicine: Medicine) {
        viewModelScope.launch {
            runCatching { addMedicine(medicine) }
                .onSuccess {
                    _state.update { it.copy(showAddDialog = false) }
                    _events.emit(MedicineEvent.MedicineAdded)
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    fun deleteMedicine(id: Long) {
        viewModelScope.launch {
            deleteMedicine(id)
        }
    }
}
