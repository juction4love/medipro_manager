package com.medipro.manager.feature.accounting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.usecase.accounting.GeneratePaymentVoucherPdfUseCase
import com.medipro.manager.domain.usecase.accounting.ObserveCustomersWithDueUseCase
import com.medipro.manager.domain.usecase.accounting.RecordCustomerReceiptUseCase
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

data class CustomerReceiptsState(
    val customers: List<Customer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val amountInput: String = "",
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastReceiptPdfPath: String? = null,
) {
    val selectedCustomer: Customer?
        get() = customers.find { it.id == selectedCustomerId }

    val canSave: Boolean
        get() = selectedCustomerId != null &&
            amountInput.toDoubleOrNull()?.let { it > 0 } == true &&
            !isSaving
}

sealed class CustomerReceiptsEvent {
    data class SharePdf(val path: String) : CustomerReceiptsEvent()
}

@HiltViewModel
class CustomerReceiptsViewModel @Inject constructor(
    observeCustomersWithDue: ObserveCustomersWithDueUseCase,
    private val recordReceipt: RecordCustomerReceiptUseCase,
    private val generatePdf: GeneratePaymentVoucherPdfUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerReceiptsState())
    val state: StateFlow<CustomerReceiptsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CustomerReceiptsEvent>()
    val events: SharedFlow<CustomerReceiptsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeCustomersWithDue().collect { customers ->
                _state.update { current ->
                    current.copy(
                        customers = customers,
                        selectedCustomerId = current.selectedCustomerId
                            ?.takeIf { id -> customers.any { it.id == id } }
                            ?: customers.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun selectCustomer(customerId: Long) = _state.update {
        it.copy(selectedCustomerId = customerId, errorMessage = null)
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amountInput = value, errorMessage = null) }

    fun onPaymentMethodChange(method: String) = _state.update { it.copy(paymentMethod = method) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun saveReceipt() {
        val current = _state.value
        val customerId = current.selectedCustomerId ?: return
        val customer = current.selectedCustomer ?: return
        val amount = current.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val ref = recordReceipt(
                    customerId = customerId,
                    amount = amount,
                    paymentMethod = current.paymentMethod,
                    notes = current.notes.takeIf { it.isNotBlank() },
                )
                val now = System.currentTimeMillis()
                val remaining = (customer.outstandingBalance - amount).coerceAtLeast(0.0)
                val pdf = generatePdf(
                    PaymentVoucherInput(
                        type = "CUSTOMER_RECEIPT",
                        title = "Customer Receipt",
                        reference = ref,
                        partyName = customer.name,
                        amount = amount,
                        paymentMethod = current.paymentMethod,
                        notes = current.notes.takeIf { it.isNotBlank() },
                        paymentDate = now,
                        previousBalance = customer.outstandingBalance,
                        remainingBalance = remaining,
                    ),
                )
                ref to pdf.absolutePath
            }.onSuccess { (ref, pdfPath) ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        amountInput = "",
                        notes = "",
                        lastReceiptPdfPath = pdfPath,
                        successMessage = "Receipt saved: $ref",
                    )
                }
                _events.emit(CustomerReceiptsEvent.SharePdf(pdfPath))
            }.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save receipt")
                }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
