package com.medipro.manager.feature.settings.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medipro.manager.core.designsystem.component.MediProLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "1.1.34"
    }

    fun openUri(uri: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Us") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MediProLogo(size = 88.dp, modifier = Modifier.fillMaxWidth())
            Text(
                "MediPro v$appVersion",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            AboutSection(title = "Bimal Tech Solution") {
                Text(
                    "Bimal Tech Solution is a technology company dedicated to developing reliable, " +
                        "innovative, and user-friendly software solutions for businesses. We focus on " +
                        "building practical digital products that help organizations improve efficiency, " +
                        "productivity, and customer experience.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Our mission is to provide high-quality technology solutions with modern design, " +
                        "secure systems, and long-term support.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            AboutSection(title = "MediPro") {
                Text(
                    "MediPro is a professional pharmacy management solution designed to simplify daily " +
                        "pharmacy operations, including medicine management, inventory tracking, sales, " +
                        "reporting, and business workflow management.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "We continuously improve our products to meet the real needs of pharmacies and " +
                        "healthcare businesses.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            AboutSection(title = "Contact Us") {
                Text("Bimal Tech Solution", fontWeight = FontWeight.SemiBold)
                Text("Developed by: Bimal Lamichhane", style = MaterialTheme.typography.bodyMedium)
                ContactRow(label = "Mobile", value = "9855065327") {
                    openUri("tel:9855065327")
                }
                ContactRow(label = "Email", value = "bimal.lamichhane@gmail.com") {
                    openUri("mailto:bimal.lamichhane@gmail.com")
                }
                ContactRow(label = "Website", value = "www.bimalpharmacy.com.np") {
                    openUri("https://www.bimalpharmacy.com.np")
                }
            }

            Text(
                "Thank you for choosing Bimal Tech Solution.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ContactRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
