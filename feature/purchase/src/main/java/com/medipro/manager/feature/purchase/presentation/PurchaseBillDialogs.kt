package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medipro.manager.domain.model.OcrMedicineDraft

@Composable
fun CreateMedicineFromBillDialog(
    draft: OcrMedicineDraft,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (OcrMedicineDraft) -> Unit,
) {
    var brand by remember(draft) { mutableStateOf(draft.brandName) }
    var generic by remember(draft) { mutableStateOf(draft.genericName) }
    var strength by remember(draft) { mutableStateOf(draft.strength) }
    var form by remember(draft) { mutableStateOf(draft.dosageForm) }
    var company by remember(draft) { mutableStateOf(draft.manufacturer) }
    var unit by remember(draft) { mutableStateOf(draft.unit) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Create Medicine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OCR pre-filled — verify and save.")
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = generic, onValueChange = { generic = it }, label = { Text("Generic") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = strength, onValueChange = { strength = it }, label = { Text("Strength") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = form, onValueChange = { form = it }, label = { Text("Form") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        draft.copy(
                            brandName = brand.trim(),
                            genericName = generic.trim(),
                            strength = strength.trim(),
                            dosageForm = form.trim(),
                            manufacturer = company.trim(),
                            unit = unit.trim(),
                        ),
                    )
                },
                enabled = brand.isNotBlank() && !isSaving,
            ) { Text(if (isSaving) "Saving…" else "Save Medicine") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}

@Composable
fun CreateSupplierFromBillDialog(
    supplierName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(supplierName) { mutableStateOf(supplierName) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Create Supplier") },
        text = {
            Column {
                Text("Supplier not found in your list. Create to auto-match next time.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank() && !isSaving) {
                Text(if (isSaving) "Saving…" else "Create Supplier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        },
    )
}
