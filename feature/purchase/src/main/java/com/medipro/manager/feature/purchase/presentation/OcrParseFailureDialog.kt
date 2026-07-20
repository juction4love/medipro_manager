package com.medipro.manager.feature.purchase.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import android.net.Uri

@Composable
fun OcrParseFailureDialog(
    imageUris: List<String>,
    ocrText: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onImproveParser: () -> Unit = {},
    feedbackOptInHint: String? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unknown Bill Format") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Could not parse line items from this bill. You can share the image or send anonymous redacted OCR to improve the parser.")
                feedbackOptInHint?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
                ocrText?.take(120)?.let { preview ->
                    Text("OCR preview: ${preview.trim()}…")
                }
            }
        },
        confirmButton = {
            Button(onClick = onRetry) { Text("Scan Again") }
        },
        dismissButton = {
            Column {
                OutlinedButton(
                    onClick = {
                        val shareUris = imageUris.mapNotNull { uriString ->
                            runCatching { Uri.parse(uriString) }.getOrNull()
                        }
                        if (shareUris.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "image/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareUris))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Send bill image"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send Image") }
                OutlinedButton(
                    onClick = onImproveParser,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Improve Parser (Anonymous)") }
                TextButton(
                    onClick = {
                        ocrText?.let { clipboard.setText(AnnotatedString(it)) }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copy OCR Text") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        },
    )
}
