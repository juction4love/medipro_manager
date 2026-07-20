package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.model.MrpUpdateChoice
import com.medipro.manager.domain.model.PurchaseBillMatchStatus
import com.medipro.manager.domain.model.ScannedPurchaseBill

@Composable
fun PurchaseBillReviewSheet(
    review: ScannedPurchaseBill,
    selectedLineIndexes: Set<Int>,
    isApplying: Boolean,
    duplicateDismissed: Boolean,
    mrpUpdateChoices: Map<Int, MrpUpdateChoice>,
    controlledVerificationAcknowledged: Boolean,
    onToggleLine: (Int) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onCreateSupplier: () -> Unit,
    onCreateMedicine: (Int) -> Unit,
    onManualMatch: (Int) -> Unit,
    onMrpChoice: (Int, MrpUpdateChoice) -> Unit,
    onAcknowledgeControlled: () -> Unit,
    onOpenExistingPurchase: (Long) -> Unit,
    onDismissDuplicate: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Review Scanned Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Confirm before import — OCR never saves directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    PurchaseBillSummaryCard(review, selectedLineIndexes)

                    SupplierMatchRow(
                        supplierName = review.supplierName,
                        matchedName = review.matchedSupplierName,
                        found = review.supplierFound,
                        onCreateSupplier = onCreateSupplier,
                    )

                    if (review.controlledCount > 0 && !controlledVerificationAcknowledged) {
                        ControlledMedicineBanner(onAcknowledge = onAcknowledgeControlled)
                    }

                    review.duplicateBill?.takeIf { !duplicateDismissed }?.let { dup ->
                        DuplicateBillBanner(
                            billNumber = dup.supplierBillNumber,
                            existingInvoice = dup.internalInvoiceNumber,
                            onOpenExisting = { onOpenExistingPurchase(dup.purchaseId) },
                            onImportAgain = onDismissDuplicate,
                        )
                    }

                    review.parseWarnings.forEach { warning ->
                        Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = review.lines,
                        key = { index, line -> "${index}_${line.parsed.batchNumber}_${line.parsed.description}" },
                    ) { index, line ->
                        BillLineReviewCard(
                            line = line,
                            selected = index in selectedLineIndexes,
                            mrpChoice = mrpUpdateChoices[index],
                            onToggle = { onToggleLine(index) },
                            onCreateMedicine = { onCreateMedicine(index) },
                            onManualMatch = { onManualMatch(index) },
                            onMrpChoice = { choice -> onMrpChoice(index, choice) },
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isApplying) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        enabled = selectedLineIndexes.isNotEmpty() && !isApplying &&
                            (review.controlledCount == 0 || controlledVerificationAcknowledged),
                    ) {
                        Text(
                            if (isApplying) {
                                "Importing…"
                            } else {
                                "Import ${selectedLineIndexes.size} Items"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseBillSummaryCard(review: ScannedPurchaseBill, selected: Set<Int>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            review.supplierName?.let { Text("Supplier: $it", fontWeight = FontWeight.SemiBold) }
            review.invoiceNumber?.let { Text("Invoice: $it") }
            Text("Items: ${review.itemCount} · Parser: ${review.parserUsed}" + if (review.pageCount > 1) " · ${review.pageCount} pages" else "")
            Text("Matched: ${review.matchedCount} · Review: ${review.reviewCount} · Unknown: ${review.unmatchedCount}")
            val total = review.estimatedImportTotal(selected).takeIf { it > 0 } ?: review.netTotal
            total?.let {
                Text("Total: ${FormatUtils.formatCurrency(it)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SupplierMatchRow(
    supplierName: String?,
    matchedName: String?,
    found: Boolean,
    onCreateSupplier: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (found) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Supplier", style = MaterialTheme.typography.labelMedium)
                Text(supplierName ?: "Not detected", fontWeight = FontWeight.SemiBold)
                if (found) {
                    Text("✓ Existing supplier: $matchedName", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Supplier not found", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!found && !supplierName.isNullOrBlank()) {
                TextButton(onClick = onCreateSupplier) { Text("Create Supplier") }
            }
        }
    }
}

@Composable
private fun DuplicateBillBanner(
    billNumber: String,
    existingInvoice: String,
    onOpenExisting: () -> Unit,
    onImportAgain: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Invoice already imported: $billNumber", fontWeight = FontWeight.SemiBold)
            Text("Existing: $existingInvoice", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenExisting) { Text("Open Existing") }
                TextButton(onClick = onImportAgain) { Text("Import Again") }
            }
        }
    }
}

@Composable
private fun ControlledMedicineBanner(onAcknowledge: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Controlled medicines detected", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Text(
                "Verify doctor license, batch number, and expiry before import.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onAcknowledge) { Text("I have verified") }
        }
    }
}

@Composable
private fun BillLineReviewCard(
    line: com.medipro.manager.domain.model.PurchaseBillLineMatch,
    selected: Boolean,
    mrpChoice: MrpUpdateChoice?,
    onToggle: () -> Unit,
    onCreateMedicine: () -> Unit,
    onManualMatch: () -> Unit,
    onMrpChoice: (MrpUpdateChoice) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (line.status) {
                PurchaseBillMatchStatus.MATCHED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                PurchaseBillMatchStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                PurchaseBillMatchStatus.UNMATCHED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                PurchaseBillMatchStatus.FREE_ITEM -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f)
            },
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(line.status)
                    Text(line.parsed.description, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    ConfidenceBadge(line.status, line.confidence)
                }
                line.match?.brandName?.let { matched ->
                    Text("→ $matched", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (line.matchedViaAlias) {
                    Text("Learned match (99%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    "Batch ${line.parsed.batchNumber} · Qty ${line.parsed.quantity} · ${FormatUtils.formatCurrency(line.parsed.unitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Exp ${line.parsed.expiryRaw}", style = MaterialTheme.typography.bodySmall)
                    if (line.nearExpiry) {
                        Text("🔴 Near Expiry", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (line.expiryNeedsReview) {
                        Text("⚠ Check expiry manually", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (line.mrpChanged) {
                    MrpDifferenceRow(
                        ocrMrp = line.parsed.mrp,
                        databaseMrp = line.databaseMrp,
                        choice = mrpChoice ?: MrpUpdateChoice.KEEP,
                        onChoice = onMrpChoice,
                    )
                }
                line.costIncreasePercent?.let { pct ->
                    Text(
                        "⚠ Cost ${pct.toInt()}% increased (was ${FormatUtils.formatCurrency(line.previousUnitPrice ?: 0.0)})",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (line.isControlled) {
                    Text(
                        "Controlled drug — verify batch, expiry & license",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (line.status == PurchaseBillMatchStatus.UNMATCHED || line.status == PurchaseBillMatchStatus.NEEDS_REVIEW) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (line.status == PurchaseBillMatchStatus.UNMATCHED) {
                            TextButton(onClick = onCreateMedicine) { Text("Create Medicine") }
                        }
                        TextButton(onClick = onManualMatch) { Text("Link Medicine") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MrpDifferenceRow(
    ocrMrp: Double,
    databaseMrp: Double?,
    choice: MrpUpdateChoice,
    onChoice: (MrpUpdateChoice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "⚠ MRP changed — bill ${FormatUtils.formatCurrency(ocrMrp)} vs DB ${FormatUtils.formatCurrency(databaseMrp ?: 0.0)}",
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onChoice(MrpUpdateChoice.UPDATE) }) {
                Text(if (choice == MrpUpdateChoice.UPDATE) "✓ Update MRP" else "Update MRP")
            }
            TextButton(onClick = { onChoice(MrpUpdateChoice.KEEP) }) {
                Text(if (choice == MrpUpdateChoice.KEEP) "✓ Keep Existing" else "Keep Existing")
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(status: PurchaseBillMatchStatus, confidence: Int) {
    val (label, color) = when (status) {
        PurchaseBillMatchStatus.MATCHED -> "✓ $confidence%" to MaterialTheme.colorScheme.primary
        PurchaseBillMatchStatus.NEEDS_REVIEW -> "⚠ $confidence%" to MaterialTheme.colorScheme.tertiary
        PurchaseBillMatchStatus.UNMATCHED -> "✗ $confidence%" to MaterialTheme.colorScheme.error
        PurchaseBillMatchStatus.FREE_ITEM -> "FREE" to MaterialTheme.colorScheme.secondary
    }
    Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusIcon(status: PurchaseBillMatchStatus) {
    val (icon, tint) = when (status) {
        PurchaseBillMatchStatus.MATCHED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        PurchaseBillMatchStatus.NEEDS_REVIEW -> Icons.Default.Help to MaterialTheme.colorScheme.tertiary
        PurchaseBillMatchStatus.UNMATCHED -> Icons.Default.Error to MaterialTheme.colorScheme.error
        PurchaseBillMatchStatus.FREE_ITEM -> Icons.Default.Warning to MaterialTheme.colorScheme.secondary
    }
    Icon(icon, contentDescription = null, tint = tint)
}
