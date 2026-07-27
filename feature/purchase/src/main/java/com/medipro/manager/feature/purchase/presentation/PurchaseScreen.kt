package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.designsystem.component.MediProScreenWithFab
import com.medipro.manager.core.designsystem.navigation.MediProGlobalSearchIcon
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.Medicine
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.PurchaseCartItem
import com.medipro.manager.feature.scanner.presentation.BarcodeScannerDialog
import com.medipro.manager.feature.scanner.presentation.PurchaseBillCaptureDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PurchaseScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGlobalSearch: (() -> Unit)? = null,
    onOpenHistory: () -> Unit = {},
    onOpenPurchaseInvoice: (String) -> Unit = {},
    onRequireSubscription: () -> Unit = {},
    viewModel: PurchaseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PurchaseEvent.ShowError -> snackbar.showSnackbar(event.message)
                is PurchaseEvent.PurchaseCompleted -> snackbar.showSnackbar("Purchase saved: ${event.invoiceNumber}")
                is PurchaseEvent.OpenExistingPurchase -> onOpenPurchaseInvoice(event.invoiceNumber)
                PurchaseEvent.RequirePremium -> onRequireSubscription()
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    MediProScreenWithFab(
        stickyBarVisible = state.cart.isNotEmpty(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startNewPurchase() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Bill") },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text("Supplier Invoice Entry") },
                    navigationIcon = {
                        MediProTopBarNavigationIcon(
                            onBack = onOpenDrawer?.let { null } ?: onBack,
                            onOpenDrawer = onOpenDrawer,
                        )
                    },
                    actions = {
                        onOpenGlobalSearch?.let { opener ->
                            MediProGlobalSearchIcon(onClick = opener)
                        }
                        IconButton(onClick = { viewModel.toggleBillScanner(true) }) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Scan supplier bill")
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, contentDescription = "Purchase History")
                        }
                    },
                )
            },
            bottomBar = {
                PurchaseSaveBar(
                    state = state,
                    onPaymentMethodChange = viewModel::onPaymentMethodChange,
                    onComplete = viewModel::completePurchase,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                PurchaseInvoiceHeader(
                    state = state,
                    onSupplierSelected = viewModel::onSupplierSelected,
                    onBillNumberChange = viewModel::onSupplierBillNumberChange,
                    onDateChange = viewModel::onPurchaseDateChange,
                    onScanBill = { viewModel.toggleBillScanner(true) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search medicine to add") },
                            placeholder = { Text("Brand / generic / barcode") },
                            singleLine = true,
                            enabled = state.invoiceHeaderReady,
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.toggleBarcodeScanner(true) },
                                    enabled = state.invoiceHeaderReady,
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                                }
                            },
                            supportingText = {
                                if (!state.invoiceHeaderReady) {
                                    Text("Complete supplier & date above first")
                                }
                            },
                        )
                    }

                    if (state.pendingMedicine != null) {
                        item {
                            InlinePurchaseEntryCard(
                                medicine = state.pendingMedicine!!,
                                batchNumber = state.draftBatchNumber,
                                expiryDate = state.draftExpiryDate,
                                quantity = state.draftQuantity,
                                costPrice = state.draftCostPrice,
                                sellingPrice = state.draftSellingPrice,
                                mrp = state.draftMrp,
                                onBatchChange = viewModel::onDraftBatchChange,
                                onExpiryChange = viewModel::onDraftExpiryChange,
                                onQuantityChange = viewModel::onDraftQuantityChange,
                                onCostChange = viewModel::onDraftCostPriceChange,
                                onSellingChange = viewModel::onDraftSellingPriceChange,
                                onMrpChange = viewModel::onDraftMrpChange,
                                onConfirm = viewModel::confirmAddItem,
                                onCancel = viewModel::clearPendingMedicine,
                            )
                        }
                    }

                    if (state.searchResults.isNotEmpty() && state.pendingMedicine == null) {
                        items(state.searchResults) { medicine ->
                            MedicineSearchRow(
                                medicine = medicine,
                                onClick = { viewModel.selectMedicineForEntry(medicine) },
                            )
                        }
                    }

                    if (state.cart.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Bill Items (${state.itemCount} units)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        itemsIndexed(state.cart) { index, item ->
                            PurchaseCartRow(
                                item = item,
                                onRemove = { viewModel.removeFromCart(index) },
                            )
                        }
                        item {
                            Text(
                                "Subtotal: ${FormatUtils.formatCurrency(state.subtotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (state.vatTotal > 0) {
                                Text(
                                    "VAT: ${FormatUtils.formatCurrency(state.vatTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Text(
                                "Total: ${FormatUtils.formatCurrency(state.totalAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else if (state.invoiceHeaderReady) {
                        item {
                            Text(
                                "Search medicine and tap Add to build the supplier bill",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.toggleBarcodeScanner(false) },
            onBarcodeScanned = viewModel::onBarcodeScanned,
        )
    }

    if (state.isScanningBill) {
        BillScanProgressOverlay(progress = state.billScanProgress)
    }

    if (state.showBillScanner && !state.isScanningBill) {
        PurchaseBillCaptureDialog(
            onDismiss = { viewModel.toggleBillScanner(false) },
            onComplete = viewModel::onBillCaptureComplete,
            isProcessing = false,
        )
    }

    if (state.showOcrFailureDialog) {
        OcrParseFailureDialog(
            imageUris = state.lastScanImageUris,
            ocrText = state.lastScanOcrText,
            onDismiss = viewModel::dismissOcrFailureDialog,
            onRetry = viewModel::retryBillScan,
            onImproveParser = viewModel::submitOcrParserFeedback,
            feedbackOptInHint = "Requires opt-in: Settings → Anonymous OCR feedback",
        )
    }

    state.scannedBillReview?.let { review ->
        PurchaseBillReviewSheet(
            review = review,
            selectedLineIndexes = state.selectedBillLineIndexes,
            isApplying = state.isApplyingBill,
            duplicateDismissed = state.duplicateBillDismissed,
            mrpUpdateChoices = state.mrpUpdateChoices,
            controlledVerificationAcknowledged = state.controlledVerificationAcknowledged,
            onToggleLine = viewModel::toggleBillLine,
            onApply = viewModel::applyScannedBill,
            onDismiss = viewModel::dismissBillReview,
            onCreateSupplier = viewModel::showCreateSupplierDialog,
            onCreateMedicine = viewModel::showCreateMedicineForLine,
            onManualMatch = viewModel::showManualMatchForLine,
            onMrpChoice = viewModel::setMrpUpdateChoice,
            onAcknowledgeControlled = viewModel::acknowledgeControlledVerification,
            onOpenExistingPurchase = viewModel::openExistingPurchase,
            onDismissDuplicate = viewModel::dismissDuplicateBillWarning,
        )
    }

    state.showManualMatchLineIndex?.let { lineIndex ->
        val line = state.scannedBillReview?.lines?.getOrNull(lineIndex)
        if (line != null) {
            ManualMatchBillLineDialog(
                ocrDescription = line.parsed.description,
                query = state.manualMatchQuery,
                results = state.manualMatchResults,
                onQueryChange = viewModel::onManualMatchQueryChange,
                onSelect = viewModel::manualMatchLine,
                onDismiss = viewModel::dismissManualMatchDialog,
            )
        }
    }

    if (state.showCreateSupplierDialog) {
        CreateSupplierFromBillDialog(
            supplierName = state.scannedBillReview?.supplierName.orEmpty(),
            isSaving = state.isCreatingFromBill,
            onDismiss = viewModel::dismissCreateSupplierDialog,
            onSave = viewModel::createSupplierFromScan,
        )
    }

    state.createMedicineDraft?.let { draft ->
        if (state.showCreateMedicineDialog) {
            CreateMedicineFromBillDialog(
                draft = draft,
                isSaving = state.isCreatingFromBill,
                onDismiss = viewModel::dismissCreateMedicineDialog,
                onSave = viewModel::saveMedicineFromBillDraft,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseWorkflowStrip(
    hasSupplier: Boolean,
    hasItems: Boolean,
) {
    val step = when {
        hasItems -> 3
        hasSupplier -> 2
        else -> 1
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(1 to "Supplier", 2 to "Add Items", 3 to "Payment").forEach { (n, label) ->
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(label) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    disabledContainerColor = if (n == step) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseInvoiceHeader(
    state: PurchaseState,
    onSupplierSelected: (Long?) -> Unit,
    onBillNumberChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onScanBill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var supplierExpanded by remember { mutableStateOf(false) }
    val selectedSupplier = state.suppliers.find { it.id == state.selectedSupplierId }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Supplier *") },
                        placeholder = { Text("Select supplier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                    )
                    DropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false },
                    ) {
                        state.suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    onSupplierSelected(supplier.id)
                                    supplierExpanded = false
                                },
                            )
                        }
                    }
                }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.supplierBillNumber,
                    onValueChange = onBillNumberChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Supplier Invoice #") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.purchaseDate,
                    onValueChange = onDateChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Invoice Date *") },
                    placeholder = { Text("yyyy-MM-dd") },
                    singleLine = true,
                )
            }

            Button(onClick = onScanBill, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null)
                Text("Scan Bill (OCR)", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlinePurchaseEntryCard(
    medicine: Medicine,
    batchNumber: String,
    expiryDate: String,
    quantity: String,
    costPrice: String,
    sellingPrice: String,
    mrp: String,
    onBatchChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onSellingChange: (String) -> Unit,
    onMrpChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(medicine.brandName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(medicine.genericName, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }

            OutlinedTextField(
                value = quantity,
                onValueChange = onQuantityChange,
                label = { Text("Qty *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = onCostChange,
                    label = { Text("Cost *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = mrp,
                    onValueChange = onMrpChange,
                    label = { Text("MRP") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = onBatchChange,
                    label = { Text("Batch *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = onExpiryChange,
                    label = { Text("Expiry *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            OutlinedTextField(
                value = sellingPrice,
                onValueChange = onSellingChange,
                label = { Text("Selling Price") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add to Bill", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PurchaseSaveBar(
    state: PurchaseState,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onComplete: () -> Unit,
) {
    if (state.cart.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.paymentMethod == PaymentMethod.CASH,
                onClick = { onPaymentMethodChange(PaymentMethod.CASH) },
                label = { Text("Cash") },
            )
            FilterChip(
                selected = state.paymentMethod == PaymentMethod.CREDIT,
                onClick = { onPaymentMethodChange(PaymentMethod.CREDIT) },
                label = { Text("Credit") },
            )
        }
        Button(
            onClick = onComplete,
            enabled = !state.isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Payments, contentDescription = null)
            Text(
                if (state.isProcessing) "Saving…" else "Save Purchase · ${FormatUtils.formatCurrency(state.totalAmount)}",
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MedicineSearchRow(medicine: Medicine, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medicine.brandName, style = MaterialTheme.typography.titleMedium)
                Text(medicine.genericName, style = MaterialTheme.typography.bodySmall)
                Text("Stock: ${medicine.stockQuantity}", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
private fun PurchaseCartRow(item: PurchaseCartItem, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.medicineName, style = MaterialTheme.typography.titleMedium)
                Text("Batch: ${item.batchNumber} · Exp: ${FormatUtils.formatDate(item.expiryDate)}")
                Text(
                    "Qty ${item.quantity} × ${FormatUtils.formatCurrency(item.unitPrice)} = ${FormatUtils.formatCurrency(item.lineTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
