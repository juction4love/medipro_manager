package com.medipro.manager.feature.license.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.domain.licensing.LicenseAccessState
import com.medipro.manager.domain.model.LicensePlan

private val subscriptionProFeatures = listOf(
    "OCR Purchase",
    "Cloud Backup",
    "Advanced Reports",
    "Multi-user",
    "Cloud Sync",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    appVersion: String,
    onBack: () -> Unit,
    onSubscribe: () -> Unit,
    onImportLicense: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val planLabel = when (state.accessState) {
        LicenseAccessState.VALID,
        LicenseAccessState.EXPIRING_SOON,
        -> LicensePlan.PRO
        LicenseAccessState.EXPIRED -> "EXPIRED"
        LicenseAccessState.NO_LICENSE -> LicensePlan.FREE
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediPro Pro") },
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
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "MediPro Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            subscriptionProFeatures.forEach { feature ->
                Text("✔ $feature", modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Current Plan", style = MaterialTheme.typography.labelLarge)
            Text(
                planLabel,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            state.validUntil?.takeIf { planLabel == LicensePlan.PRO }?.let { expiry ->
                Text("Valid until $expiry", style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "Manual billing, sales, stock & returns stay free — upgrade only for Pro productivity features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Upgrade to Pro") }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Already have a license?", style = MaterialTheme.typography.bodyMedium)

            OutlinedButton(
                onClick = onImportLicense,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import License") }

            Text(
                "Version $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
