package com.medipro.manager.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseInvoiceUiState(
    val isLoading: Boolean = true,
    val purchase: Purchase? = null,
)

@HiltViewModel
class PurchaseInvoiceViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseInvoiceUiState())
    val state = _state.asStateFlow()

    fun load(invoiceNumber: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val purchase = purchaseRepository.getPurchaseByInvoiceNumber(invoiceNumber)
            _state.update { it.copy(isLoading = false, purchase = purchase) }
        }
    }
}
