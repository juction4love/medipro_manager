package com.medipro.manager.feature.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.repository.SaleRepository
import com.medipro.manager.domain.usecase.sales.GetOrGenerateInvoiceUseCase
import com.medipro.manager.domain.usecase.sales.PrintInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaleInvoiceUiState(
    val isLoading: Boolean = true,
    val sale: Sale? = null,
    val pdfPath: String? = null,
    val isGeneratingPdf: Boolean = false,
    val isPrinting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class SaleInvoiceViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val getOrGenerateInvoice: GetOrGenerateInvoiceUseCase,
    private val printInvoice: PrintInvoiceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SaleInvoiceUiState())
    val state = _state.asStateFlow()

    fun load(invoiceRef: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val sale = saleRepository.resolveSale(invoiceRef)
            _state.update { it.copy(isLoading = false, sale = sale) }
            sale?.let { ensureInvoicePdf() }
        }
    }

    fun ensureInvoicePdf() {
        val sale = _state.value.sale ?: return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingPdf = true, errorMessage = null) }
            runCatching { getOrGenerateInvoice(sale) }
                .onSuccess { doc ->
                    _state.update { it.copy(pdfPath = doc.filePath, isGeneratingPdf = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isGeneratingPdf = false, errorMessage = e.message ?: "Could not generate PDF")
                    }
                }
        }
    }

    fun printThermal(onResult: (Result<Unit>) -> Unit) {
        val sale = _state.value.sale ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPrinting = true) }
            val result = printInvoice.thermal(sale)
            result.onSuccess {
                reloadSale(sale.invoiceNumber)
            }
            _state.update { it.copy(isPrinting = false) }
            onResult(result)
        }
    }

    fun reprintPdf() {
        val sale = _state.value.sale ?: return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingPdf = true) }
            runCatching { printInvoice.regenerateForReprint(sale) }
                .onSuccess { doc ->
                    reloadSale(sale.invoiceNumber)
                    _state.update { it.copy(pdfPath = doc.filePath, isGeneratingPdf = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isGeneratingPdf = false, errorMessage = e.message ?: "Reprint failed")
                    }
                }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }

    private suspend fun reloadSale(invoiceNumber: String) {
        saleRepository.getSaleByInvoiceNumber(invoiceNumber)?.let { updated ->
            _state.update { it.copy(sale = updated) }
        }
    }
}
