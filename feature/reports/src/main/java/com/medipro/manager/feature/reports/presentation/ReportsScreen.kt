package com.medipro.manager.feature.reports.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.medipro.manager.core.common.DateRangeUtils
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.ReportPeriod
import com.medipro.manager.core.designsystem.navigation.MediProGlobalSearchIcon
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.RankedRow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGlobalSearch: (() -> Unit)? = null,
    onRequireSubscription: () -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportsEvent.ShareExport -> {
                    val file = File(event.filePath)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Report"))
                }
                ReportsEvent.RequirePremium -> onRequireSubscription()
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
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
                    IconButton(onClick = viewModel::exportCsv) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
                    }
                    IconButton(onClick = viewModel::exportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PeriodSelector(
                selected = state.selectedPeriod,
                customStart = state.customStartMillis,
                customEnd = state.customEndMillis,
                onSelected = viewModel::onPeriodSelected,
            )

            if (state.showCustomDatePicker) {
                CustomDateRangeDialog(
                    initialStart = state.customStartMillis ?: DateRangeUtils.dayRange(-6).first,
                    initialEnd = state.customEndMillis ?: DateRangeUtils.dayRange(0).second,
                    onDismiss = viewModel::dismissCustomDatePicker,
                    onConfirm = viewModel::onCustomRangeSelected,
                )
            }

            if (state.isLoading && state.overviewKpis.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                ReportTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.label, maxLines = 1) },
                    )
                }
            }

            if (state.isTabLoading) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state.selectedTab) {
                    ReportTab.OVERVIEW -> {
                        items(state.overviewKpis.chunked(2)) { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { kpi ->
                                    KpiCard(kpi, Modifier.weight(1f))
                                }
                                if (row.size == 1) {
                                    Card(Modifier.weight(1f)) {}
                                }
                            }
                        }
                    }
                    ReportTab.SALES -> state.salesReport?.let { sales ->
                        item { SectionTitle("Daily Sales") }
                        items(sales.dailyBreakdown) { day ->
                            RankedCard(
                                title = FormatUtils.formatDate(day.dayMillis),
                                primary = FormatUtils.formatCurrency(day.amount),
                                secondary = "${day.count} invoices",
                            )
                        }
                        item { SectionTitle("Medicine-wise") }
                        items(sales.medicineWise) { row ->
                            RankedCard(
                                title = row.name,
                                primary = "${row.quantity} units",
                                secondary = FormatUtils.formatCurrency(row.amount),
                            )
                        }
                        item { SectionTitle("Payment Method") }
                        items(sales.paymentMethodWise) { row ->
                            RankedCard(
                                title = row.name,
                                primary = FormatUtils.formatCurrency(row.amount),
                                secondary = "${row.quantity} sales",
                            )
                        }
                        item { SectionTitle("Category-wise") }
                        items(sales.categoryWise) { row ->
                            RankedCard(
                                title = row.name.ifBlank { "Uncategorized" },
                                primary = FormatUtils.formatCurrency(row.amount),
                                secondary = "${row.quantity} units",
                            )
                        }
                    }
                    ReportTab.PURCHASE -> state.purchaseReport?.let { purchase ->
                        item { SectionTitle("Supplier-wise Purchase") }
                        items(purchase.supplierWise) { row ->
                            RankedCard(
                                title = row.name,
                                primary = FormatUtils.formatCurrency(row.amount),
                                secondary = "${row.quantity} bills",
                            )
                        }
                        item { SectionTitle("Daily Purchase") }
                        items(purchase.dailyBreakdown) { day ->
                            RankedCard(
                                title = FormatUtils.formatDate(day.dayMillis),
                                primary = FormatUtils.formatCurrency(day.amount),
                                secondary = "${day.count} bills",
                            )
                        }
                    }
                    ReportTab.INVENTORY -> state.inventoryReport?.let { inv ->
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                KpiCard(KpiUi("Inventory Value", FormatUtils.formatCurrency(inv.inventoryValue)), Modifier.weight(1f))
                                KpiCard(KpiUi("Total Units", inv.totalUnits.toString()), Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                KpiCard(KpiUi("Low Stock", inv.lowStockCount.toString()), Modifier.weight(1f))
                                KpiCard(KpiUi("Out of Stock", inv.outOfStockCount.toString()), Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                KpiCard(KpiUi("Expiry 30d", inv.nearExpiry30.toString()), Modifier.weight(1f))
                                KpiCard(KpiUi("Expiry 60d", inv.nearExpiry60.toString()), Modifier.weight(1f))
                                KpiCard(KpiUi("Expiry 90d", inv.nearExpiry90.toString()), Modifier.weight(1f))
                            }
                        }
                        item { SectionTitle("Fast Moving (30 days)") }
                        items(inv.fastMoving) { row -> medicineRow(row) }
                        item { SectionTitle("Slow Moving (90 days)") }
                        items(inv.slowMoving) { row ->
                            RankedCard(
                                title = row.name,
                                primary = "${row.quantity} in stock",
                                secondary = "${row.amount.toInt()} sold",
                            )
                        }
                        item { SectionTitle("Top Inventory Value") }
                        items(inv.topValueMedicines) { row ->
                            RankedCard(
                                title = row.name,
                                primary = FormatUtils.formatCurrency(row.amount),
                                secondary = "${row.quantity} units",
                            )
                        }
                    }
                    ReportTab.FINANCIAL -> state.financialReport?.let { fin ->
                        item { SectionTitle("Profit & Loss") }
                        items(fin.summary.toFinancialKpis()) { kpi -> KpiCard(kpi) }
                        item { SectionTitle("VAT Summary") }
                        item {
                            RankedCard("Sales VAT", FormatUtils.formatCurrency(fin.vatSummary.salesVat))
                            RankedCard("Purchase VAT", FormatUtils.formatCurrency(fin.vatSummary.purchaseVat))
                            RankedCard("Net VAT Payable", FormatUtils.formatCurrency(fin.vatSummary.netVat))
                        }
                        item { SectionTitle("Cash Flow") }
                        item {
                            RankedCard("Cash In", FormatUtils.formatCurrency(fin.cashFlow.cashIn))
                            RankedCard("Cash Out", FormatUtils.formatCurrency(fin.cashFlow.cashOut))
                            RankedCard("Net Cash", FormatUtils.formatCurrency(fin.cashFlow.netCash))
                        }
                    }
                    ReportTab.CUSTOMERS -> state.customerReport?.let { customers ->
                        item { SectionTitle("Top Customers") }
                        items(customers.topCustomers) { row ->
                            RankedCard(row.name, FormatUtils.formatCurrency(row.amount), "${row.quantity} visits")
                        }
                        item { SectionTitle("Credit / Outstanding") }
                        items(customers.creditCustomers) { row ->
                            RankedCard(row.name, FormatUtils.formatCurrency(row.amount), row.subtitle)
                        }
                    }
                    ReportTab.SUPPLIERS -> state.supplierReport?.let { suppliers ->
                        item { SectionTitle("Supplier Due") }
                        items(suppliers.supplierDueList) { row ->
                            RankedCard(row.name, FormatUtils.formatCurrency(row.amount), row.subtitle)
                        }
                        item { SectionTitle("Recent Purchases") }
                        items(suppliers.recentPurchases) { row ->
                            RankedCard(row.name, FormatUtils.formatCurrency(row.amount), "${row.quantity} bills")
                        }
                    }
                    ReportTab.MEDICINE -> state.medicineAnalytics?.let { med ->
                        item { SectionTitle("Top Selling") }
                        items(med.topSelling) { row -> medicineRow(row) }
                        item { SectionTitle("Least Selling") }
                        items(med.leastSelling) { row -> medicineRow(row) }
                        item { SectionTitle("Most Returned") }
                        items(med.mostReturned) { row ->
                            RankedCard(row.name, "${row.quantity} units", FormatUtils.formatCurrency(row.amount))
                        }
                    }
                    ReportTab.AUDIT -> {
                        item { SectionTitle("Audit Events") }
                        items(state.auditRows) { row ->
                            RankedCard(row.eventType, row.count.toString())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    selected: ReportPeriod,
    customStart: Long?,
    customEnd: Long?,
    onSelected: (ReportPeriod) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selected == period,
                    onClick = { onSelected(period) },
                    label = {
                        Text(
                            if (period == ReportPeriod.CUSTOM && customStart != null && customEnd != null) {
                                "${com.medipro.manager.core.common.DateRangeUtils.formatShortDate(customStart)} – ${com.medipro.manager.core.common.DateRangeUtils.formatShortDate(customEnd)}"
                            } else {
                                period.label
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun KpiCard(kpi: KpiUi, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(kpi.label, style = MaterialTheme.typography.labelMedium)
            Text(kpi.value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RankedCard(title: String, primary: String, secondary: String? = null) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text(primary, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun medicineRow(row: RankedRow) {
    RankedCard(
        title = row.name,
        primary = "${row.quantity} units",
        secondary = FormatUtils.formatCurrency(row.amount),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val startState = rememberDatePickerState(initialSelectedDateMillis = initialStart)
    val endState = rememberDatePickerState(initialSelectedDateMillis = initialEnd)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 0) {
                        step = 1
                    } else {
                        val start = startState.selectedDateMillis ?: initialStart
                        val end = endState.selectedDateMillis ?: initialEnd
                        onConfirm(minOf(start, end), maxOf(start, end))
                    }
                },
            ) {
                Text(if (step == 0) "Next" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(if (step == 0) "Select start date" else "Select end date")
            if (step == 0) {
                DatePicker(state = startState)
            } else {
                DatePicker(state = endState)
            }
        }
    }
}

private fun com.medipro.manager.domain.model.ReportDashboardSummary.toFinancialKpis(): List<KpiUi> = listOf(
    KpiUi("Net Sales", FormatUtils.formatCurrency(netSales)),
    KpiUi("Net Purchase", FormatUtils.formatCurrency(netPurchase)),
    KpiUi("Expense", FormatUtils.formatCurrency(expense)),
    KpiUi("Gross Profit", FormatUtils.formatCurrency(grossProfit)),
    KpiUi("Net Profit", FormatUtils.formatCurrency(netProfit)),
    KpiUi("Margin %", String.format("%.1f%%", marginPercent)),
    KpiUi("Discount Given", FormatUtils.formatCurrency(discountGiven)),
)
