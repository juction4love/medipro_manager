package com.medipro.manager.feature.purchase.presentation

import com.medipro.manager.domain.model.BillScanProgress
import com.medipro.manager.domain.model.MrpUpdateChoice
import com.medipro.manager.domain.model.OcrMedicineDraft
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.PurchaseCartItem
import com.medipro.manager.domain.model.ScannedPurchaseBill
import com.medipro.manager.domain.model.Supplier

data class PurchaseState(
    val searchQuery: String = "",
    val searchResults: List<com.medipro.manager.domain.model.Medicine> = emptyList(),
    val cart: List<PurchaseCartItem> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: Long? = null,
    val supplierBillNumber: String = "",
    val purchaseDate: String = "",
    val billDiscount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paidAmount: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingMedicine: com.medipro.manager.domain.model.Medicine? = null,
    val draftBatchNumber: String = "",
    val draftExpiryDate: String = "",
    val draftQuantity: String = "1",
    val draftCostPrice: String = "",
    val draftSellingPrice: String = "",
    val draftMrp: String = "",
    val showBarcodeScanner: Boolean = false,
    val showBillScanner: Boolean = false,
    val isScanningBill: Boolean = false,
    val billScanProgress: BillScanProgress? = null,
    val scannedBillReview: ScannedPurchaseBill? = null,
    val selectedBillLineIndexes: Set<Int> = emptySet(),
    val isApplyingBill: Boolean = false,
    val duplicateBillDismissed: Boolean = false,
    val showCreateSupplierDialog: Boolean = false,
    val showCreateMedicineDialog: Boolean = false,
    val createMedicineDraft: OcrMedicineDraft? = null,
    val createMedicineLineIndex: Int? = null,
    val isCreatingFromBill: Boolean = false,
    val resolvedMedicineOverrides: Map<Int, Long> = emptyMap(),
    val mrpUpdateChoices: Map<Int, MrpUpdateChoice> = emptyMap(),
    val showOcrFailureDialog: Boolean = false,
    val lastScanImageUris: List<String> = emptyList(),
    val lastScanOcrText: String? = null,
    val lastScanBill: ScannedPurchaseBill? = null,
    val showManualMatchLineIndex: Int? = null,
    val manualMatchQuery: String = "",
    val manualMatchResults: List<com.medipro.manager.domain.model.Medicine> = emptyList(),
    val controlledVerificationAcknowledged: Boolean = false,
) {
    val subtotal: Double get() = cart.sumOf { it.lineSubtotal }
    val vatTotal: Double get() = cart.sumOf { it.vatAmount }
    val totalAmount: Double get() = (subtotal + vatTotal) - billDiscount
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val invoiceHeaderReady: Boolean get() = selectedSupplierId != null && purchaseDate.isNotBlank()
}

sealed interface PurchaseEvent {
    data class ShowError(val message: String) : PurchaseEvent
    data class PurchaseCompleted(val invoiceNumber: String) : PurchaseEvent
    data class OpenExistingPurchase(val invoiceNumber: String) : PurchaseEvent
}
