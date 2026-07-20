package com.medipro.manager.feature.sales.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.InvoiceShareActions
import com.medipro.manager.core.common.InvoiceShareTarget
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.model.SaleItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleInvoiceScreen(
    invoiceNumber: String,
    onBack: () -> Unit,
    onReturnSale: (Long) -> Unit,
    viewModel: SaleInvoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(invoiceNumber) {
        viewModel.load(invoiceNumber)
    }

    androidx.compose.runtime.LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.sale == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Invoice not found",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = invoiceNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                SaleInvoiceContent(
                    sale = state.sale!!,
                    pdfPath = state.pdfPath,
                    isGeneratingPdf = state.isGeneratingPdf,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    onPrint = {
                        viewModel.printThermal { result ->
                            InvoiceShareActions.notifyThermalResult(context, result)
                        }
                    },
                    onPdf = {
                        state.pdfPath?.let { InvoiceShareActions.notifySaved(context, it) }
                            ?: viewModel.ensureInvoicePdf()
                    },
                    onWhatsApp = {
                        state.pdfPath?.let {
                            InvoiceShareActions.sharePdf(context, it, InvoiceShareTarget.WHATSAPP)
                        }
                    },
                    onShare = {
                        state.pdfPath?.let {
                            InvoiceShareActions.sharePdf(context, it, InvoiceShareTarget.SHARE_SHEET)
                        }
                    },
                    onReturnSale = onReturnSale,
                    isCancelled = state.sale!!.isCancelled,
                )
            }
        }
    }
}

@Composable
private fun SaleInvoiceContent(
    sale: Sale,
    pdfPath: String?,
    isGeneratingPdf: Boolean,
    modifier: Modifier = Modifier,
    onPrint: () -> Unit,
    onPdf: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit,
    onReturnSale: (Long) -> Unit,
    isCancelled: Boolean,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sale.invoiceNumber, style = MaterialTheme.typography.headlineSmall)
                Text(FormatUtils.formatDate(sale.saleDate), style = MaterialTheme.typography.bodyMedium)
                sale.customerName?.let {
                    Text("Customer: $it", style = MaterialTheme.typography.bodyLarge)
                }
                Text("Payment: ${sale.paymentMethod} (${sale.paymentStatus})")
                if (sale.printCount > 0) {
                    Text(
                        "Printed ${sale.printCount} time(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = FormatUtils.formatCurrency(sale.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Items", style = MaterialTheme.typography.titleMedium)
                sale.items.forEach { item ->
                    InvoiceLineItemRow(item)
                    HorizontalDivider()
                }
                TotalSummaryRow("Subtotal", sale.subtotal)
                TotalSummaryRow("Discount", sale.discount)
                TotalSummaryRow("VAT", sale.vatAmount)
                TotalSummaryRow("Total", sale.totalAmount, bold = true)
            }
        }

        InvoiceActionButtons(
            pdfPath = pdfPath,
            isGenerating = isGeneratingPdf,
            onPrint = onPrint,
            onPdf = onPdf,
            onWhatsApp = onWhatsApp,
            onShare = onShare,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onReturnSale(sale.id) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCancelled,
        ) {
            Text("↩ Process Return")
        }
        if (!isCancelled) {
            Text(
                "Posted invoices cannot be cancelled. Use Process Return to reverse stock, " +
                    "payments, and ledger entries with a full audit trail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InvoiceLineItemRow(item: SaleItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(item.medicineName, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Qty ${item.quantity} × ${FormatUtils.formatCurrency(item.unitPrice)}")
            Text(FormatUtils.formatCurrency(item.totalPrice))
        }
        val meta = buildList {
            if (item.batchNumber.isNotBlank()) add("Batch: ${item.batchNumber}")
            item.expiryDate?.let { exp ->
                val expStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(exp))
                add("Exp: $expStr")
            }
        }.joinToString(" • ")
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TotalSummaryRow(label: String, amount: Double, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else null)
        Text(FormatUtils.formatCurrency(amount), fontWeight = if (bold) FontWeight.Bold else null)
    }
}
