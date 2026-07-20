package com.medipro.manager.feature.settings.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.designsystem.navigation.MediProTopBarNavigationIcon
import com.medipro.manager.domain.model.PrinterProfile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrinterSettingsScreen(
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: PrinterSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val settings = state.settings

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshDevices() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                ),
            )
        } else {
            viewModel.refreshDevices()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer") },
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
                onClick = viewModel::runTestPrint,
                enabled = !state.isTesting && settings.isConfigured,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(if (state.isTesting) "Printing…" else "Test Print")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrinterProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = state.selectedProfile == profile,
                        onClick = { viewModel.selectProfile(profile) },
                        label = { Text(profile.label) },
                    )
                }
            }

            OutlinedTextField(
                value = settings.printerName,
                onValueChange = viewModel::onPrinterNameChange,
                label = { Text("Printer Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = settings.macAddress,
                onValueChange = viewModel::onMacAddressChange,
                label = { Text("MAC Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            )

            if (state.pairedDevices.isNotEmpty()) {
                Text("Paired Bluetooth Devices", fontWeight = FontWeight.SemiBold)
                state.pairedDevices.forEach { (name, address) ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.selectDevice(name, address) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(name, fontWeight = FontWeight.Medium)
                            Text(address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(onClick = viewModel::refreshDevices) { Text("Refresh devices") }
            }

            HorizontalDivider()
            Text("Paper Size", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.paperWidthMm == 58,
                    onClick = { viewModel.onPaperWidthChange(58) },
                    label = { Text("58 mm") },
                )
                FilterChip(
                    selected = settings.paperWidthMm == 80,
                    onClick = { viewModel.onPaperWidthChange(80) },
                    label = { Text("80 mm") },
                )
            }

            Text("Characters per Line", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(32, 42, 48).forEach { chars ->
                    FilterChip(
                        selected = settings.charsPerLine == chars,
                        onClick = { viewModel.onCharsPerLineChange(chars) },
                        label = { Text(chars.toString()) },
                    )
                }
            }

            ToggleRow("Auto Connect", settings.autoConnect, viewModel::onAutoConnectChange)
            ToggleRow("Auto Cut", settings.autoCut, viewModel::onAutoCutChange)
            ToggleRow("Open Cash Drawer", settings.openCashDrawer, viewModel::onOpenDrawerChange)
            ToggleRow("Print Logo (pharmacy name)", settings.printLogo, viewModel::onPrintLogoChange)
            ToggleRow("Print Duplicate Copy", settings.printDuplicateCopy, viewModel::onDuplicateCopyChange)

            HorizontalDivider()
            Text(
                "Receipt includes pharmacy header, line items with batch/expiry, totals, payment, Thank You, and QR (medipro://invoice/…).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
