package com.medipro.manager.feature.sales.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.PosCartItem
import com.medipro.manager.domain.model.PosSearchResponse
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.StockBatch
import com.medipro.manager.domain.model.toSaleItem
import com.medipro.manager.domain.usecase.customer.ObserveCustomersUseCase
import com.medipro.manager.domain.usecase.sales.CreateSaleUseCase
import com.medipro.manager.domain.usecase.sales.FindPosAlternativesUseCase
import com.medipro.manager.domain.usecase.sales.GenerateInvoiceNumberUseCase
import com.medipro.manager.domain.usecase.sales.GenerateInvoiceUseCase
import com.medipro.manager.domain.usecase.sales.PrintInvoiceUseCase
import com.medipro.manager.domain.usecase.sales.GetSaleByInvoiceNumberUseCase
import com.medipro.manager.domain.usecase.sales.GetAvailableBatchesUseCase
import com.medipro.manager.domain.usecase.sales.GetSaleByIdUseCase
import com.medipro.manager.domain.usecase.sales.LookupPosBarcodeUseCase
import com.medipro.manager.domain.usecase.sales.SearchPosMedicinesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val searchPos: SearchPosMedicinesUseCase,
    private val findAlternatives: FindPosAlternativesUseCase,
    private val lookupBarcode: LookupPosBarcodeUseCase,
    private val getAvailableBatches: GetAvailableBatchesUseCase,
    private val generateInvoiceNumber: GenerateInvoiceNumberUseCase,
    private val createSale: CreateSaleUseCase,
    private val getSaleById: GetSaleByIdUseCase,
    private val getSaleByInvoiceNumber: GetSaleByInvoiceNumberUseCase,
    private val generateInvoice: GenerateInvoiceUseCase,
    private val printInvoice: PrintInvoiceUseCase,
    private val observeCustomers: ObserveCustomersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SalesState())
    val state: StateFlow<SalesState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SalesEvent>()
    val events: SharedFlow<SalesEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeCustomers().collect { customers ->
                _state.update { it.copy(customers = customers) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, errorMessage = null, selectedResult = null, alternativeBrands = emptyList()) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), didYouMean = null, isSearching = false, searchDurationMs = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            var response = PosSearchResponse(emptyList())
            val elapsed = measureTimeMillis {
                runCatching { searchPos(query) }
                    .onSuccess { response = it }
                    .onFailure { e -> _state.update { s -> s.copy(errorMessage = e.message) } }
            }
            _state.update {
                it.copy(
                    searchResults = response.results,
                    didYouMean = response.didYouMean,
                    isSearching = false,
                    searchDurationMs = elapsed,
                )
            }
        }
    }

    fun onBarcodeSubmit(barcode: String) = viewModelScope.launch {
        if (barcode.isBlank()) return@launch
        runCatching { lookupBarcode(barcode.trim()) }
            .onSuccess { result ->
                if (result != null) addSearchResultToCart(result, autoSelectFefo = true)
                else _state.update { it.copy(errorMessage = "Medicine not found for barcode") }
            }
    }

    fun onBarcodeQueryChange(value: String) = _state.update { it.copy(barcodeQuery = value) }

    fun toggleBarcodeScanner(show: Boolean) = _state.update { it.copy(showBarcodeScanner = show) }

    fun onBarcodeScanned(barcode: String) {
        _state.update { it.copy(barcodeQuery = barcode, showBarcodeScanner = false) }
        onBarcodeSubmit(barcode)
    }

    fun selectSearchResult(result: PosSearchResult) {
        _state.update { it.copy(selectedResult = result) }
        viewModelScope.launch {
            result.medicineId?.let { id ->
                runCatching { findAlternatives(id) }
                    .onSuccess { alts -> _state.update { it.copy(alternativeBrands = alts) } }
            }
        }
    }

    fun addSearchResultToCart(result: PosSearchResult, autoSelectFefo: Boolean = false) {
        if (!result.inStock || result.medicineId == null) {
            _state.update { it.copy(errorMessage = "${result.brandName} is not in stock") }
            return
        }
        val medicineId = result.medicineId ?: return
        viewModelScope.launch {
            runCatching {
                addMedicineById(medicineId, result, autoSelectFefo)
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onBatchPickerSelect(batchId: Long) = _state.update { it.copy(selectedBatchId = batchId) }

    fun confirmBatchSelection() {
        val current = _state.value
        val medicine = current.batchPickerMedicine ?: return
        val batchId = current.selectedBatchId ?: return
        val batch = current.availableBatches.find { it.batchId == batchId } ?: return
        val medicineId = medicine.medicineId ?: return
        viewModelScope.launch {
            runCatching {
                addToCartWithBatch(batch, medicineId, medicine)
                dismissBatchPicker()
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun useFefoForBatchPicker() {
        _state.value.availableBatches.firstOrNull()?.let { fefo ->
            _state.update { it.copy(selectedBatchId = fefo.batchId) }
        }
    }

    fun dismissBatchPicker() = _state.update {
        it.copy(
            showBatchPicker = false,
            batchPickerMedicine = null,
            availableBatches = emptyList(),
            selectedBatchId = null,
        )
    }

    private suspend fun addMedicineById(
        medicineId: Long,
        meta: PosSearchResult,
        autoSelectFefo: Boolean = false,
    ) {
        val batches = getAvailableBatches(medicineId)
        if (batches.isEmpty()) throw IllegalStateException("No stock available for ${meta.brandName}")
        if (batches.size == 1 || autoSelectFefo) {
            addToCartWithBatch(batches.first(), medicineId, meta)
        } else {
            _state.update {
                it.copy(
                    showBatchPicker = true,
                    batchPickerMedicine = meta,
                    availableBatches = batches,
                    selectedBatchId = batches.first().batchId,
                )
            }
        }
    }

    private fun addToCartWithBatch(batch: StockBatch, medicineId: Long, meta: PosSearchResult) {
        val existing = _state.value.cart.find { it.medicineId == medicineId && it.batchId == batch.batchId }
        if (existing != null) {
            updateCartQuantity(existing.batchId, existing.medicineId, existing.quantity + 1)
        } else {
            val item = PosCartItem(
                medicineId = medicineId,
                medicineUuid = "",
                medicineName = meta.brandName,
                batchId = batch.batchId,
                batchUuid = batch.batchUuid,
                batchNumber = batch.batchNumber,
                expiryDate = batch.expiryDate,
                quantity = 1,
                unitPrice = meta.sellingPrice.takeIf { it > 0 } ?: batch.sellingPrice,
                mrp = meta.mrp,
                vatPercent = 13.0,
                availableStock = batch.availableQuantity,
                requiresPrescription = meta.requiresPrescription,
                scheduleCategory = meta.scheduleCategory,
            )
            _state.update {
                it.copy(
                    cart = it.cart + item,
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedResult = null,
                    alternativeBrands = emptyList(),
                    errorMessage = null,
                )
            }
        }
    }

    fun updateCartQuantity(batchId: Long, medicineId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(batchId, medicineId)
            return
        }
        viewModelScope.launch {
            val batches = getAvailableBatches(medicineId)
            val batch = batches.find { it.batchId == batchId } ?: return@launch
            val capped = quantity.coerceAtMost(batch.availableQuantity)
            _state.update { state ->
                state.copy(
                    cart = state.cart.map { item ->
                        if (item.batchId == batchId && item.medicineId == medicineId) {
                            item.copy(quantity = capped)
                        } else item
                    }
                )
            }
            // FEFO: if qty exceeds first batch, add overflow to next batch
            if (quantity > batch.availableQuantity && batches.size > 1) {
                val overflow = quantity - batch.availableQuantity
                val nextBatch = batches[1]
                val existingNext = _state.value.cart.find {
                    it.medicineId == medicineId && it.batchId == nextBatch.batchId
                }
                if (existingNext != null) {
                    updateCartQuantity(nextBatch.batchId, medicineId, existingNext.quantity + overflow)
                } else {
                    val firstItem = _state.value.cart.find { it.batchId == batchId && it.medicineId == medicineId } ?: return@launch
                    val overflowQty = overflow.coerceAtMost(nextBatch.availableQuantity)
                    if (overflowQty > 0) {
                        _state.update {
                            it.copy(
                                cart = it.cart + firstItem.copy(
                                    batchId = nextBatch.batchId,
                                    batchUuid = nextBatch.batchUuid,
                                    batchNumber = nextBatch.batchNumber,
                                    expiryDate = nextBatch.expiryDate,
                                    quantity = overflowQty,
                                    availableStock = nextBatch.availableQuantity,
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateLineDiscount(batchId: Long, medicineId: Long, discount: Double) {
        _state.update { state ->
            state.copy(
                cart = state.cart.map { item ->
                    if (item.batchId == batchId && item.medicineId == medicineId) {
                        item.copy(discount = discount.coerceAtLeast(0.0))
                    } else item
                }
            )
        }
    }

    fun removeFromCart(batchId: Long, medicineId: Long) {
        _state.update { state ->
            state.copy(cart = state.cart.filterNot { it.batchId == batchId && it.medicineId == medicineId })
        }
    }

    fun clearCart() = _state.update {
        it.copy(
            cart = emptyList(),
            billDiscount = 0.0,
            paidAmount = "",
            showBatchPicker = false,
            batchPickerMedicine = null,
            availableBatches = emptyList(),
            selectedBatchId = null,
        )
    }

    fun onBillDiscountChange(value: Double) = _state.update { it.copy(billDiscount = value.coerceAtLeast(0.0)) }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _state.update {
            val paid = when (method) {
                PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.ESEWA,
                PaymentMethod.KHALTI, PaymentMethod.IME_PAY -> it.totalAmount.toString()
                PaymentMethod.CREDIT -> "0"
                PaymentMethod.MIXED -> it.paidAmount
            }
            it.copy(paymentMethod = method, paidAmount = paid)
        }
    }

    fun onPaidAmountChange(value: String) = _state.update { it.copy(paidAmount = value) }

    fun onCustomerSelected(customerId: Long?) = _state.update { it.copy(selectedCustomerId = customerId) }

    fun onPrescriptionNumberChange(value: String) = _state.update { it.copy(prescriptionNumber = value) }

    fun onDoctorNameChange(value: String) = _state.update { it.copy(doctorName = value) }

    fun onPatientNameChange(value: String) = _state.update { it.copy(patientName = value) }

    fun toggleCheckout(show: Boolean) {
        _state.update {
            val paid = if (show && it.paidAmount.isBlank()) it.totalAmount.toString() else it.paidAmount
            it.copy(showCheckout = show, paidAmount = paid)
        }
    }

    fun completeSale() {
        val current = _state.value
        if (current.cart.isEmpty()) {
            _state.update { it.copy(errorMessage = "Cart is empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, errorMessage = null) }
            runCatching {
                val invoiceNumber = generateInvoiceNumber()
                val paid = current.paidAmount.toDoubleOrNull() ?: 0.0
                val isCredit = current.paymentMethod == PaymentMethod.CREDIT ||
                    (current.paymentMethod == PaymentMethod.MIXED && paid < current.totalAmount)

                if (isCredit && current.selectedCustomerId == null) {
                    throw IllegalStateException("Select a customer for credit sale")
                }
                if (current.requiresPrescriptionDetails) {
                    if (current.prescriptionNumber.isBlank() || current.doctorName.isBlank() || current.patientName.isBlank()) {
                        throw IllegalStateException("Prescription details required for Rx / Schedule H medicines")
                    }
                }
                if (paid > current.totalAmount) {
                    throw IllegalStateException("Paid amount exceeds total")
                }

                val paymentStatus = when {
                    paid >= current.totalAmount -> "PAID"
                    paid <= 0.0 && isCredit -> "CREDIT"
                    else -> "PARTIAL"
                }

                val sale = Sale(
                    customerId = current.selectedCustomerId,
                    customerName = current.customers.find { it.id == current.selectedCustomerId }?.name,
                    invoiceNumber = invoiceNumber,
                    subtotal = current.subtotal,
                    discount = current.billDiscount,
                    vatAmount = current.vatTotal,
                    totalAmount = current.totalAmount,
                    paidAmount = paid,
                    paymentStatus = paymentStatus,
                    paymentMethod = current.paymentMethod.name,
                    isCredit = isCredit,
                    prescriptionNumber = current.prescriptionNumber.ifBlank { null },
                    doctorName = current.doctorName.ifBlank { null },
                    patientName = current.patientName.ifBlank { null },
                    items = current.cart.map { it.toSaleItem() },
                )

                val saleId = createSale(sale)
                val savedSale = getSaleById(saleId) ?: sale.copy(id = saleId)
                val invoiceDoc = generateInvoice(savedSale)
                Triple(invoiceDoc.filePath, invoiceNumber, savedSale.totalAmount to savedSale.id)
            }.onSuccess { (path, invoiceNumber, totalAndId) ->
                val (total, saleId) = totalAndId
                val receipt = CompletedSaleReceipt(
                    saleId = saleId,
                    invoiceNumber = invoiceNumber,
                    totalAmount = total,
                    pdfPath = path,
                )
                _state.update {
                    it.copy(
                        isProcessing = false,
                        cart = emptyList(),
                        billDiscount = 0.0,
                        paidAmount = "",
                        prescriptionNumber = "",
                        doctorName = "",
                        patientName = "",
                        showCheckout = false,
                        successMessage = "Sale completed: $invoiceNumber",
                        lastInvoicePath = path,
                        lastReceipt = receipt,
                        completedReceipt = receipt,
                    )
                }
                _events.emit(SalesEvent.SaleCompleted(invoiceNumber))
            }.onFailure { e ->
                _state.update { it.copy(isProcessing = false, errorMessage = e.message ?: "Sale failed") }
                _events.emit(SalesEvent.ShowError(e.message ?: "Sale failed"))
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }

    fun dismissCompletedReceipt() = _state.update { it.copy(completedReceipt = null) }

    fun startNewSale() {
        _state.update {
            it.copy(
                cart = emptyList(),
                billDiscount = 0.0,
                paidAmount = "",
                prescriptionNumber = "",
                doctorName = "",
                patientName = "",
                searchQuery = "",
                searchResults = emptyList(),
                selectedResult = null,
                alternativeBrands = emptyList(),
                showCheckout = false,
                completedReceipt = null,
                errorMessage = null,
                successMessage = "New sale ready — search or scan to add items",
                showBatchPicker = false,
                batchPickerMedicine = null,
                availableBatches = emptyList(),
                selectedBatchId = null,
            )
        }
        viewModelScope.launch { _events.emit(SalesEvent.FocusSearch) }
    }

    fun toggleReturnLookup(show: Boolean) {
        _state.update {
            it.copy(
                showReturnLookup = show,
                returnInvoiceQuery = if (show) it.returnInvoiceQuery else "",
                errorMessage = null,
            )
        }
    }

    fun onReturnInvoiceQueryChange(value: String) = _state.update { it.copy(returnInvoiceQuery = value) }

    fun lookupReturnInvoice() {
        val query = _state.value.returnInvoiceQuery.trim()
        if (query.isBlank()) {
            _state.update { it.copy(errorMessage = "Enter invoice number") }
            return
        }
        viewModelScope.launch {
            runCatching { getSaleByInvoiceNumber(query) }
                .onSuccess { sale ->
                    if (sale == null) {
                        _state.update { it.copy(errorMessage = "Invoice not found: $query") }
                    } else {
                        _state.update { it.copy(showReturnLookup = false, returnInvoiceQuery = "") }
                        _events.emit(SalesEvent.OpenReturn(sale.id))
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message ?: "Lookup failed") }
                }
        }
    }

    fun onSearchSubmit() {
        val query = _state.value.searchQuery.trim()
        if (query.all { it.isDigit() } && query.length >= 6) {
            onBarcodeSubmit(query)
        }
    }

    fun printCompletedReceipt(onResult: (kotlin.Result<Unit>) -> Unit) {
        val receipt = _state.value.lastReceipt ?: return
        viewModelScope.launch {
            val sale = getSaleById(receipt.saleId) ?: return@launch
            onResult(printInvoice.thermal(sale))
        }
    }
}
