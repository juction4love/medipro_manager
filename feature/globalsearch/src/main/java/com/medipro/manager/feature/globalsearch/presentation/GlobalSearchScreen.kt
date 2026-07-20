package com.medipro.manager.feature.globalsearch.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.domain.model.GlobalSearchCategory
import com.medipro.manager.domain.model.GlobalSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onResultClick: (GlobalSearchResult) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Medicines, customers, invoices…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.query.trim().length < 2 -> {
                    SearchHint(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    )
                }
                state.hasSearched && state.results.isEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No results for \"${state.query.trim()}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    GlobalSearchResults(
                        results = state.results,
                        onResultClick = onResultClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Search everything from one place",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Type at least 2 characters to search:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(
            "Medicines (brand, generic, barcode)",
            "Customers (name, phone)",
            "Suppliers (name, phone, PAN)",
            "Sales invoices",
            "Purchase invoices",
        ).forEach { hint ->
            Text(
                text = "• $hint",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun GlobalSearchResults(
    results: com.medipro.manager.domain.model.GlobalSearchResponse,
    onResultClick: (GlobalSearchResult) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (results.medicines.isNotEmpty()) {
            item { SearchSectionHeader("Medicines", results.medicines.size) }
            items(results.medicines, key = { "medicine-${it.id}" }) { result ->
                SearchResultRow(result, Icons.Default.Inventory2, onResultClick)
            }
        }
        if (results.customers.isNotEmpty()) {
            item { SearchSectionHeader("Customers", results.customers.size) }
            items(results.customers, key = { "customer-${it.id}" }) { result ->
                SearchResultRow(result, Icons.Default.People, onResultClick)
            }
        }
        if (results.suppliers.isNotEmpty()) {
            item { SearchSectionHeader("Suppliers", results.suppliers.size) }
            items(results.suppliers, key = { "supplier-${it.id}" }) { result ->
                SearchResultRow(result, Icons.Default.Store, onResultClick)
            }
        }
        if (results.sales.isNotEmpty()) {
            item { SearchSectionHeader("Sales Invoices", results.sales.size) }
            items(results.sales, key = { "sale-${it.id}" }) { result ->
                SearchResultRow(result, Icons.Default.Receipt, onResultClick)
            }
        }
        if (results.purchases.isNotEmpty()) {
            item { SearchSectionHeader("Purchase Invoices", results.purchases.size) }
            items(results.purchases, key = { "purchase-${it.id}" }) { result ->
                SearchResultRow(result, Icons.Default.ShoppingCart, onResultClick)
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SearchResultRow(
    result: GlobalSearchResult,
    icon: ImageVector,
    onClick: (GlobalSearchResult) -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(result) },
        headlineContent = {
            Text(
                text = result.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = result.subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = categoryLabel(result.category),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

private fun categoryLabel(category: GlobalSearchCategory): String = when (category) {
    GlobalSearchCategory.MEDICINE -> "Medicine"
    GlobalSearchCategory.CUSTOMER -> "Customer"
    GlobalSearchCategory.SUPPLIER -> "Supplier"
    GlobalSearchCategory.SALE_INVOICE -> "Sales invoice"
    GlobalSearchCategory.PURCHASE_INVOICE -> "Purchase invoice"
}
