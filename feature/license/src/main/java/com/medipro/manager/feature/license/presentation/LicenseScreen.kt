package com.medipro.manager.feature.license.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
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
fun LicenseScreen(
    onLicenseVerified: () -> Unit,
    expiredMode: Boolean = false,
    viewModel: LicenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    LaunchedEffect(expiredMode) {
        if (expiredMode) viewModel.showExpired()
    }

    LaunchedEffect(deviceId) {
        if (!expiredMode) viewModel.checkExistingLicense(deviceId)
    }

    LaunchedEffect(state.isActivated) {
        if (state.isActivated) onLicenseVerified()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MediProLogo(size = 100.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (state.step) {
                LicenseStep.EXPIRED -> "License Expired"
                LicenseStep.MOBILE -> "Activate MediPro"
                LicenseStep.OTP -> "Verify OTP"
                LicenseStep.DETAILS -> "Pharmacy Details"
            },
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Device: ${deviceId.take(12)}…", style = MaterialTheme.typography.bodySmall)
        if (state.devMode) {
            Text(
                text = "Dev licensing mode",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.licenseExpiryLabel?.let {
            Text(text = "Valid until: $it", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))

        when (state.step) {
            LicenseStep.EXPIRED -> {
                Text(
                    "Your 1-year license has expired. Renew to continue using MediPro.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = viewModel::restartActivation, modifier = Modifier.fillMaxWidth()) {
                    Text("Renew License")
                }
                TextButton(onClick = onLicenseVerified, modifier = Modifier.fillMaxWidth()) {
                    Text("Contact Distributor")
                }
            }
            LicenseStep.MOBILE -> {
                OutlinedTextField(
                    value = state.mobileNumber,
                    onValueChange = viewModel::onMobileChange,
                    label = { Text("Mobile Number") },
                    prefix = { Text("+977 ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            LicenseStep.OTP -> {
                Text("OTP sent to +977 ${state.mobileNumber}", style = MaterialTheme.typography.bodyMedium)
                if (state.otpAutoVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Phone verified automatically. Continue to pharmacy details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.otp,
                    onValueChange = viewModel::onOtpChange,
                    label = { Text("OTP Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.otpAutoVerified,
                )
            }
            LicenseStep.DETAILS -> {
                OutlinedTextField(
                    value = state.pharmacyName,
                    onValueChange = viewModel::onPharmacyNameChange,
                    label = { Text("Pharmacy Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.ownerName,
                    onValueChange = viewModel::onOwnerNameChange,
                    label = { Text("Owner Name") },
                    modifier = Modifier.fillMaxWidth(),
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
        } else when (state.step) {
            LicenseStep.MOBILE -> {
                if (activity == null) {
                    Text("Unable to start phone verification on this screen.", color = MaterialTheme.colorScheme.error)
                } else {
                    Button(
                        onClick = { viewModel.sendOtp(activity) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.mobileNumber.length == 10,
                    ) { Text("Send OTP") }
                }
            }
            LicenseStep.OTP -> {
                Button(
                    onClick = viewModel::verifyOtpAndContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.otpAutoVerified) "Continue" else "Verify OTP") }
                if (activity != null && !state.otpAutoVerified) {
                    TextButton(
                        onClick = { viewModel.resendOtp(activity) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Resend OTP") }
                }
            }
            LicenseStep.DETAILS -> Button(
                onClick = { viewModel.activateLicense(deviceId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.pharmacyName.isNotBlank(),
            ) { Text("Activate 1 Year License") }
            LicenseStep.EXPIRED -> Unit
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
