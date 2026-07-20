package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medipro.manager.domain.model.BillScanProgress

@Composable
fun BillScanProgressOverlay(progress: BillScanProgress?) {
    if (progress == null) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Reading bill…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(progress.displayLabel(), style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(
                    progress = { progress.progressFraction.coerceAtLeast(0.05f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
