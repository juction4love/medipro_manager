package com.medipro.manager.feature.expiry.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun ExpiryScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Expiring Medicines",
        description = "Track batches nearing expiry and take action.",
        onBack = onBack
    )
}
