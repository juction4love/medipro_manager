package com.medipro.manager.feature.purchase.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.domain.model.Purchase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    onBack: () -> Unit,
    onReturnPurchase: (Long) -> Unit,
    viewModel: PurchaseHistoryViewModel = hiltViewModel(),
) {
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (purchases.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No purchases yet")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(purchases, key = { it.id }) { purchase ->
                    PurchaseHistoryCard(
                        purchase = purchase,
                        onReturn = { onReturnPurchase(purchase.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseHistoryCard(
    purchase: Purchase,
    onReturn: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(purchase.invoiceNumber, style = MaterialTheme.typography.titleSmall)
                    Text(purchase.supplierName.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(Date(purchase.purchaseDate)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        FormatUtils.formatCurrency(purchase.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = onReturn) {
                    Text("Return")
                }
            }
        }
    }
}
