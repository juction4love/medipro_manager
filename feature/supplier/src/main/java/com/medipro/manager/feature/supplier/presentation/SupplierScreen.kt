package com.medipro.manager.feature.supplier.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun SupplierScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Supplier Management",
        description = "Add, edit suppliers. Track outstanding payments.",
        onBack = onBack
    )
}
