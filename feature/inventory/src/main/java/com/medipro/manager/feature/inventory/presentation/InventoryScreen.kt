package com.medipro.manager.feature.inventory.presentation

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.medipro.manager.core.designsystem.component.MediProScreenWithFab
import com.medipro.manager.core.designsystem.navigation.MediProGlobalSearchIcon
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.BatchStockDetail
import com.medipro.manager.domain.model.ExpiryReportRow
import com.medipro.manager.domain.model.InventoryMedicineStock
import com.medipro.manager.domain.model.StockAdjustment
import com.medipro.manager.domain.model.StockAdjustmentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGlobalSearch: (() -> Unit)? = null,
    onOpenAdjustment: (batchId: Long, type: String?) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val stockSearchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InventoryEvent.OpenAdjustment -> onOpenAdjustment(event.batchId, event.type)
                InventoryEvent.FocusStockSearch -> stockSearchFocus.requestFocus()
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Stock Room") },
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
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                state.summary?.let { SummaryCards(it) }
                StockTab(
                    state = state,
                    searchFocus = stockSearchFocus,
                    onSearch = viewModel::onSearchQueryChange,
                    onSelectMedicine = viewModel::selectMedicine,
                    onClearSelection = viewModel::clearSelection,
                    onSelectBatch = viewModel::selectBatch,
                    onClearBatch = viewModel::clearBatchSelection,
                    onAdjustmentType = viewModel::onAdjustmentTypeSelected,
                    onAdjustmentQty = viewModel::onAdjustmentQuantityChange,
                    onAdjustmentReason = viewModel::onAdjustmentReasonChange,
                    onAdjustmentRemarks = viewModel::onAdjustmentRemarksChange,
                    onSaveAdjustment = viewModel::saveInlineAdjustment,
                )
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCards(summary: com.medipro.manager.domain.model.InventorySummary) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryChip("Total Stock", FormatUtils.formatCount(summary.totalStockUnits))
        SummaryChip("Inventory Value", FormatUtils.formatCurrency(summary.inventoryValue))
        SummaryChip("Low Stock", summary.lowStockCount.toString())
        SummaryChip("Near Expiry", summary.nearExpiryCount.toString())
        SummaryChip("Expired", summary.expiredCount.toString())
        SummaryChip("Today's Adjustments", summary.todayAdjustments.toString())
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label: $value") })
}

@Composable
private fun StockTab(
    state: InventoryState,
    searchFocus: FocusRequester,
    onSearch: (String) -> Unit,
    onSelectMedicine: (InventoryMedicineStock) -> Unit,
    onClearSelection: () -> Unit,
    onSelectBatch: (Long, StockAdjustmentType?) -> Unit,
    onClearBatch: () -> Unit,
    onAdjustmentType: (StockAdjustmentType) -> Unit,
    onAdjustmentQty: (String) -> Unit,
    onAdjustmentReason: (String) -> Unit,
    onAdjustmentRemarks: (String) -> Unit,
    onSaveAdjustment: () -> Unit,
) {
    if (state.isLoading && state.summary == null) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                label = { Text("Search medicine") },
                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                singleLine = true,
            )
        }

        if (state.selectedMedicine != null) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        state.selectedMedicine!!.medicine.brandName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onClearSelection) { Text("Back") }
                }
            }
            items(state.selectedMedicine!!.batches, key = { it.batchId }) { batch ->
                val batchHistory = state.adjustments.filter { it.batchId == batch.batchId }.take(3)
                BatchControlCard(
                    batch = batch,
                    isSelected = state.selectedBatchId == batch.batchId,
                    batchHistory = batchHistory,
                    inlineAdjustment = if (state.selectedBatchId == batch.batchId) state.inlineAdjustment else null,
                    onSelect = { onSelectBatch(batch.batchId, null) },
                    onQuickAction = { type -> onSelectBatch(batch.batchId, type) },
                    onClear = onClearBatch,
                    onAdjustmentType = onAdjustmentType,
                    onAdjustmentQty = onAdjustmentQty,
                    onAdjustmentReason = onAdjustmentReason,
                    onAdjustmentRemarks = onAdjustmentRemarks,
                    onSaveAdjustment = onSaveAdjustment,
                )
            }
        } else {
            items(state.searchResults, key = { it.medicine.id }) { item ->
                Card(
                    onClick = { onSelectMedicine(item) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.medicine.brandName, style = MaterialTheme.typography.titleSmall)
                            Text(item.medicine.genericName, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${item.totalQty} ${item.medicine.unit}")
                    }
                }
            }
            if (state.searchQuery.length >= 2 && state.searchResults.isEmpty()) {
                item { Text("No medicines found", Modifier.padding(8.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchControlCard(
    batch: BatchStockDetail,
    isSelected: Boolean,
    batchHistory: List<StockAdjustment>,
    inlineAdjustment: InlineAdjustmentDraft?,
    onSelect: () -> Unit,
    onQuickAction: (StockAdjustmentType) -> Unit,
    onClear: () -> Unit,
    onAdjustmentType: (StockAdjustmentType) -> Unit,
    onAdjustmentQty: (String) -> Unit,
    onAdjustmentReason: (String) -> Unit,
    onAdjustmentRemarks: (String) -> Unit,
    onSaveAdjustment: () -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val historyFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = when {
            batch.isExpired -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            isSelected -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            else -> CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Batch: ${batch.batchNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Expiry: ${dateFmt.format(Date(batch.expiryDate))}")
            Text(
                "Current Qty: ${batch.sellableQty} sellable · ${batch.damagedQty} damaged",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (batchHistory.isNotEmpty()) {
                Text("Recent adjustments", style = MaterialTheme.typography.labelMedium)
                batchHistory.forEach { adj ->
                    Text(
                        "${historyFmt.format(Date(adj.createdAt))} · ${adj.type.label} · ${adj.adjustQty} → ${adj.newQty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isSelected) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onSelect) { Text("Adjust") }
                    TextButton(onClick = { onQuickAction(StockAdjustmentType.DAMAGE) }) { Text("Damage") }
                    TextButton(onClick = { onQuickAction(StockAdjustmentType.EXPIRED) }) { Text("Expired") }
                    TextButton(onClick = { onQuickAction(StockAdjustmentType.LOST) }) { Text("Lost") }
                    TextButton(onClick = { onQuickAction(StockAdjustmentType.PHYSICAL_COUNT) }) { Text("Count") }
                }
            } else {
                HorizontalDivider()
                Text("Action", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StockAdjustmentType.entries.forEach { type ->
                        FilterChip(
                            selected = inlineAdjustment?.selectedType == type,
                            onClick = { onAdjustmentType(type) },
                            label = { Text(type.label, maxLines = 1) },
                        )
                    }
                }

                if (inlineAdjustment?.isLoading == true) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else if (inlineAdjustment?.context != null) {
                    OutlinedTextField(
                        value = inlineAdjustment.quantityInput,
                        onValueChange = onAdjustmentQty,
                        label = {
                            Text(
                                if (inlineAdjustment.selectedType.usesNewQty) "New Qty (Physical Count)"
                                else "Adjust Qty",
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            inlineAdjustment.previewNewQty?.let { newQty ->
                                Text("New quantity will be: $newQty")
                            }
                        },
                    )
                    OutlinedTextField(
                        value = inlineAdjustment.reason,
                        onValueChange = onAdjustmentReason,
                        label = { Text("Reason *") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = inlineAdjustment.remarks,
                        onValueChange = onAdjustmentRemarks,
                        label = { Text("Remarks (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onClear) { Text("Cancel") }
                        Button(
                            onClick = onSaveAdjustment,
                            enabled = inlineAdjustment.canSave,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (inlineAdjustment.isSaving) "Saving…" else "Save Adjustment")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentHistoryTab(adjustments: List<StockAdjustment>) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    if (adjustments.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { Text("No adjustments yet") }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(adjustments, key = { it.uuid }) { adj ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${adj.type.emoji} ${adj.type.label}", style = MaterialTheme.typography.titleSmall)
                    Text("${adj.medicineName} · Batch ${adj.batchNumber}")
                    Text("${adj.oldQty} → ${adj.newQty} (${adj.adjustQty})")
                    Text(adj.reason, style = MaterialTheme.typography.bodySmall)
                    Text(dateFmt.format(Date(adj.createdAt)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ReportsTab(
    damageRows: List<StockAdjustment>,
    expiryRows: List<ExpiryReportRow>,
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Damage Report", style = MaterialTheme.typography.titleMedium) }
        if (damageRows.isEmpty()) {
            item { Text("No damage adjustments recorded") }
        } else {
            items(damageRows, key = { it.uuid }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${row.medicineName} · ${row.batchNumber}")
                        Text("Qty: ${abs(row.adjustQty)} · ${row.reason}")
                        Text(dateFmt.format(Date(row.createdAt)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { Text("Expiry Report", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        if (expiryRows.isEmpty()) {
            item { Text("No batches near expiry") }
        } else {
            items(expiryRows, key = { "${it.medicineName}_${it.batchNumber}" }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${row.medicineName} · ${row.batchNumber}")
                        Text("Expiry: ${dateFmt.format(Date(row.expiryDate))}")
                        Text("Remaining: ${row.remainingQty}")
                    }
                }
            }
        }
    }
}

private fun abs(n: Int) = if (n < 0) -n else n
