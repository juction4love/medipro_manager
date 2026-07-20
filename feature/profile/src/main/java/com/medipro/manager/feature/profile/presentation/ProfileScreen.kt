package com.medipro.manager.feature.profile.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Pharmacy Profile",
        description = "Pharmacy profile and business details.",
        onBack = onBack
    )
}
