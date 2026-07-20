package com.medipro.manager.feature.accounting.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.medipro.manager.core.common.InvoiceShareActions
import com.medipro.manager.core.common.InvoiceShareTarget
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.Supplier

private val paymentMethods = listOf("CASH", "BANK", "CHEQUE")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupplierPaymentsScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: SupplierPaymentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SupplierPaymentsEvent.SharePdf ->
                    InvoiceShareActions.sharePdf(context, event.path, InvoiceShareTarget.SHARE_SHEET)
                is SupplierPaymentsEvent.PrintResult -> snackbar.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Payments") },
                navigationIcon = {
                    MediProTopBarNavigationIcon(
                        onBack = onOpenDrawer?.let { null } ?: onBack,
                        onOpenDrawer = onOpenDrawer,
                    )
                },
                actions = {
                    if (state.lastVoucher != null) {
                        IconButton(onClick = viewModel::printLastVoucher, enabled = !state.isPrinting) {
                            Icon(Icons.Default.Print, contentDescription = "Print voucher")
                        }
                        state.lastVoucherPdfPath?.let { path ->
                            IconButton(onClick = {
                                InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.SHARE_SHEET)
                            }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Share voucher PDF")
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = viewModel::savePayment,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (state.isSaving) "Saving…" else "Pay Supplier",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { padding ->
        if (state.suppliers.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No supplier dues outstanding", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Credit purchases will appear here when you owe suppliers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("Outstanding", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(state.suppliers) { supplier ->
                    SupplierDueRow(
                        supplier = supplier,
                        selected = supplier.id == state.selectedSupplierId,
                        onClick = { viewModel.selectSupplier(supplier.id) },
                    )
                }
                item {
                    Text("Pay Supplier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    state.selectedSupplier?.let { supplier ->
                        Text(
                            "Due: ${FormatUtils.formatCurrency(supplier.outstandingBalance)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Amount (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.forEach { method ->
                            FilterChip(
                                selected = state.paymentMethod == method,
                                onClick = { viewModel.onPaymentMethodChange(method) },
                                label = { Text(method) },
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierDueRow(
    supplier: Supplier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(supplier.name, fontWeight = FontWeight.SemiBold)
                supplier.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text(
                FormatUtils.formatCurrency(supplier.outstandingBalance),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
