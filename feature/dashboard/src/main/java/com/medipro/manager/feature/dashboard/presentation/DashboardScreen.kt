package com.medipro.manager.feature.dashboard.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.designsystem.component.SectionHeader
import com.medipro.manager.core.designsystem.component.StatCard
import com.medipro.manager.core.designsystem.component.MediProScreenWithFab
import com.medipro.manager.core.designsystem.navigation.MediProGlobalSearchIcon
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.AlertSeverity

private data class SpeedDialAction(
    val title: String,
    val route: String,
    val icon: ImageVector,
)

private val speedDialActions = listOf(
    SpeedDialAction("Adjustment", "inventory", Icons.Default.Tune),
    SpeedDialAction("Supplier", "supplier", Icons.Default.Store),
    SpeedDialAction("Customer", "customer", Icons.Default.People),
    SpeedDialAction("Purchase", "purchase", Icons.Default.ShoppingCart),
    SpeedDialAction("Sale", "sales", Icons.Default.PointOfSale),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGlobalSearch: (() -> Unit)? = null,
    onOpenInvoice: (Long) -> Unit = { onNavigate("sales") },
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.NavigateToFeature -> onNavigate(event.route)
                is DashboardEvent.OpenInvoice -> onOpenInvoice(event.saleId)
                DashboardEvent.Refresh -> Unit
            }
        }
    }

    Scaffold(
            topBar = {
                DashboardTopBar(
                    pharmacyName = state.pharmacyName,
                    dateLabel = state.dateLabel,
                    syncStatusLabel = state.syncStatusLabel,
                    licenseDaysRemaining = state.licenseDaysRemaining,
                    onOpenDrawer = onOpenDrawer,
                    onOpenGlobalSearch = onOpenGlobalSearch,
                    onNotifications = { onNavigate("notification") },
                )
            },
        ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { CashInHandHero(state.cashInDrawer, state.dateLabel) }
                    item { QuickActionsSection(onNavigate = { route -> viewModel.onFeatureClick(route) }) }
                    item { KpiSection("Today", todayOverviewKpis(state)) }
                    item { OcrScannerSection(state) }
                    item { KpiSection("Cash & Dues", cashAndDuesKpis(state)) }
                    item { KpiSection("Stock", stockOverviewKpis(state)) }
                    item {
                        AlertsSection(
                            alerts = state.alerts,
                            onAlertClick = { route -> route?.let(viewModel::onFeatureClick) },
                        )
                    }
                }
            }
        }
    }
}

private fun todayOverviewKpis(state: DashboardState) = listOf(
    KpiItem("Today's Sales", state.todaySales, "${state.todaySalesCount} invoices"),
    KpiItem("Today's Purchase", state.todayPurchase, "${state.todayPurchaseCount} bills"),
)

private fun ocrAnalyticsKpis(state: DashboardState) = listOf(
    KpiItem("Today's Bills", state.ocrTodayBills),
    KpiItem("Average OCR Accuracy", state.ocrAverageAccuracy),
    KpiItem("Learned Aliases", state.ocrLearnedAliases),
    KpiItem("Manual Corrections", state.ocrManualCorrections),
    KpiItem("OCR Saved Time", state.ocrSavedTime),
)

@Composable
private fun OcrScannerSection(state: DashboardState) {
    SectionHeader(title = "OCR Scanner")
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OcrLastScanRow("Last Scan", state.ocrLastScanTime)
            OcrLastScanRow("Supplier", state.ocrLastScanSupplier)
            OcrLastScanRow("Accuracy", state.ocrLastScanAccuracy)
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    KpiSection("", ocrAnalyticsKpis(state))
}

@Composable
private fun OcrLastScanRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun cashAndDuesKpis(state: DashboardState) = listOf(
    KpiItem("Customer Due", state.pendingCustomerDue, "${state.pendingCustomerDueCount} customers"),
    KpiItem("Supplier Due", state.supplierDue, "${state.supplierDueCount} suppliers"),
)

private fun stockOverviewKpis(state: DashboardState) = listOf(
    KpiItem("Inventory Value", state.inventoryValue),
    KpiItem("Low Stock", state.lowStockCount.toString()),
    KpiItem("Near Expiry", state.nearExpiryCount.toString()),
    KpiItem("Expired", state.expiredCount.toString()),
)

private fun financialKpis(state: DashboardState) = listOf(
    KpiItem("Today's Sales", state.todaySales, "${state.todaySalesCount} Invoices"),
    KpiItem("Sales Return", state.todaySalesReturn),
    KpiItem("Net Sales", state.netSales),
    KpiItem("Today's Purchase", state.todayPurchase, "${state.todayPurchaseCount} Bills"),
    KpiItem("Today's Profit", state.profit),
    KpiItem("Today's Expense", state.todayExpense),
    KpiItem("Cash In Drawer", state.cashInDrawer),
    KpiItem("Bank Balance", state.bankBalance),
)

private fun inventoryAlertKpis(state: DashboardState) = listOf(
    KpiItem("Low Stock", state.lowStockCount.toString()),
    KpiItem("Out Of Stock", state.outOfStockCount.toString()),
    KpiItem("Near Expiry (30d)", state.nearExpiryCount.toString()),
    KpiItem("Expired", state.expiredCount.toString()),
    KpiItem("Today's Adjustments", state.todayAdjustments),
)

private fun customerKpis(state: DashboardState) = listOf(
    KpiItem("Today's Customers", state.todayCustomers.toString()),
    KpiItem("Pending Due", state.pendingCustomerDue, "${state.pendingCustomerDueCount} accounts"),
    KpiItem("Collected Today", state.collectedToday),
)

private fun supplierKpis(state: DashboardState) = listOf(
    KpiItem("Supplier Due", state.supplierDue, "${state.supplierDueCount} suppliers"),
    KpiItem("Today's Payment", state.todaySupplierPayment),
)

private fun businessKpis(state: DashboardState) = listOf(
    KpiItem("Best Selling", state.bestSellingMedicine),
    KpiItem("Top Category", state.topCategory),
    KpiItem("Today's Transactions", state.todayTransactions.toString()),
)

@Composable
private fun CashInHandHero(cashInHand: String, dateLabel: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Cash in Hand",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                cashInHand,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (dateLabel.isNotBlank()) {
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun KpiSection(title: String, items: List<KpiItem>) {
    if (title.isNotBlank()) {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(8.dp))
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(((items.size + 1) / 2 * 96).coerceAtLeast(96).dp),
    ) {
        items(items) { kpi ->
            StatCard(
                title = kpi.label,
                value = kpi.value,
                subtitle = kpi.subtitle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InventoryCard(state: DashboardState) {
    SectionHeader(title = "Inventory")
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InventoryMetricRow("Active Products", state.activeMedicineCount)
            InventoryMetricRow("Master Catalog", state.masterCatalogCount)
            InventoryMetricRow("Total Stock Units", state.totalStockUnits)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Inventory Value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.inventoryValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "Σ Purchase Cost",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun InventoryMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    pharmacyName: String,
    dateLabel: String,
    syncStatusLabel: String,
    licenseDaysRemaining: Int?,
    onOpenDrawer: (() -> Unit)?,
    onOpenGlobalSearch: (() -> Unit)?,
    onNotifications: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        navigationIcon = {
            MediProTopBarNavigationIcon(onBack = null, onOpenDrawer = onOpenDrawer)
        },
        title = {
            Column {
                Text(
                    text = "MediPro ERP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = pharmacyName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateLabel.isNotBlank()) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        },
        actions = {
            onOpenGlobalSearch?.let { opener ->
                MediProGlobalSearchIcon(onClick = opener)
            }
            IconButton(onClick = onNotifications) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = syncStatusLabel,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = licenseDaysRemaining?.let { "$it Days Left" } ?: syncStatusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        },
    )
}

@Composable
private fun AlertsSection(
    alerts: List<AlertUi>,
    onAlertClick: (String?) -> Unit,
) {
    SectionHeader(title = "Alerts")
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            val (emoji, containerColor) = when (alert.severity) {
                AlertSeverity.CRITICAL -> "🔴" to MaterialTheme.colorScheme.errorContainer
                AlertSeverity.WARNING -> "🟠" to Color(0xFFFFE0B2)
                AlertSeverity.INFO -> "🟢" to MaterialTheme.colorScheme.tertiaryContainer
                AlertSeverity.OK -> "✅" to MaterialTheme.colorScheme.tertiaryContainer
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = alert.route != null) { onAlertClick(alert.route) },
                colors = CardDefaults.cardColors(containerColor = containerColor),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                    Text(text = alert.message, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(onNavigate: (String) -> Unit) {
    SectionHeader(title = "Quick Actions")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        item { QuickActionCard("New Sale", Icons.Default.PointOfSale, "sales", onNavigate) }
        item { QuickActionCard("New Purchase", Icons.Default.ShoppingCart, "purchase", onNavigate) }
        item { QuickActionCard("Receive Payment", Icons.Default.People, "accounting/customer_receipts", onNavigate) }
        item { QuickActionCard("Supplier Payment", Icons.Default.Store, "accounting/supplier_payments", onNavigate) }
        item { QuickActionCard("Stock Adjustment", Icons.Default.Tune, "inventory", onNavigate) }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    route: String,
    onNavigate: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(route) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DashboardSpeedDial(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        speedDialActions.forEach { action ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                SpeedDialItem(
                    label = action.title,
                    icon = action.icon,
                    onClick = { onNavigate(action.route) },
                )
            }
        }
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close quick actions" else "Quick actions",
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun SalesAnalyticsSection(
    todayAmount: String,
    yesterdayAmount: String,
    last7DaysAmount: String,
    last30DaysAmount: String,
) {
    SectionHeader(title = "Sales Analytics")
    Spacer(modifier = Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsRow("Today's Sales", todayAmount)
            AnalyticsRow("Yesterday", yesterdayAmount)
            AnalyticsRow("Last 7 Days", last7DaysAmount)
            AnalyticsRow("Last 30 Days", last30DaysAmount)
            SalesSparkline(
                values = listOf(
                    parseAmount(yesterdayAmount),
                    parseAmount(todayAmount),
                    parseAmount(last7DaysAmount) / 7.0,
                    parseAmount(last30DaysAmount) / 30.0,
                ),
            )
        }
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SalesSparkline(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 8.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val max = values.max().coerceAtLeast(1.0)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - ((value / max) * size.height * 0.85f).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height - ((value / max) * size.height * 0.85f).toFloat()
            drawCircle(color = color, radius = 5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun TransactionRow(
    title: String,
    subtitle: String,
    amount: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PrescriptionQueueSection() {
    SectionHeader(title = "Prescription Queue")
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RxStatChip("Pending Rx", "0", Modifier.weight(1f))
        RxStatChip("Verified", "0", Modifier.weight(1f))
        RxStatChip("Waiting", "0", Modifier.weight(1f))
    }
}

@Composable
private fun RxStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BackupLicenseSection(
    lastBackupLabel: String?,
    lastBackupEncrypted: Boolean,
    cloudBackupStatus: String,
    licensePlan: String,
    licenseMobile: String?,
    licenseValidUntil: String?,
    licenseDaysRemaining: Int?,
    licenseLastVerified: String?,
    syncStatusLabel: String,
    onBackupClick: () -> Unit,
) {
    SectionHeader(title = "Backup & License")
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onBackupClick),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Last Backup", style = MaterialTheme.typography.labelLarge)
                Text(
                    lastBackupLabel ?: "Never",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Status: ${if (lastBackupEncrypted) "Encrypted" else "Plain"}", style = MaterialTheme.typography.bodySmall)
                Text("Cloud: $cloudBackupStatus", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Licensed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Plan: $licensePlan", style = MaterialTheme.typography.bodyMedium)
                Text("Mobile: ${licenseMobile ?: "—"}", style = MaterialTheme.typography.bodySmall)
                Text("Expires: ${licenseValidUntil ?: "—"}", style = MaterialTheme.typography.bodySmall)
                Text("Remaining: ${licenseDaysRemaining ?: "—"} Days", style = MaterialTheme.typography.bodySmall)
                Text("Last Verified: ${licenseLastVerified ?: "—"}", style = MaterialTheme.typography.bodySmall)
                Text(syncStatusLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun parseAmount(formatted: String): Double {
    val digits = formatted.filter { it.isDigit() || it == '.' || it == '-' }
    return digits.toDoubleOrNull() ?: 0.0
}
