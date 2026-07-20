package com.medipro.manager.feature.sales.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.common.InvoiceShareActions
import com.medipro.manager.core.common.InvoiceShareTarget
import com.medipro.manager.domain.model.Sale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleHistoryScreen(
    onBack: () -> Unit,
    onReturnSale: (Long) -> Unit,
    onOpenInvoice: (String) -> Unit = {},
    viewModel: SaleHistoryViewModel = hiltViewModel(),
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (sales.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No sales yet")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sales, key = { it.id }) { sale ->
                    SwipeableSaleHistoryCard(
                        sale = sale,
                        onOpen = { onOpenInvoice(sale.invoiceNumber) },
                        onReturn = { onReturnSale(sale.id) },
                        onShare = {
                            viewModel.ensurePdfPath(sale) { path ->
                                InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.SHARE_SHEET)
                            }
                        },
                        onWhatsApp = {
                            viewModel.ensurePdfPath(sale) { path ->
                                InvoiceShareActions.sharePdf(context, path, InvoiceShareTarget.WHATSAPP)
                            }
                        },
                        onPrint = {
                            viewModel.printInvoice(sale) { result ->
                                InvoiceShareActions.notifyThermalResult(context, result)
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSaleHistoryCard(
    sale: Sale,
    onOpen: () -> Unit,
    onReturn: () -> Unit,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
    onPrint: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { false },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onReturn) {
                    Icon(Icons.Default.Undo, contentDescription = "Return")
                }
                IconButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                IconButton(onClick = onWhatsApp) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "WhatsApp PDF")
                }
            }
        },
    ) {
        SaleHistoryCard(sale = sale, onOpen = onOpen)
    }
}

@Composable
private fun SaleHistoryCard(
    sale: Sale,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(sale.invoiceNumber, style = MaterialTheme.typography.titleSmall)
            sale.customerName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sale.saleDate)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(FormatUtils.formatCurrency(sale.totalAmount), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Swipe right for actions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
