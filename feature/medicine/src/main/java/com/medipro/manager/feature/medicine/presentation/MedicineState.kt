package com.medipro.manager.feature.medicine.presentation

import com.medipro.manager.domain.model.Medicine

data class MedicineState(
    val isLoading: Boolean = true,
    val medicines: List<Medicine> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val errorMessage: String? = null
)

sealed interface MedicineEvent {
    data class ShowMessage(val message: String) : MedicineEvent
    data object MedicineAdded : MedicineEvent
}
