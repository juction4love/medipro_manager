package com.medipro.manager.feature.sales.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.InvoiceShareActions
import com.medipro.manager.core.common.InvoiceShareTarget
import com.medipro.manager.core.designsystem.component.MediProScreenWithFab
import com.medipro.manager.core.designsystem.navigation.MediProGlobalSearchIcon
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.feature.scanner.presentation.BarcodeScannerDialog
import com.medipro.manager.domain.model.PaymentMethod
import com.medipro.manager.domain.model.PosCartItem
import com.medipro.manager.domain.model.PosSearchResult
import com.medipro.manager.domain.model.PosSearchSource
import com.medipro.manager.domain.model.StockBatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SalesScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGlobalSearch: (() -> Unit)? = null,
    onOpenHistory: () -> Unit = {},
    onOpenReturn: (Long) -> Unit = {},
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SalesEvent.ShowError -> snackbar.showSnackbar(event.message)
                is SalesEvent.SaleCompleted -> snackbar.showSnackbar("Sale saved: ${event.invoiceNumber}")
                SalesEvent.FocusSearch -> searchFocus.requestFocus()
                is SalesEvent.OpenReturn -> onOpenReturn(event.saleId)
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    LaunchedEffect(Unit) {
        searchFocus.requestFocus()
    }

    MediProScreenWithFab(
        stickyBarVisible = state.cart.isNotEmpty(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startNewSale()
                    searchFocus.requestFocus()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Sale") },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text("Pharmacy Counter") },
                    navigationIcon = {
                        MediProTopBarNavigationIcon(
                            onBack = onOpenDrawer?.let { null } ?: onBack,
                            onOpenDrawer = onOpenDrawer,
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleReturnLookup(true) }) {
                            Icon(Icons.Default.Undo, contentDescription = "Return")
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, contentDescription = "Sales History")
                        }
                        state.lastInvoicePath?.let { path ->
                            IconButton(onClick = {
                                InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.SHARE_SHEET)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share last invoice")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                PosSearchHeader(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearchSubmit = viewModel::onSearchSubmit,
                    searchFocus = searchFocus,
                    onScanClick = { viewModel.toggleBarcodeScanner(true) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.searchDurationMs?.let { ms ->
                        item {
                            val slow = ms > PosPerformanceTargets.SEARCH_MS
                            Text(
                                "${state.searchResults.size} results · ${ms}ms" +
                                    if (slow) " (slow)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slow) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    state.didYouMean?.let { suggestion ->
                        item {
                            Text(
                                "Did you mean: $suggestion?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (state.searchResults.isNotEmpty()) {
                        items(state.searchResults, key = { it.key }) { result ->
                            SearchResultItem(
                                result = result,
                                onSelect = { viewModel.selectSearchResult(result) },
                                onAdd = { viewModel.addSearchResultToCart(result) },
                            )
                        }
                    } else if (state.isSearching) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Searching catalog…")
                            }
                        }
                    } else if (state.searchQuery.length >= 2) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("No medicines found", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    state.selectedResult?.let { selected ->
                        item {
                            GenericSuggestionPanel(
                                result = selected,
                                alternatives = state.alternativeBrands,
                                onAddAlternative = viewModel::addSearchResultToCart,
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                CounterCartPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f),
                    state = state,
                    onClearCart = viewModel::clearCart,
                    onIncrease = { item ->
                        viewModel.updateCartQuantity(item.batchId, item.medicineId, item.quantity + 1)
                    },
                    onDecrease = { item ->
                        viewModel.updateCartQuantity(item.batchId, item.medicineId, item.quantity - 1)
                    },
                    onRemove = { item ->
                        viewModel.removeFromCart(item.batchId, item.medicineId)
                    },
                    onDiscountChange = { item, discount ->
                        viewModel.updateLineDiscount(item.batchId, item.medicineId, discount)
                    },
                    onPaymentMethodChange = viewModel::onPaymentMethodChange,
                    onCustomerSelected = viewModel::onCustomerSelected,
                    onPaidAmountChange = viewModel::onPaidAmountChange,
                    onPrescriptionNumberChange = viewModel::onPrescriptionNumberChange,
                    onDoctorNameChange = viewModel::onDoctorNameChange,
                    onPatientNameChange = viewModel::onPatientNameChange,
                    onCompleteSale = viewModel::completeSale,
                )
            }
        }
    }

    if (state.showReturnLookup) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleReturnLookup(false) },
            title = { Text("Return Sale") },
            text = {
                OutlinedTextField(
                    value = state.returnInvoiceQuery,
                    onValueChange = viewModel::onReturnInvoiceQueryChange,
                    label = { Text("Invoice number") },
                    placeholder = { Text("Scan or type invoice #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = viewModel::lookupReturnInvoice) { Text("Find Invoice") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleReturnLookup(false) }) { Text("Cancel") }
            },
        )
    }

    if (state.showCheckout && state.requiresPrescriptionDetails) {
        CheckoutBottomSheet(
            state = state,
            onDismiss = { viewModel.toggleCheckout(false) },
            onBillDiscountChange = viewModel::onBillDiscountChange,
            onPaymentMethodChange = viewModel::onPaymentMethodChange,
            onPaidAmountChange = viewModel::onPaidAmountChange,
            onPrescriptionNumberChange = viewModel::onPrescriptionNumberChange,
            onDoctorNameChange = viewModel::onDoctorNameChange,
            onPatientNameChange = viewModel::onPatientNameChange,
            onCustomerSelected = viewModel::onCustomerSelected,
            onConfirm = viewModel::completeSale,
        )
    }

    state.completedReceipt?.let { receipt ->
        SaleCompletedDialog(
            receipt = receipt,
            onPrint = {
                viewModel.printCompletedReceipt { result ->
                    InvoiceShareActions.notifyThermalResult(context, result)
                }
            },
            onSavePdf = {
                InvoiceShareActions.notifySaved(context, receipt.pdfPath)
            },
            onWhatsApp = {
                InvoiceShareActions.sharePdf(context, receipt.pdfPath, InvoiceShareTarget.WHATSAPP)
            },
            onShare = {
                InvoiceShareActions.sharePdf(context, receipt.pdfPath, InvoiceShareTarget.SHARE_SHEET)
            },
            onDismiss = viewModel::dismissCompletedReceipt,
        )
    }

    if (state.showBarcodeScanner) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.toggleBarcodeScanner(false) },
            onBarcodeScanned = viewModel::onBarcodeScanned,
        )
    }

    if (state.showBatchPicker && state.batchPickerMedicine != null) {
        BatchPickerDialog(
            medicineName = state.batchPickerMedicine!!.brandName,
            batches = state.availableBatches,
            selectedBatchId = state.selectedBatchId,
            onSelectBatch = viewModel::onBatchPickerSelect,
            onConfirm = viewModel::confirmBatchSelection,
            onDismiss = viewModel::dismissBatchPicker,
            onUseFefo = viewModel::useFefoForBatchPicker,
        )
    }
}

@Composable
private fun BatchPickerDialog(
    medicineName: String,
    batches: List<StockBatch>,
    selectedBatchId: Long?,
    onSelectBatch: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onUseFefo: () -> Unit,
) {
    val expiryFmt = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Batch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    medicineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Batch No", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Expiry", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Stock", Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                batches.forEach { batch ->
                    val selected = batch.batchId == selectedBatchId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBatch(batch.batchId) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectBatch(batch.batchId) },
                        )
                        Text(batch.batchNumber, Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            expiryFmt.format(Date(batch.expiryDate)),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            batch.availableQuantity.toString(),
                            Modifier.weight(0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUseFefo) { Text("Auto FEFO") }
                Button(onClick = onConfirm, enabled = selectedBatchId != null) { Text("Select") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchResultItem(
    result: PosSearchResult,
    onSelect: () -> Unit,
    onAdd: () -> Unit,
) {
    val inStock = result.inStock
    var showMenu by remember { mutableStateOf(false) }
    val expiryFmt = remember { SimpleDateFormat("MM/yyyy", Locale.getDefault()) }
    Box {
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("View details") },
                onClick = { showMenu = false; onSelect() },
            )
            DropdownMenuItem(
                text = { Text("Stock: ${result.stockQuantity}") },
                onClick = { showMenu = false },
                enabled = false,
            )
            result.batchNumber?.let { batch ->
                DropdownMenuItem(
                    text = { Text("Batch: $batch") },
                    onClick = { showMenu = false },
                    enabled = false,
                )
            }
            DropdownMenuItem(
                text = { Text("Price: ${FormatUtils.formatCurrency(result.sellingPrice)}") },
                onClick = { showMenu = false },
                enabled = false,
            )
            if (inStock) {
                DropdownMenuItem(
                    text = { Text("Add to cart") },
                    onClick = { showMenu = false; onAdd() },
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect,
                    onLongClick = { showMenu = true },
                ),
            colors = when {
                result.isExpiredStock -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                !inStock -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                else -> CardDefaults.cardColors()
            },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val genericLine = result.genericStrengthLabel.ifBlank { result.composition }
                    if (genericLine.isNotBlank()) {
                        Text(genericLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        result.brandName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Stock ${result.stockQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (result.mrp > 0) {
                            Text(
                                "MRP ${FormatUtils.formatCurrency(result.mrp)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else if (result.sellingPrice > 0) {
                            Text(
                                FormatUtils.formatCurrency(result.sellingPrice),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    result.batchNumber?.let { batch ->
                        Text("Batch $batch", style = MaterialTheme.typography.bodySmall)
                        result.expiryDate?.let { exp ->
                            Text(
                                "EXP ${expiryFmt.format(Date(exp))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    when {
                        result.isExpiredStock -> {
                            Text(
                                "🔴 Expired",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        result.isLowStock -> {
                            Text(
                                "🟠 Low Stock",
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    result.rxBadge?.let { badge ->
                        AssistChip(onClick = {}, enabled = false, label = { Text(badge) })
                    }
                    if (!inStock && result.source == PosSearchSource.CATALOG) {
                        Text("Catalog only — not in stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (inStock && !result.isExpiredStock) {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenericSuggestionPanel(
    result: PosSearchResult,
    alternatives: List<PosSearchResult>,
    onAddAlternative: (PosSearchResult) -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text("Generic: ${result.composition.ifBlank { result.genericName }}", style = MaterialTheme.typography.titleSmall)
            if (alternatives.isNotEmpty()) {
                Text("Alternative brands", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    alternatives.take(6).forEach { alt ->
                        AssistChip(
                            onClick = { onAddAlternative(alt) },
                            label = { Text(alt.brandName) },
                            enabled = alt.inStock,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: PosCartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onDiscountChange: (Double) -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("MM/yyyy", Locale.getDefault()) }
    var showDiscount by remember(item.batchId) { mutableStateOf(item.discount > 0) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var discountInput by remember(item.discount) {
        mutableStateOf(if (item.discount > 0) item.discount.toString() else "")
    }

    if (showBatchDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDialog = false },
            title = { Text("Batch Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.medicineName, fontWeight = FontWeight.SemiBold)
                    Text("Batch: ${item.batchNumber}")
                    Text("Expiry: ${dateFmt.format(Date(item.expiryDate))}")
                    Text("Unit price: ${FormatUtils.formatCurrency(item.unitPrice)}")
                    if (item.mrp > 0) Text("MRP: ${FormatUtils.formatCurrency(item.mrp)}")
                    Text("Line total: ${FormatUtils.formatCurrency(item.lineTotal)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchDialog = false }) { Text("OK") }
            },
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.medicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(24.dp))
                }
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(24.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showBatchDialog = true },
                    modifier = Modifier.weight(1f),
                ) { Text("Batch", maxLines = 1) }
                androidx.compose.material3.OutlinedButton(
                    onClick = { showDiscount = !showDiscount },
                    modifier = Modifier.weight(1f),
                ) { Text("Discount", maxLines = 1) }
                androidx.compose.material3.OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                ) { Text("Delete", maxLines = 1) }
            }

            if (showDiscount) {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Line discount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            if (item.requiresPrescription || item.scheduleCategory in setOf("H", "H1", "X")) {
                Text("Rx Required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CounterCartPanel(
    state: SalesState,
    onClearCart: () -> Unit,
    onIncrease: (PosCartItem) -> Unit,
    onDecrease: (PosCartItem) -> Unit,
    onRemove: (PosCartItem) -> Unit,
    onDiscountChange: (PosCartItem, Double) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCustomerSelected: (Long?) -> Unit,
    onPaidAmountChange: (String) -> Unit,
    onPrescriptionNumberChange: (String) -> Unit,
    onDoctorNameChange: (String) -> Unit,
    onPatientNameChange: (String) -> Unit,
    onCompleteSale: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cart = state.cart
    var customerExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = state.customers.find { it.id == state.selectedCustomerId }

    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Current Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClearCart) { Text("Clear") }
            }
        }

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Search or scan — items appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(cart, key = { "${it.medicineId}-${it.batchId}" }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { onIncrease(item) },
                        onDecrease = { onDecrease(item) },
                        onRemove = { onRemove(item) },
                        onDiscountChange = { onDiscountChange(item, it) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Walk-in Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = customerExpanded, onDismissRequest = { customerExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Walk-in") },
                        onClick = { onCustomerSelected(null); customerExpanded = false },
                    )
                    state.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name) },
                            onClick = { onCustomerSelected(customer.id); customerExpanded = false },
                        )
                    }
                }
            }

            Text("Payment", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                listOf(
                    PaymentMethod.CASH,
                    PaymentMethod.CARD,
                    PaymentMethod.ESEWA,
                    PaymentMethod.KHALTI,
                    PaymentMethod.IME_PAY,
                    PaymentMethod.CREDIT,
                ).forEach { method ->
                    FilterChip(
                        selected = state.paymentMethod == method,
                        onClick = { onPaymentMethodChange(method) },
                        label = { Text(method.label, maxLines = 1) },
                    )
                }
            }

            if (state.paymentMethod == PaymentMethod.CREDIT || state.paymentMethod == PaymentMethod.MIXED) {
                OutlinedTextField(
                    value = state.paidAmount,
                    onValueChange = onPaidAmountChange,
                    label = { Text("Paid Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }

            if (state.requiresPrescriptionDetails) {
                OutlinedTextField(
                    value = state.prescriptionNumber,
                    onValueChange = onPrescriptionNumberChange,
                    label = { Text("Prescription #") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.doctorName,
                    onValueChange = onDoctorNameChange,
                    label = { Text("Doctor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.patientName,
                    onValueChange = onPatientNameChange,
                    label = { Text("Patient") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Text(
                FormatUtils.formatCurrency(state.totalAmount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Button(
                onClick = onCompleteSale,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (state.isProcessing) "Processing…" else "Complete Sale",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PosSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    searchFocus: FocusRequester,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
            label = { Text("Search medicine…") },
            placeholder = { Text("Brand, generic, barcode") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
        )

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                "Scan Barcode",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StickyCheckoutBar(
    itemCount: Int,
    totalAmount: Double,
    onCheckout: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$itemCount Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = FormatUtils.formatCurrency(totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = onCheckout,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Checkout", fontWeight = FontWeight.SemiBold)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

private val CHECKOUT_PAYMENT_METHODS = listOf(
    PaymentMethod.CASH,
    PaymentMethod.CARD,
    PaymentMethod.ESEWA,
    PaymentMethod.KHALTI,
    PaymentMethod.IME_PAY,
    PaymentMethod.CREDIT,
    PaymentMethod.MIXED,
)

@Composable
private fun PaymentMethodOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (selected) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null)
        Text(value, fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CheckoutBottomSheet(
    state: SalesState,
    onDismiss: () -> Unit,
    onBillDiscountChange: (Double) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onPaidAmountChange: (String) -> Unit,
    onPrescriptionNumberChange: (String) -> Unit,
    onDoctorNameChange: (String) -> Unit,
    onPatientNameChange: (String) -> Unit,
    onCustomerSelected: (Long?) -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var discountInput by remember(state.billDiscount) {
        mutableStateOf(if (state.billDiscount > 0) state.billDiscount.toString() else "")
    }
    var customerExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = state.customers.find { it.id == state.selectedCustomerId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Checkout", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Total: ${FormatUtils.formatCurrency(state.totalAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text("Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            CHECKOUT_PAYMENT_METHODS.forEach { method ->
                PaymentMethodOption(
                    label = method.label,
                    selected = state.paymentMethod == method,
                    onClick = { onPaymentMethodChange(method) },
                )
            }

            HorizontalDivider()

            TotalRow("Subtotal", FormatUtils.formatCurrency(state.subtotal))
            TotalRow("VAT", FormatUtils.formatCurrency(state.vatTotal))
            if (state.totalMrpSavings > 0) {
                TotalRow("MRP Savings", FormatUtils.formatCurrency(state.totalMrpSavings))
            }

            OutlinedTextField(
                value = discountInput,
                onValueChange = { discountInput = it; onBillDiscountChange(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Bill Discount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            ExposedDropdownMenuBox(expanded = customerExpanded, onExpandedChange = { customerExpanded = it }) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Walk-in Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                DropdownMenu(expanded = customerExpanded, onDismissRequest = { customerExpanded = false }) {
                    DropdownMenuItem(text = { Text("Walk-in") }, onClick = { onCustomerSelected(null); customerExpanded = false })
                    state.customers.forEach { customer ->
                        DropdownMenuItem(text = { Text(customer.name) }, onClick = { onCustomerSelected(customer.id); customerExpanded = false })
                    }
                }
            }

            if (state.paymentMethod == PaymentMethod.MIXED || state.paymentMethod == PaymentMethod.CREDIT) {
                OutlinedTextField(
                    value = state.paidAmount,
                    onValueChange = onPaidAmountChange,
                    label = { Text("Paid Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.requiresPrescriptionDetails) {
                Text("Prescription required (Schedule H / Rx)", color = MaterialTheme.colorScheme.error)
                OutlinedTextField(value = state.prescriptionNumber, onValueChange = onPrescriptionNumberChange, label = { Text("Prescription Number *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.doctorName, onValueChange = onDoctorNameChange, label = { Text("Doctor Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.patientName, onValueChange = onPatientNameChange, label = { Text("Patient Name *") }, modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = onConfirm,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (state.isProcessing) "Processing…" else "Complete Sale")
            }
        }
    }
}
