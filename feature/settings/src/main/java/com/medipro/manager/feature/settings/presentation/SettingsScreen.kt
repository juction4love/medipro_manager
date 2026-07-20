package com.medipro.manager.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit = {},
    onOpenPrinter: () -> Unit = {},
    onOpenOcrLearning: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenAppInfo: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (state.backupWarning) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Column {
                            Text(
                                "Backup overdue",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Last backup: ${state.lastBackupLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Text("Last backup: ${state.lastBackupLabel}")
                    }
                }
            }
            OutlinedTextField(
                value = state.pharmacyName,
                onValueChange = viewModel::onPharmacyNameChange,
                label = { Text("Pharmacy Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.pharmacyPhone,
                onValueChange = viewModel::onPharmacyPhoneChange,
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = state.pharmacyAddress,
                onValueChange = viewModel::onPharmacyAddressChange,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = onOpenOcrLearning,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("OCR Learning")
                }
            }
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Anonymous OCR feedback", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Opt-in only. Redacted bill samples help improve parser — no pharmacy name sent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.ocrFeedbackOptIn,
                        onCheckedChange = viewModel::onOcrFeedbackOptInChange,
                    )
                }
            }
            Button(
                onClick = onOpenPrinter,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Printer Settings")
                }
            }
            Button(
                onClick = onOpenBackup,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Backup & Restore")
            }
            Text(
                "App",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            SettingsNavButton("User Guide / Help", Icons.Default.HelpOutline, onOpenHelp)
            SettingsNavButton("License & Subscription", Icons.Default.VerifiedUser, onOpenLicense)
            SettingsNavButton("About Us", Icons.Default.Info, onOpenAbout)
            SettingsNavButton("App Information", Icons.Default.CardMembership, onOpenAppInfo)
        }
    }
}

@Composable
private fun SettingsNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}
