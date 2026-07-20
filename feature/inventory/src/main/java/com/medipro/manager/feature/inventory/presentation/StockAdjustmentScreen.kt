package com.medipro.manager.feature.inventory.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.domain.model.StockAdjustmentType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StockAdjustmentScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = onBack,
    viewModel: StockAdjustmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StockAdjustmentEvent.Saved -> {
                    snackbar.showSnackbar("Adjustment saved")
                    onSaved()
                }
                is StockAdjustmentEvent.ShowError -> snackbar.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Adjustment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.context != null) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save Adjustment")
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
            }
            state.context == null -> {
                Text("Batch not found", Modifier.padding(padding).padding(16.dp))
            }
            else -> {
                val ctx = state.context!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ctx.medicine.brandName, style = MaterialTheme.typography.titleMedium)
                            Text("Batch: ${ctx.batch.batchNumber}")
                            Text("Expiry: ${dateFmt.format(Date(ctx.batch.expiryDate))}")
                            Text("Current Qty: ${ctx.batch.sellableQty}")
                            if (ctx.batch.damagedQty > 0) {
                                Text("Damaged: ${ctx.batch.damagedQty}")
                            }
                        }
                    }

                    Text("Adjustment Type", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StockAdjustmentType.entries.forEach { type ->
                            FilterChip(
                                selected = state.selectedType == type,
                                onClick = { viewModel.onTypeSelected(type) },
                                label = { Text("${type.emoji} ${type.label}") },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.quantityInput,
                        onValueChange = viewModel::onQuantityChange,
                        label = {
                            Text(
                                if (state.selectedType.usesNewQty) "New Qty (Physical Count)"
                                else "Adjust Qty"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            state.previewNewQty?.let { newQty ->
                                Text("New quantity will be: $newQty")
                            }
                        },
                    )

                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = viewModel::onReasonChange,
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.remarks,
                        onValueChange = viewModel::onRemarksChange,
                        label = { Text("Remarks (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
