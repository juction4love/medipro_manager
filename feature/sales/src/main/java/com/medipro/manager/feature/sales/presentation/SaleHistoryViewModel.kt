package com.medipro.manager.feature.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.usecase.sales.GetOrGenerateInvoiceUseCase
import com.medipro.manager.domain.usecase.sales.ObserveSalesUseCase
import com.medipro.manager.domain.usecase.sales.PrintInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaleHistoryViewModel @Inject constructor(
    observeSales: ObserveSalesUseCase,
    private val getOrGenerateInvoice: GetOrGenerateInvoiceUseCase,
    private val printInvoiceUseCase: PrintInvoiceUseCase,
) : ViewModel() {
    val sales: StateFlow<List<Sale>> = observeSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ensurePdfPath(sale: Sale, onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { getOrGenerateInvoice(sale) }
                .onSuccess { onReady(it.filePath) }
        }
    }

    fun printInvoice(sale: Sale, onResult: (kotlin.Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(printInvoiceUseCase.thermal(sale))
        }
    }
}
