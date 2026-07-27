package com.medipro.manager.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.MrpUpdateChoice
import com.medipro.manager.domain.model.OcrMedicineDraft
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.Purchase
import com.medipro.manager.domain.model.PurchaseCartItem
import com.medipro.manager.domain.model.PurchaseBillMatchStatus
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.model.Supplier
import com.medipro.manager.domain.model.mergePurchaseCart
import com.medipro.manager.domain.model.toMedicineDraft
import com.medipro.manager.domain.model.toPurchaseItem
import com.medipro.manager.domain.usecase.ocr.LogOcrScanSessionUseCase
import com.medipro.manager.domain.usecase.ocr.RecordOcrManualCorrectionUseCase
import com.medipro.manager.domain.usecase.ocr.SubmitOcrFeedbackUseCase
import com.medipro.manager.domain.model.OcrFeedbackType
import com.medipro.manager.domain.usecase.purchase.CreateMedicineFromBillDraftUseCase
import com.medipro.manager.domain.usecase.purchase.CreatePurchaseUseCase
import com.medipro.manager.domain.usecase.purchase.GeneratePurchaseInvoiceNumberUseCase
import com.medipro.manager.domain.usecase.purchase.LineMatchForMedicineUseCase
import com.medipro.manager.domain.usecase.purchase.LookupMedicineBarcodeForPurchaseUseCase
import com.medipro.manager.domain.usecase.purchase.ResolveMedicineForPurchaseUseCase
import com.medipro.manager.domain.usecase.purchase.SaveOcrMedicineAliasUseCase
import com.medipro.manager.domain.usecase.purchase.ScanPurchaseBillUseCase
import com.medipro.manager.domain.usecase.purchase.SearchMedicinesForPurchaseUseCase
import com.medipro.manager.domain.usecase.purchase.UpdateMedicineMrpFromBillUseCase
import com.medipro.manager.domain.usecase.supplier.AddSupplierUseCase
import com.medipro.manager.domain.usecase.supplier.ObserveSuppliersUseCase
import com.medipro.manager.domain.usecase.license.CanAccessPremiumFeatureUseCase
import com.medipro.manager.domain.licensing.PremiumFeature
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val searchMedicines: SearchMedicinesForPurchaseUseCase,
    private val lookupBarcode: LookupMedicineBarcodeForPurchaseUseCase,
    private val scanPurchaseBill: ScanPurchaseBillUseCase,
    private val resolveMedicineForPurchase: ResolveMedicineForPurchaseUseCase,
    private val createMedicineFromBillDraft: CreateMedicineFromBillDraftUseCase,
    private val saveOcrMedicineAlias: SaveOcrMedicineAliasUseCase,
    private val lineMatchForMedicine: LineMatchForMedicineUseCase,
    private val updateMedicineMrpFromBill: UpdateMedicineMrpFromBillUseCase,
    private val logOcrScanSession: LogOcrScanSessionUseCase,
    private val recordOcrManualCorrection: RecordOcrManualCorrectionUseCase,
    private val submitOcrFeedback: SubmitOcrFeedbackUseCase,
    private val addSupplier: AddSupplierUseCase,
    private val generateInvoiceNumber: GeneratePurchaseInvoiceNumberUseCase,
    private val createPurchase: CreatePurchaseUseCase,
    private val observeSuppliers: ObserveSuppliersUseCase,
    private val canAccessPremium: CanAccessPremiumFeatureUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseState())
    val state: StateFlow<PurchaseState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>()
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    private var searchJob: Job? = null

    init {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        _state.update { it.copy(purchaseDate = today) }
        viewModelScope.launch {
            observeSuppliers().collect { suppliers ->
                _state.update { it.copy(suppliers = suppliers) }
            }
        }
    }

    fun onSupplierBillNumberChange(value: String) = _state.update { it.copy(supplierBillNumber = value) }

    fun onPurchaseDateChange(value: String) = _state.update { it.copy(purchaseDate = value) }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, errorMessage = null) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            runCatching { searchMedicines(query) }
                .onSuccess { results -> _state.update { it.copy(searchResults = results) } }
        }
    }

    fun toggleBarcodeScanner(show: Boolean) = _state.update { it.copy(showBarcodeScanner = show) }

    fun toggleBillScanner(show: Boolean) {
        if (!show) {
            _state.update { it.copy(showBillScanner = false, errorMessage = null) }
            return
        }
        if (canAccessPremium(PremiumFeature.OCR_PURCHASE)) {
            _state.update { it.copy(showBillScanner = true, errorMessage = null) }
        } else {
            viewModelScope.launch { _events.emit(PurchaseEvent.RequirePremium) }
        }
    }

    fun onBillCaptureComplete(imageUris: List<android.net.Uri>) {
        if (imageUris.isEmpty()) return
        _state.update {
            it.copy(
                isScanningBill = true,
                showBillScanner = false,
                billScanProgress = com.medipro.manager.domain.model.BillScanProgress("Starting…"),
            )
        }
        viewModelScope.launch {
            runCatching {
                scanPurchaseBill(imageUris.map { it.toString() }, _state.value.suppliers) { progress ->
                    _state.update { state -> state.copy(billScanProgress = progress) }
                }
            }.onSuccess { scanned ->
                if (scanned.lines.isEmpty()) {
                    _state.update {
                        it.copy(
                            isScanningBill = false,
                            billScanProgress = null,
                            showOcrFailureDialog = true,
                            lastScanImageUris = scanned.sourceImageUris.ifEmpty { imageUris.map { uri -> uri.toString() } },
                            lastScanOcrText = scanned.rawOcrText,
                            lastScanBill = scanned,
                            errorMessage = scanned.parseWarnings.firstOrNull() ?: "Could not parse bill items",
                        )
                    }
                } else {
                    applyScannedResult(scanned)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isScanningBill = false,
                        billScanProgress = null,
                        errorMessage = e.message ?: "Bill scan failed",
                    )
                }
            }
        }
    }

    private fun applyScannedResult(scanned: ScannedPurchaseBill) {
        val autoSelected = scanned.lines.mapIndexedNotNull { index, line ->
            if (line.status != PurchaseBillMatchStatus.UNMATCHED) index else null
        }.toSet()
        val defaultMrpChoices = scanned.lines.mapIndexedNotNull { index, line ->
            if (line.mrpChanged) index to MrpUpdateChoice.KEEP else null
        }.toMap()
        _state.update {
            it.copy(
                isScanningBill = false,
                billScanProgress = null,
                scannedBillReview = scanned,
                selectedBillLineIndexes = autoSelected,
                duplicateBillDismissed = false,
                resolvedMedicineOverrides = emptyMap(),
                mrpUpdateChoices = defaultMrpChoices,
                controlledVerificationAcknowledged = scanned.controlledCount == 0,
                errorMessage = null,
                successMessage = "Scanned ${scanned.lines.size} items (${scanned.matchedCount} matched) — review & confirm",
            )
        }
        viewModelScope.launch { runCatching { logOcrScanSession(scanned) } }
        if (scanned.matchedSupplierId != null) onSupplierSelected(scanned.matchedSupplierId)
        scanned.invoiceNumber?.let { onSupplierBillNumberChange(it) }
        scanned.invoiceDate?.let { onPurchaseDateChange(it) }
    }

    fun dismissBillReview() = _state.update {
        it.copy(
            scannedBillReview = null,
            selectedBillLineIndexes = emptySet(),
            duplicateBillDismissed = false,
            resolvedMedicineOverrides = emptyMap(),
            mrpUpdateChoices = emptyMap(),
            controlledVerificationAcknowledged = false,
        )
    }

    fun dismissOcrFailureDialog() = _state.update {
        it.copy(showOcrFailureDialog = false, lastScanImageUris = emptyList(), lastScanOcrText = null, lastScanBill = null)
    }

    fun retryBillScan() {
        _state.update {
            it.copy(showOcrFailureDialog = false, showBillScanner = true, lastScanImageUris = emptyList(), lastScanOcrText = null, lastScanBill = null)
        }
    }

    fun submitOcrParserFeedback() {
        val bill = _state.value.lastScanBill
        viewModelScope.launch {
            runCatching {
                submitOcrFeedback(bill, OcrFeedbackType.PARSE_FAIL).getOrThrow()
            }.onSuccess {
                _state.update {
                    it.copy(successMessage = "Anonymous OCR sample sent — thank you", showOcrFailureDialog = false)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(errorMessage = e.message ?: "Enable OCR feedback in Settings to contribute")
                }
            }
        }
    }

    fun setMrpUpdateChoice(lineIndex: Int, choice: MrpUpdateChoice) = _state.update {
        it.copy(mrpUpdateChoices = it.mrpUpdateChoices + (lineIndex to choice))
    }

    fun acknowledgeControlledVerification() = _state.update { it.copy(controlledVerificationAcknowledged = true) }

    fun showManualMatchForLine(index: Int) {
        val line = _state.value.scannedBillReview?.lines?.getOrNull(index) ?: return
        _state.update {
            it.copy(
                showManualMatchLineIndex = index,
                manualMatchQuery = line.parsed.description,
                manualMatchResults = emptyList(),
            )
        }
        onManualMatchQueryChange(line.parsed.description)
    }

    fun dismissManualMatchDialog() = _state.update {
        it.copy(showManualMatchLineIndex = null, manualMatchQuery = "", manualMatchResults = emptyList())
    }

    fun onManualMatchQueryChange(query: String) {
        _state.update { it.copy(manualMatchQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(manualMatchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            runCatching { searchMedicines(query) }
                .onSuccess { results -> _state.update { it.copy(manualMatchResults = results) } }
        }
    }

    fun manualMatchLine(medicine: com.medipro.manager.domain.model.Medicine) {
        val lineIndex = _state.value.showManualMatchLineIndex ?: return
        val review = _state.value.scannedBillReview ?: return
        val parsed = review.lines.getOrNull(lineIndex)?.parsed ?: return
        viewModelScope.launch {
            runCatching {
                saveOcrMedicineAlias(parsed.description, medicine.id, medicine.brandName)
                recordOcrManualCorrection()
                lineMatchForMedicine(parsed, medicine.id)
            }.onSuccess { updatedLine ->
                val updatedLines = review.lines.toMutableList().also { it[lineIndex] = updatedLine }
                val mrpChoices = _state.value.mrpUpdateChoices.toMutableMap()
                if (updatedLine.mrpChanged) mrpChoices[lineIndex] = MrpUpdateChoice.KEEP
                _state.update {
                    it.copy(
                        scannedBillReview = review.copy(lines = updatedLines),
                        selectedBillLineIndexes = it.selectedBillLineIndexes + lineIndex,
                        resolvedMedicineOverrides = it.resolvedMedicineOverrides + (lineIndex to medicine.id),
                        mrpUpdateChoices = mrpChoices,
                        showManualMatchLineIndex = null,
                        manualMatchQuery = "",
                        manualMatchResults = emptyList(),
                        successMessage = "Linked & saved for future scans (${medicine.brandName})",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(errorMessage = e.message ?: "Could not link medicine") }
            }
        }
    }

    fun dismissDuplicateBillWarning() = _state.update { it.copy(duplicateBillDismissed = true) }

    fun showCreateSupplierDialog() = _state.update { it.copy(showCreateSupplierDialog = true) }

    fun dismissCreateSupplierDialog() = _state.update { it.copy(showCreateSupplierDialog = false) }

    fun createSupplierFromScan(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingFromBill = true) }
            runCatching {
                addSupplier(Supplier(name = name.trim()))
            }.onSuccess { id ->
                onSupplierSelected(id)
                _state.update {
                    val review = it.scannedBillReview
                    it.copy(
                        isCreatingFromBill = false,
                        showCreateSupplierDialog = false,
                        scannedBillReview = review?.copy(
                            matchedSupplierId = id,
                            matchedSupplierName = name.trim(),
                        ),
                        successMessage = "Supplier created — matched for this bill",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isCreatingFromBill = false, errorMessage = e.message ?: "Could not create supplier")
                }
            }
        }
    }

    fun showCreateMedicineForLine(index: Int) {
        val line = _state.value.scannedBillReview?.lines?.getOrNull(index) ?: return
        _state.update {
            it.copy(
                showCreateMedicineDialog = true,
                createMedicineLineIndex = index,
                createMedicineDraft = line.parsed.toMedicineDraft(),
            )
        }
    }

    fun dismissCreateMedicineDialog() = _state.update {
        it.copy(showCreateMedicineDialog = false, createMedicineDraft = null, createMedicineLineIndex = null)
    }

    fun saveMedicineFromBillDraft(draft: OcrMedicineDraft) {
        val lineIndex = _state.value.createMedicineLineIndex ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCreatingFromBill = true) }
            runCatching { createMedicineFromBillDraft(draft) }
                .onSuccess { medicineId ->
                    val parsed = _state.value.scannedBillReview?.lines?.getOrNull(lineIndex)?.parsed
                    if (parsed != null) {
                        runCatching {
                            saveOcrMedicineAlias(parsed.description, medicineId, draft.brandName)
                            recordOcrManualCorrection()
                        }
                    }
                    _state.update {
                        it.copy(
                            isCreatingFromBill = false,
                            showCreateMedicineDialog = false,
                            createMedicineDraft = null,
                            createMedicineLineIndex = null,
                            resolvedMedicineOverrides = it.resolvedMedicineOverrides + (lineIndex to medicineId),
                            selectedBillLineIndexes = it.selectedBillLineIndexes + lineIndex,
                            successMessage = "Medicine created — included in import",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isCreatingFromBill = false, errorMessage = e.message ?: "Could not create medicine")
                    }
                }
        }
    }

    fun openExistingPurchase(purchaseId: Long) {
        val invoice = _state.value.scannedBillReview?.duplicateBill?.internalInvoiceNumber ?: return
        viewModelScope.launch {
            _events.emit(PurchaseEvent.OpenExistingPurchase(invoice))
            dismissBillReview()
        }
    }

    fun toggleBillLine(index: Int) = _state.update { state ->
        val updated = state.selectedBillLineIndexes.toMutableSet()
        if (index in updated) updated.remove(index) else updated.add(index)
        state.copy(selectedBillLineIndexes = updated)
    }

    fun applyScannedBill() {
        val review = _state.value.scannedBillReview ?: return
        val selected = _state.value.selectedBillLineIndexes
        if (selected.isEmpty()) return
        val currentCart = _state.value.cart
        val overrides = _state.value.resolvedMedicineOverrides
        val mrpChoices = _state.value.mrpUpdateChoices
        val hasControlled = review.lines.any { it.isControlled && it.status != PurchaseBillMatchStatus.FREE_ITEM }
        if (hasControlled && !_state.value.controlledVerificationAcknowledged) {
            _state.update { it.copy(errorMessage = "Verify batch, expiry & license for controlled medicines before import") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isApplyingBill = true, errorMessage = null) }
            runCatching {
                val newItems = mutableListOf<PurchaseCartItem>()
                var lastMedicineId: Long? = null
                var lastMatch = review.lines.firstOrNull()?.match
                var skipped = 0

                review.lines.forEachIndexed { index, line ->
                    if (index !in selected) return@forEachIndexed
                    val parsed = line.parsed
                    val expiry = parsed.expiryDateMillis()
                        ?: throw IllegalStateException("Invalid expiry for ${parsed.description}")

                    val overrideId = overrides[index]
                    val medicineId = overrideId ?: when {
                        line.status == PurchaseBillMatchStatus.FREE_ITEM -> {
                            lastMedicineId ?: resolveMedicineForPurchase(lastMatch, parsed.description)
                        }
                        else -> resolveMedicineForPurchase(line.match, parsed.description)
                    }

                    if (medicineId == null) {
                        skipped++
                        return@forEachIndexed
                    }

                    if (line.mrpChanged && mrpChoices[index] == MrpUpdateChoice.UPDATE) {
                        updateMedicineMrpFromBill(medicineId, parsed.mrp)
                    }

                    if (!line.parsed.isFreeItem) {
                        lastMedicineId = medicineId
                        lastMatch = line.match
                    }

                    val displayName = line.match?.brandName ?: parsed.description
                    newItems.add(
                        PurchaseCartItem(
                            medicineId = medicineId,
                            medicineName = displayName,
                            batchNumber = parsed.batchNumber,
                            expiryDate = expiry,
                            quantity = parsed.quantity,
                            unitPrice = parsed.unitPrice,
                            sellingPrice = parsed.mrp,
                            mrp = parsed.mrp,
                        ),
                    )
                }

                if (newItems.isEmpty()) {
                    throw IllegalStateException("No items could be matched — create medicines or link manually")
                }
                val controlledImported = review.lines.withIndex()
                    .count { (index, line) -> index in selected && line.isControlled }
                mergePurchaseCart(currentCart, newItems) to (skipped to controlledImported)
            }.onSuccess { (mergedCart, skipInfo) ->
                val (skipped, controlledImported) = skipInfo
                _state.update {
                    it.copy(
                        cart = mergedCart,
                        isApplyingBill = false,
                        scannedBillReview = null,
                        selectedBillLineIndexes = emptySet(),
                        duplicateBillDismissed = false,
                        resolvedMedicineOverrides = emptyMap(),
                        mrpUpdateChoices = emptyMap(),
                        controlledVerificationAcknowledged = false,
                        successMessage = buildString {
                            append("Imported to cart — review & tap Save Purchase when ready")
                            if (skipped > 0) append(" ($skipped skipped)")
                            if (controlledImported > 0) append(" · $controlledImported controlled — verify license")
                        },
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isApplyingBill = false, errorMessage = e.message ?: "Could not apply bill")
                }
            }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        _state.update { it.copy(showBarcodeScanner = false) }
        viewModelScope.launch {
            runCatching { lookupBarcode(barcode) }
                .onSuccess { medicine ->
                    if (medicine != null) {
                        selectMedicineForEntry(medicine)
                    } else {
                        _state.update { it.copy(errorMessage = "Medicine not found for barcode") }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message ?: "Barcode lookup failed") }
                }
        }
    }

    fun selectMedicineForEntry(medicine: com.medipro.manager.domain.model.Medicine) {
        if (_state.value.selectedSupplierId == null) {
            _state.update { it.copy(errorMessage = "Select supplier first") }
            return
        }
        val defaultExpiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time)
        _state.update {
            it.copy(
                pendingMedicine = medicine,
                draftBatchNumber = "",
                draftExpiryDate = defaultExpiry,
                draftQuantity = "1",
                draftCostPrice = if (medicine.purchasePrice > 0) medicine.purchasePrice.toString() else "",
                draftSellingPrice = if (medicine.sellingPrice > 0) medicine.sellingPrice.toString() else "",
                draftMrp = if (medicine.mrp > 0) medicine.mrp.toString() else "",
                searchQuery = medicine.brandName,
                searchResults = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun clearPendingMedicine() {
        _state.update {
            it.copy(pendingMedicine = null, searchQuery = "", errorMessage = null)
        }
    }

    fun onDraftBatchChange(value: String) = _state.update { it.copy(draftBatchNumber = value) }
    fun onDraftExpiryChange(value: String) = _state.update { it.copy(draftExpiryDate = value) }
    fun onDraftQuantityChange(value: String) = _state.update { it.copy(draftQuantity = value) }
    fun onDraftCostPriceChange(value: String) = _state.update { it.copy(draftCostPrice = value) }
    fun onDraftSellingPriceChange(value: String) = _state.update { it.copy(draftSellingPrice = value) }
    fun onDraftMrpChange(value: String) = _state.update { it.copy(draftMrp = value) }

    fun confirmAddItem() {
        val current = _state.value
        val medicine = current.pendingMedicine ?: return
        val batchNumber = current.draftBatchNumber.trim()
        val quantity = current.draftQuantity.toIntOrNull() ?: 0
        val costPrice = current.draftCostPrice.toDoubleOrNull() ?: 0.0
        val sellingPrice = current.draftSellingPrice.toDoubleOrNull() ?: 0.0
        val mrp = current.draftMrp.toDoubleOrNull() ?: 0.0
        val expiry = parseExpiryDate(current.draftExpiryDate)

        when {
            batchNumber.isBlank() -> _state.update { it.copy(errorMessage = "Batch number required") }
            quantity <= 0 -> _state.update { it.copy(errorMessage = "Quantity must be greater than 0") }
            costPrice <= 0 -> _state.update { it.copy(errorMessage = "Cost price required") }
            expiry == null -> _state.update { it.copy(errorMessage = "Invalid expiry date (yyyy-MM-dd)") }
            else -> {
                val item = PurchaseCartItem(
                    medicineId = medicine.id,
                    medicineName = medicine.brandName,
                    batchNumber = batchNumber,
                    expiryDate = expiry,
                    quantity = quantity,
                    unitPrice = costPrice,
                    sellingPrice = sellingPrice,
                    mrp = mrp,
                    vatPercent = medicine.vatPercent,
                )
                _state.update {
                    it.copy(
                        cart = it.cart + item,
                        pendingMedicine = null,
                        searchQuery = "",
                        errorMessage = null,
                        successMessage = "Added ${medicine.brandName} — search next medicine",
                    )
                }
            }
        }
    }

    fun removeFromCart(index: Int) {
        _state.update { state ->
            state.copy(cart = state.cart.filterIndexed { i, _ -> i != index })
        }
    }

    fun onBillDiscountChange(value: Double) {
        _state.update { it.copy(billDiscount = value.coerceAtLeast(0.0)) }
    }

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

    fun onSupplierSelected(supplierId: Long?) = _state.update { it.copy(selectedSupplierId = supplierId) }

    fun completePurchase() {
        val current = _state.value
        if (current.cart.isEmpty()) {
            _state.update { it.copy(errorMessage = "Add at least one item") }
            return
        }
        if (current.selectedSupplierId == null) {
            _state.update { it.copy(errorMessage = "Select a supplier") }
            return
        }
        if (current.purchaseDate.isBlank() || parseExpiryDate(current.purchaseDate) == null) {
            _state.update { it.copy(errorMessage = "Enter valid invoice date (yyyy-MM-dd)") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, errorMessage = null) }
            runCatching {
                val invoiceNumber = generateInvoiceNumber()
                val paid = when (current.paymentMethod) {
                    PaymentMethod.CREDIT -> 0.0
                    else -> current.paidAmount.toDoubleOrNull() ?: current.totalAmount
                }
                val isCredit = current.paymentMethod == PaymentMethod.CREDIT ||
                    (current.paymentMethod == PaymentMethod.MIXED && paid < current.totalAmount)

                if (isCredit && paid >= current.totalAmount) {
                    throw IllegalStateException("Credit purchase requires unpaid balance")
                }
                if (paid > current.totalAmount) {
                    throw IllegalStateException("Paid amount exceeds total")
                }

                val paymentStatus = when {
                    paid >= current.totalAmount -> "PAID"
                    paid <= 0.0 -> "PENDING"
                    else -> "PARTIAL"
                }

                val purchase = Purchase(
                    supplierId = current.selectedSupplierId,
                    supplierName = current.suppliers.find { it.id == current.selectedSupplierId }?.name,
                    invoiceNumber = invoiceNumber,
                    purchaseDate = parseExpiryDate(current.purchaseDate) ?: System.currentTimeMillis(),
                    subtotal = current.subtotal,
                    discount = current.billDiscount,
                    vatAmount = current.vatTotal,
                    totalAmount = current.totalAmount,
                    paidAmount = paid,
                    paymentStatus = paymentStatus,
                    paymentMethod = current.paymentMethod.name,
                    notes = current.supplierBillNumber.trim().takeIf { it.isNotBlank() }
                        ?.let { bill -> "Supplier Bill: $bill" },
                    items = current.cart.map { it.toPurchaseItem() },
                )
                createPurchase(purchase)
                invoiceNumber
            }.onSuccess { invoiceNumber ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                _state.update {
                    it.copy(
                        isProcessing = false,
                        cart = emptyList(),
                        billDiscount = 0.0,
                        paidAmount = "",
                        pendingMedicine = null,
                        supplierBillNumber = "",
                        purchaseDate = today,
                        successMessage = "Purchase saved: $invoiceNumber",
                    )
                }
                _events.emit(PurchaseEvent.PurchaseCompleted(invoiceNumber))
            }.onFailure { e ->
                _state.update {
                    it.copy(isProcessing = false, errorMessage = e.message ?: "Purchase failed")
                }
                _events.emit(PurchaseEvent.ShowError(e.message ?: "Purchase failed"))
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }

    fun startNewPurchase() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        _state.update {
            it.copy(
                cart = emptyList(),
                billDiscount = 0.0,
                paidAmount = "",
                searchQuery = "",
                searchResults = emptyList(),
                pendingMedicine = null,
                supplierBillNumber = "",
                purchaseDate = today,
                paymentMethod = PaymentMethod.CASH,
                errorMessage = null,
                successMessage = "New purchase — select supplier & invoice details",
            )
        }
    }

    private fun parseExpiryDate(input: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(input.trim())?.time
    }.getOrNull()
}
