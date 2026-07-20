package com.medipro.manager.feature.scanner.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun ScannerScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Barcode & Bill Scanner",
        description = "Barcode scan is available in Sales & Purchase. Supplier bill OCR (camera) is on Purchase → Scan Bill.",
        onBack = onBack
    )
}
