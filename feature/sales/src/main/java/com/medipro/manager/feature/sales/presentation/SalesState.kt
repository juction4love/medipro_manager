package com.medipro.manager.feature.sales.presentation

import com.medipro.manager.domain.model.Customer
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.PosCartItem
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.StockBatch

data class SalesState(
    val searchQuery: String = "",
    val barcodeQuery: String = "",
    val searchResults: List<PosSearchResult> = emptyList(),
    val didYouMean: String? = null,
    val alternativeBrands: List<PosSearchResult> = emptyList(),
    val selectedResult: PosSearchResult? = null,
    val cart: List<PosCartItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val billDiscount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paidAmount: String = "",
    val isProcessing: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastInvoicePath: String? = null,
    val lastReceipt: CompletedSaleReceipt? = null,
    val completedReceipt: CompletedSaleReceipt? = null,
    val prescriptionNumber: String = "",
    val doctorName: String = "",
    val patientName: String = "",
    val showCheckout: Boolean = false,
    val showBarcodeScanner: Boolean = false,
    val showReturnLookup: Boolean = false,
    val returnInvoiceQuery: String = "",
    val searchDurationMs: Long? = null,
    val showBatchPicker: Boolean = false,
    val batchPickerMedicine: PosSearchResult? = null,
    val availableBatches: List<StockBatch> = emptyList(),
    val selectedBatchId: Long? = null,
) {
    val subtotal: Double get() = cart.sumOf { it.lineSubtotal }
    val vatTotal: Double get() = cart.sumOf { it.vatAmount }
    val totalAmount: Double get() = (subtotal + vatTotal) - billDiscount
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val requiresPrescriptionDetails: Boolean get() = cart.any { it.requiresPrescription || it.scheduleCategory in RX_SCHEDULES }
    val totalMrpSavings: Double get() = cart.sumOf { it.savingsFromMrp }
}

private val RX_SCHEDULES = setOf("H", "H1", "X", "NARCOTIC", "SCHEDULE_H", "SCHEDULE_X")

sealed interface SalesEvent {
    data class ShowError(val message: String) : SalesEvent
    data class SaleCompleted(val invoiceNumber: String) : SalesEvent
    data object FocusSearch : SalesEvent
    data class OpenReturn(val saleId: Long) : SalesEvent
}
