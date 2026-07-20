package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.model.PurchaseReturnReason
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseReturnScreen(
    onBack: () -> Unit,
    onReturnCompleted: () -> Unit = onBack,
    viewModel: PurchaseReturnViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PurchaseReturnEvent.ReturnCompleted -> {
                    snackbar.showSnackbar("Return saved successfully")
                    onReturnCompleted()
                }
                is PurchaseReturnEvent.ShowError -> snackbar.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Return") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!state.isLoading && state.purchase != null) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Total: ${FormatUtils.formatCurrency(state.grandTotal)}", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = viewModel::saveReturn,
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(if (state.isSaving) "Saving…" else "Save Return")
                    }
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
            state.purchase == null -> {
                Text(
                    "Purchase not found",
                    Modifier.padding(padding).padding(16.dp),
                )
            }
            else -> {
                val purchase = state.purchase!!
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(purchase.invoiceNumber, style = MaterialTheme.typography.titleMedium)
                                Text(purchase.supplierName.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        .format(Date(purchase.purchaseDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    item {
                        ReasonDropdown(
                            selected = state.selectedReason,
                            onSelected = viewModel::onReasonSelected,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = state.notes,
                            onValueChange = viewModel::onNotesChange,
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(state.lines, key = { it.purchaseItemUuid }) { line ->
                        ReturnLineCard(
                            line = line,
                            onQtyChange = { viewModel.onReturnQtyChange(line.purchaseItemUuid, it) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonDropdown(
    selected: PurchaseReturnReason,
    onSelected: (PurchaseReturnReason) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Return Reason") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PurchaseReturnReason.entries.forEach { reason ->
                DropdownMenuItem(
                    text = { Text(reason.label) },
                    onClick = {
                        onSelected(reason)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ReturnLineCard(
    line: com.medipro.manager.domain.model.PurchaseReturnLine,
    onQtyChange: (Int) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(line.medicineName, style = MaterialTheme.typography.titleSmall)
            Text("Batch: ${line.batchNumber}")
            Text("Purchased: ${line.purchasedQty} | Returned: ${line.alreadyReturnedQty} | Stock: ${line.currentBatchStock}")
            Text("Max returnable: ${line.maxReturnableQty}", color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = if (line.returnQty == 0) "" else line.returnQty.toString(),
                onValueChange = { onQtyChange(it.toIntOrNull() ?: 0) },
                label = { Text("Return Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    if (line.maxReturnableQty == 0) Text("Nothing left to return")
                },
            )
            if (line.returnQty > 0) {
                Text("Line total: ${FormatUtils.formatCurrency(line.lineTotal)}")
            }
        }
    }
}
