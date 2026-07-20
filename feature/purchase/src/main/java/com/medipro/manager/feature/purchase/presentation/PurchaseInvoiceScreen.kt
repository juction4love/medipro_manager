package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.model.Purchase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseInvoiceScreen(
    invoiceNumber: String,
    onBack: () -> Unit,
    onReturnPurchase: (Long) -> Unit,
    viewModel: PurchaseInvoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(invoiceNumber) {
        viewModel.load(invoiceNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase Bill") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.purchase == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Purchase bill not found", style = MaterialTheme.typography.titleMedium)
                    Text(
                        invoiceNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                PurchaseInvoiceContent(
                    purchase = state.purchase!!,
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    onReturnPurchase = onReturnPurchase,
                )
            }
        }
    }
}

@Composable
private fun PurchaseInvoiceContent(
    purchase: Purchase,
    modifier: Modifier = Modifier,
    onReturnPurchase: (Long) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(purchase.invoiceNumber, style = MaterialTheme.typography.headlineSmall)
                Text(FormatUtils.formatDate(purchase.purchaseDate), style = MaterialTheme.typography.bodyMedium)
                purchase.supplierName?.let {
                    Text("Supplier: $it", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = FormatUtils.formatCurrency(purchase.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("${purchase.items.size} item(s)", style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(
            onClick = { onReturnPurchase(purchase.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Process Return")
        }
    }
}
