package com.medipro.manager.feature.accounting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.PaymentVoucherInput
import com.medipro.manager.domain.model.PrinterProfile
import com.medipro.manager.domain.model.Supplier
import com.medipro.manager.domain.model.ThermalPrintResult
import com.medipro.manager.domain.usecase.accounting.GeneratePaymentVoucherPdfUseCase
import com.medipro.manager.domain.usecase.accounting.ObserveSuppliersWithDueUseCase
import com.medipro.manager.domain.usecase.accounting.PrintPaymentVoucherUseCase
import com.medipro.manager.domain.usecase.accounting.RecordSupplierPaymentUseCase
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

data class SupplierPaymentsState(
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: Long? = null,
    val amountInput: String = "",
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isPrinting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastVoucherPdfPath: String? = null,
    val lastVoucher: PaymentVoucherInput? = null,
) {
    val selectedSupplier: Supplier?
        get() = suppliers.find { it.id == selectedSupplierId }

    val canSave: Boolean
        get() = selectedSupplierId != null &&
            amountInput.toDoubleOrNull()?.let { it > 0 } == true &&
            !isSaving
}

sealed class SupplierPaymentsEvent {
    data class SharePdf(val path: String) : SupplierPaymentsEvent()
    data class PrintResult(val message: String) : SupplierPaymentsEvent()
}

@HiltViewModel
class SupplierPaymentsViewModel @Inject constructor(
    observeSuppliersWithDue: ObserveSuppliersWithDueUseCase,
    private val recordPayment: RecordSupplierPaymentUseCase,
    private val generatePdf: GeneratePaymentVoucherPdfUseCase,
    private val printVoucher: PrintPaymentVoucherUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierPaymentsState())
    val state: StateFlow<SupplierPaymentsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SupplierPaymentsEvent>()
    val events: SharedFlow<SupplierPaymentsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSuppliersWithDue().collect { suppliers ->
                _state.update { current ->
                    current.copy(
                        suppliers = suppliers,
                        selectedSupplierId = current.selectedSupplierId
                            ?.takeIf { id -> suppliers.any { it.id == id } }
                            ?: suppliers.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun selectSupplier(supplierId: Long) = _state.update {
        it.copy(selectedSupplierId = supplierId, errorMessage = null)
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amountInput = value, errorMessage = null) }

    fun onPaymentMethodChange(method: String) = _state.update { it.copy(paymentMethod = method) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun savePayment() {
        val current = _state.value
        val supplierId = current.selectedSupplierId ?: return
        val supplier = current.selectedSupplier ?: return
        val amount = current.amountInput.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val ref = recordPayment(
                    supplierId = supplierId,
                    amount = amount,
                    paymentMethod = current.paymentMethod,
                    notes = current.notes.takeIf { it.isNotBlank() },
                )
                val now = System.currentTimeMillis()
                val remaining = (supplier.outstandingBalance - amount).coerceAtLeast(0.0)
                val voucher = PaymentVoucherInput(
                    type = "SUPPLIER_PAYMENT",
                    title = "Supplier Payment Voucher",
                    reference = ref,
                    partyName = supplier.name,
                    amount = amount,
                    paymentMethod = current.paymentMethod,
                    notes = current.notes.takeIf { it.isNotBlank() },
                    paymentDate = now,
                    previousBalance = supplier.outstandingBalance,
                    remainingBalance = remaining,
                )
                val pdf = generatePdf(voucher)
                Triple(ref, pdf.absolutePath, voucher)
            }.onSuccess { (ref, pdfPath, voucher) ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        amountInput = "",
                        notes = "",
                        lastVoucherPdfPath = pdfPath,
                        lastVoucher = voucher,
                        successMessage = "Payment saved: $ref",
                    )
                }
                _events.emit(SupplierPaymentsEvent.SharePdf(pdfPath))
                printLastVoucher()
            }.onFailure { e ->
                _state.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save payment")
                }
            }
        }
    }

    fun printLastVoucher() {
        val voucher = _state.value.lastVoucher ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPrinting = true) }
            when (val result = printVoucher(voucher, PrinterProfile.COUNTER)) {
                is ThermalPrintResult.Success ->
                    _events.emit(SupplierPaymentsEvent.PrintResult("Voucher sent to printer"))
                is ThermalPrintResult.Failure ->
                    _events.emit(SupplierPaymentsEvent.PrintResult(result.message))
            }
            _state.update { it.copy(isPrinting = false) }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
