package com.medipro.manager.feature.accounting.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.common.FormatUtils
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.Expense
import com.medipro.manager.domain.model.ExpenseCategories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val expensePaymentMethods = listOf("CASH", "CARD", "ESEWA", "KHALTI", "BANK")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
        state.successMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                navigationIcon = {
                    MediProTopBarNavigationIcon(
                        onBack = onOpenDrawer?.let { null } ?: onBack,
                        onOpenDrawer = onOpenDrawer,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = viewModel::saveExpense,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (state.isSaving) "Saving…" else "Record Expense",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Today's Expenses", fontWeight = FontWeight.SemiBold)
                        Text(
                            FormatUtils.formatCurrency(state.todayTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            item {
                Text("New Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategories.ALL.forEach { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.onCategoryChange(category) },
                            label = { Text(ExpenseCategories.label(category)) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Tea for staff, transport fare") },
                )
            }
            item {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount (Rs.)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    expensePaymentMethods.forEach { method ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { viewModel.onPaymentMethodChange(method) },
                            label = { Text(method.replace('_', ' ')) },
                        )
                    }
                }
            }
            item {
                Text("Today's Entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (state.expenses.isEmpty()) {
                item {
                    Text(
                        "No expenses recorded today",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.expenses) { expense ->
                    ExpenseRow(expense)
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expense.expenseDate))
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(ExpenseCategories.label(expense.category), fontWeight = FontWeight.SemiBold)
                Text(expense.description, style = MaterialTheme.typography.bodySmall)
                Text("$time · ${expense.paymentMethod}", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                FormatUtils.formatCurrency(expense.amount),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
