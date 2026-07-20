package com.medipro.manager.feature.customer.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun CustomerScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Customer Management",
        description = "Manage customers and credit sales.",
        onBack = onBack
    )
}
