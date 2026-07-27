package com.medipro.manager.feature.license.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medipro.manager.domain.licensing.PremiumFeature
import com.medipro.manager.domain.usecase.license.CanAccessPremiumFeatureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PremiumGateViewModel @Inject constructor(
    private val canAccessPremium: CanAccessPremiumFeatureUseCase,
) : androidx.lifecycle.ViewModel() {
    fun isAllowed(feature: PremiumFeature): Boolean = canAccessPremium(feature)
}

@Composable
fun PremiumGatedScreen(
    feature: PremiumFeature,
    onRequireSubscription: () -> Unit,
    viewModel: PremiumGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val allowed = viewModel.isAllowed(feature)
    LaunchedEffect(allowed) {
        if (!allowed) onRequireSubscription()
    }
    if (allowed) {
        content()
    } else {
        PremiumLockedPlaceholder(onSubscribe = onRequireSubscription)
    }
}

@Composable
private fun PremiumLockedPlaceholder(onSubscribe: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Pro Feature", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Subscribe or import a license to unlock this feature. Core billing remains available in Free mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSubscribe) { Text("View Subscription") }
    }
}
