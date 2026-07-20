package com.medipro.manager.feature.notification.presentation

import androidx.compose.runtime.Composable
import com.medipro.manager.core.designsystem.component.FeaturePlaceholderScreen

@Composable
fun NotificationScreen(onBack: () -> Unit) {
    FeaturePlaceholderScreen(
        title = "Notifications",
        description = "Low stock and expiry alerts.",
        onBack = onBack
    )
}
