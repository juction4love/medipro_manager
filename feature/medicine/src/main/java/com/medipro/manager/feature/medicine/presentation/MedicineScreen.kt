package com.medipro.manager.feature.medicine.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.model.Medicine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScreen(
    onBack: () -> Unit,
    viewModel: MedicineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Medicine")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                label = { Text("Search brand, generic, composition, strength, manufacturer, barcode") },
                singleLine = true
            )

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.medicines, key = { it.id }) { medicine ->
                    MedicineItem(
                        medicine = medicine,
                        onDelete = { viewModel.deleteMedicine(medicine.id) }
                    )
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddMedicineDialog(
            onDismiss = { viewModel.toggleAddDialog(false) },
            onSave = viewModel::addMedicine
        )
    }
}

@Composable
private fun MedicineItem(medicine: Medicine, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = medicine.brandName)
                Text(text = "${medicine.genericName} • ${medicine.composition.ifBlank { medicine.manufacturer }}")
                if (medicine.strength.isNotBlank()) Text(text = "Strength: ${medicine.strength}")
                Text(text = "MRP: ${FormatUtils.formatCurrency(medicine.mrp)}")
                if (medicine.requiresPrescription) Text(text = "Rx • ${medicine.scheduleCategory}")
                medicine.barcode?.let { Text(text = "Barcode: $it") }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun AddMedicineDialog(onDismiss: () -> Unit, onSave: (Medicine) -> Unit) {
    var brandName by remember { mutableStateOf("") }
    var generic by remember { mutableStateOf("") }
    var composition by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("") }
    var dosageForm by remember { mutableStateOf("Tablet") }
    var manufacturer by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var requiresPrescription by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medicine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = brandName, onValueChange = { brandName = it }, label = { Text("Brand Name") })
                OutlinedTextField(value = generic, onValueChange = { generic = it }, label = { Text("Generic Name") })
                OutlinedTextField(value = composition, onValueChange = { composition = it }, label = { Text("Composition") })
                OutlinedTextField(value = strength, onValueChange = { strength = it }, label = { Text("Strength (e.g. 400 mg)") })
                OutlinedTextField(value = dosageForm, onValueChange = { dosageForm = it }, label = { Text("Dosage Form") })
                OutlinedTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = { Text("Manufacturer") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") })
                OutlinedTextField(value = mrp, onValueChange = { mrp = it }, label = { Text("MRP") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (brandName.isNotBlank()) {
                        onSave(
                            Medicine(
                                brandName = brandName,
                                genericName = generic.ifBlank { brandName },
                                composition = composition,
                                strength = strength,
                                dosageForm = dosageForm,
                                manufacturer = manufacturer,
                                category = category.ifBlank { "General" },
                                barcode = barcode.ifBlank { null },
                                mrp = mrp.toDoubleOrNull() ?: 0.0,
                                sellingPrice = mrp.toDoubleOrNull() ?: 0.0,
                                requiresPrescription = requiresPrescription
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
