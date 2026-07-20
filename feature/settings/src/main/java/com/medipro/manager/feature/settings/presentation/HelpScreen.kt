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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val query = searchQuery.trim()
    val filtered = remember(query) {
        if (query.length < 2) {
            MediProHelpSections.sections
        } else {
            MediProHelpSections.sections.filter { it.matchesQuery(query) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help / User Guide") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HelpIntroCard()
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search guide") },
                placeholder = { Text("sale, backup, batch, OTP…") },
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                Text(
                    "No matching topics for \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                filtered.forEach { section ->
                    HelpSectionCard(section)
                }
            }
            HelpSupportCard()
            Text(
                "MediPro v1.1.34 · Offline guide · PDF: docs/help/MediPro-Staff-Guide.html",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun HelpIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "MediPro Staff Guide — छिटो सन्दर्भ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Activation → purchase → sale → return/adjustment → reports → backup. " +
                    "Posted invoice cancel गर्न मिल्दैन — Process Return प्रयोग गर्नुहोस्.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HelpSectionCard(section: HelpSection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(section.titleNe, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(section.titleEn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            section.steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
            }
            section.tips?.let { tips ->
                Text(
                    tips,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HelpSupportCard() {
    val context = LocalContext.current
    fun openUri(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Support / सम्पर्क", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(MediProSupportInfo.COMPANY, fontWeight = FontWeight.SemiBold)
            Text("Developer: ${MediProSupportInfo.DEVELOPER}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { openUri("tel:${MediProSupportInfo.MOBILE}") }) {
                Text("Mobile: ${MediProSupportInfo.MOBILE}")
            }
            TextButton(onClick = { openUri("mailto:${MediProSupportInfo.EMAIL}") }) {
                Text("Email: ${MediProSupportInfo.EMAIL}")
            }
            TextButton(onClick = { openUri(MediProSupportInfo.WEBSITE) }) {
                Text("Website: ${MediProSupportInfo.WEBSITE_LABEL}")
            }
            Text(
                "License, device change, backup recovery, वा training को लागि सम्पर्क गर्नुहोस्।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class HelpSection(
    val id: String,
    val titleNe: String,
    val titleEn: String,
    val steps: List<String>,
    val tips: String? = null,
    val keywords: List<String> = emptyList(),
) {
    fun matchesQuery(query: String): Boolean {
        val q = query.lowercase()
        return titleNe.lowercase().contains(q) ||
            titleEn.lowercase().contains(q) ||
            keywords.any { it.lowercase().contains(q) } ||
            steps.any { it.lowercase().contains(q) } ||
            tips?.lowercase()?.contains(q) == true
    }
}

object MediProHelpSections {
    val sections: List<HelpSection> = listOf(
        HelpSection(
            id = "login",
            titleNe = "१. App Login / OTP (सक्रियकरण)",
            titleEn = "Login & Activation",
            keywords = listOf("otp", "license", "pin", "activate"),
            steps = listOf(
                "पहिलो पटक app खोल्दा Activate MediPro — registered mobile number राख्नुहोस्।",
                "OTP verify (Resend OTP available)।",
                "Pharmacy Details: नाम, ठेगाना, PAN → Activate।",
                "App Lock PIN optional — पछि प्रवेशमा PIN।",
                "License समाप्त: Settings → License & Subscription।",
            ),
            tips = "💡 Device change → support सम्पर्क (device ID bound)।",
        ),
        HelpSection(
            id = "dashboard",
            titleNe = "२. Dashboard",
            titleEn = "Dashboard Overview",
            keywords = listOf("home", "alerts", "summary"),
            steps = listOf(
                "तल Dashboard tab — दैनिक summary cards (sales, stock alerts)।",
                "Recent sales र purchases quick view।",
                "Low stock / expiry alerts हेर्नुहोस्।",
                "Drawer बाट Accounting, Settings, Help खोल्न सकिन्छ।",
            ),
        ),
        HelpSection(
            id = "medicine_search",
            titleNe = "३. Medicine Search",
            titleEn = "Medicine Search",
            keywords = listOf("catalog", "brand", "generic", "global search"),
            steps = listOf(
                "Sales/Purchase/Inventory मा search: brand, generic, composition, barcode।",
                "Top bar Global Search — medicines, invoices, customers (where available)।",
                "Medicine module (Drawer): catalog view र medicine master।",
                "Search परिणाममा stock, MRP, batch preview देखिन्छ।",
            ),
        ),
        HelpSection(
            id = "purchase",
            titleNe = "४. Purchase (खरिद)",
            titleEn = "Purchase",
            keywords = listOf("supplier", "ocr", "batch", "expiry"),
            steps = listOf(
                "Purchase tab → search वा barcode scan।",
                "Batch No, Expiry (yyyy-MM-dd), Qty, Cost अनिवार्य।",
                "Supplier select — Create Supplier (Purchase screen)।",
                "OCR bill scan → line import (expiry validated)।",
                "Confirm → batch + stock update।",
            ),
            tips = "💡 Supplier menu placeholder — Purchase workflow use गर्नुहोस्।",
        ),
        HelpSection(
            id = "sales",
            titleNe = "५. Sales (बिक्री / Counter)",
            titleEn = "Pharmacy Counter Sale",
            keywords = listOf("billing", "invoice", "cart", "payment", "credit"),
            steps = listOf(
                "Sales tab → search वा barcode scan → cart।",
                "Qty, discount, payment method, customer (credit)।",
                "Rx medicine: prescription details required।",
                "Complete Sale → PDF / thermal print / WhatsApp share।",
                "History icon → past invoices।",
            ),
            tips = "💡 Cart clear before checkout = pre-commit cancel।",
        ),
        HelpSection(
            id = "batch_selection",
            titleNe = "६. Batch Selection (Manual Sale)",
            titleEn = "Batch Selection & FEFO",
            keywords = listOf("fefo", "expiry", "batch picker", "multiple batch"),
            steps = listOf(
                "Manual add: धेरै batch भए Select Batch dialog खुल्छ।",
                "Batch table: Batch No, Expiry, Stock — expiry ASC (FEFO order)।",
                "Auto FEFO button वा batch छानेर Select।",
                "Single batch भए automatic FEFO select।",
            ),
        ),
        HelpSection(
            id = "barcode",
            titleNe = "७. Barcode Scan",
            titleEn = "Barcode Scanning",
            keywords = listOf("scan", "camera", "qr"),
            steps = listOf(
                "Sales/Purchase search bar को scan icon → camera permission allow।",
                "Medicine barcode scan → auto add (Sales: FEFO batch, no picker)।",
                "Numeric barcode (6+ digits) search field मा paste/submit पनि मिल्छ।",
                "Return lookup: invoice number scan/type।",
            ),
        ),
        HelpSection(
            id = "sales_return",
            titleNe = "८. Sales Return (फिर्ता)",
            titleEn = "Sales Return",
            keywords = listOf("return", "refund", "reverse"),
            steps = listOf(
                "Sales History / Invoice → ↩ Process Return।",
                "Items + qty + reason → Confirm।",
                "Stock, payment, ledger reverse + audit trail।",
                "Posted invoice cancel disabled — Return नै workflow।",
            ),
        ),
        HelpSection(
            id = "adjustment",
            titleNe = "९. Stock Adjustment",
            titleEn = "Inventory Adjustment",
            keywords = listOf("expired", "damage", "lost", "count"),
            steps = listOf(
                "Inventory → medicine → batch → Adjust।",
                "Type: Expired, Damage, Lost, Physical Count, Manual Correction।",
                "Qty + Reason → Save → Adjustments history tab।",
            ),
            tips = "💡 Lost = physical verification missing stock।",
        ),
        HelpSection(
            id = "reports",
            titleNe = "१०. Reports",
            titleEn = "Reports & Accounting",
            keywords = listOf("report", "day closing", "ledger", "accounting"),
            steps = listOf(
                "Reports tab — sales/purchase summaries।",
                "Drawer → Accounting: customer receipts, supplier payments, expenses।",
                "Day Closing — cash reconciliation (Accounting → Day Closing)।",
                "Inventory Reports tab — damage/expiry reports।",
            ),
        ),
        HelpSection(
            id = "backup",
            titleNe = "११. Backup",
            titleEn = "Backup",
            keywords = listOf("encrypt", "medipro file"),
            steps = listOf(
                "Settings → Backup & Restore → strong password।",
                "Create Backup → .medipro encrypted (AES-256-GCM)।",
                "Safe storage (Drive/PC) — weekly habit।",
            ),
        ),
        HelpSection(
            id = "restore",
            titleNe = "१२. Restore",
            titleEn = "Restore",
            keywords = listOf("recover", "import"),
            steps = listOf(
                "Restore Backup → .medipro file + password।",
                "⚠️ Current data overwrite — restore अघि backup लिनुहोस्।",
            ),
        ),
        HelpSection(
            id = "about",
            titleNe = "१३. About & App Information",
            titleEn = "About",
            keywords = listOf("version", "device id"),
            steps = listOf(
                "Settings → About Us — company र MediPro info।",
                "Settings → App Information — version, package, Device ID।",
                "Support को लागi यो Help को Contact section हेर्नुहोस्।",
            ),
        ),
    )
}
