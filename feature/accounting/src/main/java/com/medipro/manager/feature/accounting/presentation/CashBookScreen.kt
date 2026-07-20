package com.medipro.manager.feature.accounting.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.CashBookEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: CashBookViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CashBookEvent.ShareExport -> {
                    val file = File(event.path)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Cash Book"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Book") },
                navigationIcon = {
                    MediProTopBarNavigationIcon(
                        onBack = onOpenDrawer?.let { null } ?: onBack,
                        onOpenDrawer = onOpenDrawer,
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::exportCsv, enabled = !state.isExporting) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export CSV")
                    }
                    IconButton(onClick = viewModel::exportPdf, enabled = !state.isExporting) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Today", fontWeight = FontWeight.SemiBold)
                        SummaryRow("Cash In", FormatUtils.formatCurrency(state.totalIn))
                        SummaryRow("Cash Out", FormatUtils.formatCurrency(state.totalOut))
                        HorizontalDivider()
                        SummaryRow(
                            "Closing Balance",
                            FormatUtils.formatCurrency(state.closingBalance),
                            bold = true,
                        )
                    }
                }
            }
            item {
                Text("Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        "No cash entries today",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.entries) { entry ->
                    CashBookEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun CashBookEntryRow(entry: CashBookEntry) {
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(entry.entryDate))
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(entry.description, fontWeight = FontWeight.Medium)
                Text(
                    "$time${entry.referenceType?.let { " · $it" }.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                if (entry.cashIn > 0) {
                    Text(
                        "+ ${FormatUtils.formatCurrency(entry.cashIn)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (entry.cashOut > 0) {
                    Text(
                        "- ${FormatUtils.formatCurrency(entry.cashOut)}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
