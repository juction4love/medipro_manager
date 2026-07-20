package com.medipro.manager.feature.accounting.presentation

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon

data class AccountingMenuItem(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
)

private val accountingMenuItems = listOf(
    AccountingMenuItem(
        "Customer Receipts",
        "Collect due from customers",
        "customer_receipts",
        Icons.Default.People,
    ),
    AccountingMenuItem(
        "Supplier Payments",
        "Pay supplier outstanding bills",
        "supplier_payments",
        Icons.Default.Store,
    ),
    AccountingMenuItem(
        "Cash Book",
        "Daily cash in and out",
        "cash_book",
        Icons.Default.Payments,
    ),
    AccountingMenuItem(
        "Ledger",
        "Account balances and entries",
        "ledger",
        Icons.AutoMirrored.Filled.ReceiptLong,
    ),
    AccountingMenuItem(
        "Expenses",
        "Daily shop expenses — tea, transport, misc",
        "expenses",
        Icons.Default.Receipt,
    ),
    AccountingMenuItem(
        "Day Closing",
        "End-of-day sales, cash & difference",
        "day_closing",
        Icons.Default.CalendarMonth,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounting") },
                navigationIcon = {
                    MediProTopBarNavigationIcon(
                        onBack = onOpenDrawer?.let { null } ?: onBack,
                        onOpenDrawer = onOpenDrawer,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Payments, cash book, and day closing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(accountingMenuItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(item.route) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountingPlaceholderScreen(
    title: String,
    description: String,
    onBack: () -> Unit,
) {
    FeaturePlaceholderScreen(title = title, description = description, onBack = onBack)
}
