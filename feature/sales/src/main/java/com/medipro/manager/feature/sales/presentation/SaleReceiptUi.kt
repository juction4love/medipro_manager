package com.medipro.manager.feature.sales.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medipro.manager.core.common.FormatUtils

data class CompletedSaleReceipt(
    val saleId: Long,
    val invoiceNumber: String,
    val totalAmount: Double,
    val pdfPath: String,
)

@Composable
fun SaleCompletedDialog(
    receipt: CompletedSaleReceipt,
    onPrint: () -> Unit,
    onSavePdf: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    "Sale Successful",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    receipt.invoiceNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    FormatUtils.formatCurrency(receipt.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                )
                ReceiptActionButton(emoji = "🖨", label = "Print", onClick = onPrint)
                ReceiptActionButton(emoji = "📄", label = "Save PDF", onClick = onSavePdf)
                ReceiptActionButton(emoji = "💬", label = "WhatsApp", onClick = onWhatsApp)
                ReceiptActionButton(emoji = "📤", label = "Share", onClick = onShare)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        },
        dismissButton = {},
    )
}

@Composable
fun InvoiceActionButtons(
    pdfPath: String?,
    isGenerating: Boolean,
    onPrint: () -> Unit,
    onPdf: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isGenerating) {
            Text("Generating PDF…", style = MaterialTheme.typography.bodySmall)
        }
        ReceiptActionButton(emoji = "🖨", label = "Print", onClick = onPrint, enabled = !isGenerating)
        ReceiptActionButton(
            emoji = "📄",
            label = "PDF",
            onClick = onPdf,
            enabled = !isGenerating && pdfPath != null,
        )
        ReceiptActionButton(
            emoji = "💬",
            label = "WhatsApp",
            onClick = onWhatsApp,
            enabled = !isGenerating && pdfPath != null,
        )
        ReceiptActionButton(
            emoji = "📤",
            label = "Share",
            onClick = onShare,
            enabled = !isGenerating && pdfPath != null,
        )
    }
}

@Composable
private fun ReceiptActionButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(emoji, modifier = Modifier.padding(end = 12.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ReceiptActionButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        icon()
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
