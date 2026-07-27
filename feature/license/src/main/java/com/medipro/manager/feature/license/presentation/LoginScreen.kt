package com.medipro.manager.feature.license.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medipro.manager.core.designsystem.component.MediProLogo

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkipInDevMode: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MediProLogo(size = 100.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Sign in to MediPro", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Core billing works in Free mode. Sign in to sync and unlock Pro features.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.devMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Dev mode — Firebase login skipped",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            LoginStep.MOBILE -> {
                OutlinedTextField(
                    value = state.mobileNumber,
                    onValueChange = viewModel::onMobileChange,
                    label = { Text("Mobile Number") },
                    prefix = { Text("+977 ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.devMode,
                )
            }
            LoginStep.OTP -> {
                Text("OTP sent to +977 ${state.mobileNumber}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChange,
                    label = { Text("OTP Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else when {
            state.devMode -> {
                Button(
                    onClick = viewModel::continueInDevMode,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue to Dashboard") }
                onSkipInDevMode?.let { skip ->
                    TextButton(onClick = skip, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip")
                    }
                }
            }
            state.step == LoginStep.MOBILE -> {
                if (activity == null) {
                    Text("Unable to start phone verification.", color = MaterialTheme.colorScheme.error)
                } else {
                    Button(
                        onClick = { viewModel.sendOtp(activity) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.mobileNumber.length == 10,
                    ) { Text("Send OTP") }
                }
            }
            state.step == LoginStep.OTP -> {
                Button(
                    onClick = viewModel::verifyOtpAndContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Verify & Continue") }
                if (activity != null) {
                    TextButton(
                        onClick = { viewModel.resendOtp(activity) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Resend OTP") }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
