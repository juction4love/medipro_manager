package com.medipro.manager.feature.accounting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Expense
import com.medipro.manager.domain.model.ExpenseCategories
import com.medipro.manager.domain.usecase.accounting.ObserveTodayExpensesUseCase
import com.medipro.manager.domain.usecase.accounting.RecordExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesState(
    val expenses: List<Expense> = emptyList(),
    val category: String = ExpenseCategories.ALL.first(),
    val description: String = "",
    val amountInput: String = "",
    val paymentMethod: String = "CASH",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val todayTotal: Double
        get() = expenses.sumOf { it.amount }

    val canSave: Boolean
        get() = description.isNotBlank() &&
            amountInput.toDoubleOrNull()?.let { it > 0 } == true &&
            !isSaving
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    observeTodayExpenses: ObserveTodayExpensesUseCase,
    private val recordExpense: RecordExpenseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ExpensesState())
    val state: StateFlow<ExpensesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeTodayExpenses().collect { expenses ->
                _state.update { it.copy(expenses = expenses) }
            }
        }
    }

    fun onCategoryChange(category: String) = _state.update { it.copy(category = category) }

    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }

    fun onAmountChange(value: String) = _state.update { it.copy(amountInput = value) }

    fun onPaymentMethodChange(method: String) = _state.update { it.copy(paymentMethod = method) }

    fun saveExpense() {
        val current = _state.value
        val amount = current.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0 || current.description.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                recordExpense(
                    category = current.category,
                    description = current.description.trim(),
                    amount = amount,
                    paymentMethod = current.paymentMethod,
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSaving = false,
                        description = "",
                        amountInput = "",
                        successMessage = "Expense recorded",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save expense")
                }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
