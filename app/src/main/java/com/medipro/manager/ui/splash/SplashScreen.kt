package com.medipro.manager.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.designsystem.component.MediProLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        delay(800)
        when (destination) {
            SplashDestination.LOGIN -> onNavigateToLogin()
            SplashDestination.APP_LOCK -> onNavigateToAppLock()
            SplashDestination.DASHBOARD -> onNavigateToDashboard()
            SplashDestination.LOADING -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MediProLogo(size = 140.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Pharmacy ERP", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}
