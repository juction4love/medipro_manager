package com.medipro.manager.feature.accounting.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.InvoiceShareActions
import com.medipro.manager.core.common.InvoiceShareTarget
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.DayClosingPreview
import com.medipro.manager.domain.model.DayClosingReason
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayClosingScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: DayClosingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day Closing") },
                navigationIcon = {
                    MediProTopBarNavigationIcon(
                        onBack = onOpenDrawer?.let { null } ?: onBack,
                        onOpenDrawer = onOpenDrawer,
                    )
                },
                actions = {
                    state.pdfPath?.let { path ->
                        IconButton(onClick = {
                            InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.WHATSAPP)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp")
                        }
                        IconButton(onClick = {
                            InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.SHARE_SHEET)
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "Share PDF")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.preview?.isAlreadyClosed != true) {
                Button(
                    onClick = viewModel::closeDay,
                    enabled = state.canClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        if (state.isClosing) "Closing…" else "Close Day",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                BoxLoading(Modifier.fillMaxSize().padding(padding))
            }
            state.preview != null -> {
                DayClosingContent(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    preview = state.preview!!,
                    actualCashInput = state.actualCashInput,
                    remarks = state.remarks,
                    differenceReason = state.differenceReason,
                    difference = state.difference,
                    needsDifferenceReason = state.needsDifferenceReason,
                    isClosed = state.preview!!.isAlreadyClosed,
                    onActualCashChange = viewModel::onActualCashChange,
                    onRemarksChange = viewModel::onRemarksChange,
                    onDifferenceReasonChange = viewModel::onDifferenceReasonChange,
                )
            }
        }
    }
}

@Composable
private fun BoxLoading(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Loading day summary…", Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayClosingContent(
    preview: DayClosingPreview,
    actualCashInput: String,
    remarks: String,
    differenceReason: String?,
    difference: Double?,
    needsDifferenceReason: Boolean,
    isClosed: Boolean,
    onActualCashChange: (String) -> Unit,
    onRemarksChange: (String) -> Unit,
    onDifferenceReasonChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(preview.dateLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (isClosed) {
            Text(
                "Day already closed",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SectionCard("Sales Summary") {
            MetricRow("Sales Count", preview.salesCount.toString())
            MetricRow("Return Count", preview.returnCount.toString())
            MetricRow("Discount", FormatUtils.formatCurrency(preview.discountTotal))
            MetricRow("VAT", FormatUtils.formatCurrency(preview.vatTotal))
        }

        SectionCard("Payment Methods") {
            MetricRow("Cash Sales", FormatUtils.formatCurrency(preview.cashSales))
            MetricRow("Card", FormatUtils.formatCurrency(preview.cardSales))
            MetricRow("eSewa", FormatUtils.formatCurrency(preview.esewaSales))
            MetricRow("Khalti", FormatUtils.formatCurrency(preview.khaltiSales))
            MetricRow("IME Pay", FormatUtils.formatCurrency(preview.imeSales))
            MetricRow("Credit Sales", FormatUtils.formatCurrency(preview.creditSales))
        }

        SectionCard("Cash Reconciliation") {
            MetricRow("Opening Cash", FormatUtils.formatCurrency(preview.openingCash))
            MetricRow("+ Customer Receipts", FormatUtils.formatCurrency(preview.customerReceipts))
            MetricRow("- Supplier Payments", FormatUtils.formatCurrency(preview.supplierPayments))
            MetricRow("- Expenses", FormatUtils.formatCurrency(preview.expenses))
            MetricRow("- Returns", FormatUtils.formatCurrency(preview.returnsAmount))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            MetricRow(
                "Expected Cash",
                FormatUtils.formatCurrency(preview.expectedCash),
                bold = true,
                highlight = true,
            )
        }

        if (!isClosed) {
            OutlinedTextField(
                value = actualCashInput,
                onValueChange = onActualCashChange,
                label = { Text("Actual Cash (Rs.)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            difference?.let { diff ->
                MetricRow(
                    "Difference",
                    FormatUtils.formatCurrency(diff),
                    bold = true,
                    highlight = abs(diff) > 0.01,
                    error = abs(diff) > 0.01,
                )
            }
            if (needsDifferenceReason) {
                Text("Difference reason", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = differenceReason == DayClosingReason.SHORT_CASH,
                        onClick = { onDifferenceReasonChange(DayClosingReason.SHORT_CASH) },
                        label = { Text("Short Cash") },
                    )
                    FilterChip(
                        selected = differenceReason == DayClosingReason.EXCESS_CASH,
                        onClick = { onDifferenceReasonChange(DayClosingReason.EXCESS_CASH) },
                        label = { Text("Excess Cash") },
                    )
                }
            }
            OutlinedTextField(
                value = remarks,
                onValueChange = onRemarksChange,
                label = { Text("Remarks (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            preview.closedRecord?.let { closed ->
                MetricRow("Actual Cash", FormatUtils.formatCurrency(closed.actualCash), bold = true)
                MetricRow("Difference", FormatUtils.formatCurrency(closed.difference), bold = true)
                closed.differenceReason?.let {
                    MetricRow("Reason", DayClosingReason.label(it).orEmpty())
                }
                closed.remarks?.takeIf { it.isNotBlank() }?.let {
                    Text("Remarks: $it", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    bold: Boolean = false,
    highlight: Boolean = false,
    error: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = when {
                error -> MaterialTheme.colorScheme.error
                highlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
