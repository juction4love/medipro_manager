package com.medipro.manager.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.domain.model.OcrMedicineAlias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrLearningScreen(
    onBack: () -> Unit,
    viewModel: OcrLearningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Learning") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Learned mappings from bill scans. Wrong mapping? Delete, edit, or disable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Search OCR or medicine") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = viewModel::showResetConfirm,
                enabled = state.allAliases.isNotEmpty() && !state.isResetting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Learning")
            }
            if (state.visibleAliases.isEmpty()) {
                Text(
                    "No learned mappings yet — link medicines while reviewing scanned bills.",
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.visibleAliases, key = { it.id }) { alias ->
                        OcrAliasCard(
                            alias = alias,
                            onEdit = { viewModel.startEdit(alias) },
                            onDelete = { viewModel.deleteAlias(alias.id) },
                            onToggle = { viewModel.toggleEnabled(alias) },
                        )
                    }
                }
            }
        }
    }

    state.editingAlias?.let { alias ->
        EditOcrAliasDialog(
            ocrText = state.editOcrText,
            medicineQuery = state.editMedicineQuery,
            results = state.editMedicineResults,
            isSaving = state.isSaving,
            onOcrTextChange = viewModel::onEditOcrTextChange,
            onMedicineQueryChange = viewModel::onEditMedicineQueryChange,
            onSelectMedicine = viewModel::selectMedicine,
            onDismiss = viewModel::dismissEdit,
            onSave = viewModel::saveEdit,
            aliasLabel = alias.ocrText,
        )
    }

    if (state.showResetConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.isResetting) viewModel.dismissResetConfirm() },
            title = { Text("Reset Learning") },
            text = { Text("Delete all OCR learned aliases?") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmResetLearning,
                    enabled = !state.isResetting,
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissResetConfirm,
                    enabled = !state.isResetting,
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun OcrAliasCard(
    alias: OcrMedicineAlias,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(alias.ocrText, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("→ ${alias.medicineName}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (alias.isEnabled) "Active · ${alias.hitCount} hits" else "Disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alias.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onToggle) {
                    Text(if (alias.isEnabled) "Disable" else "Enable")
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun EditOcrAliasDialog(
    ocrText: String,
    medicineQuery: String,
    results: List<com.medipro.manager.domain.model.Medicine>,
    isSaving: Boolean,
    onOcrTextChange: (String) -> Unit,
    onMedicineQueryChange: (String) -> Unit,
    onSelectMedicine: (com.medipro.manager.domain.model.Medicine) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    aliasLabel: String,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit Mapping") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ocrText,
                    onValueChange = onOcrTextChange,
                    label = { Text("OCR text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = medicineQuery,
                    onValueChange = onMedicineQueryChange,
                    label = { Text("Medicine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                ) {
                    items(results, key = { it.id }) { medicine ->
                        Text(
                            "${medicine.brandName} · ${medicine.genericName}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMedicine(medicine) }
                                .padding(vertical = 6.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving && ocrText.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}
