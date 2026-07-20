package com.medipro.manager.ui.applock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medipro.manager.core.designsystem.component.MediProLogo
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSetup by remember { mutableStateOf(false) }
    var confirmPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MediProLogo(size = 72.dp, compact = true)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSetup) "Set App PIN" else "Enter PIN",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        if (isSetup) {
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6) confirmPin = it },
                label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    if (isSetup) {
                        if (pin.length < 4) {
                            error = "PIN must be at least 4 digits"
                            return@launch
                        }
                        if (pin != confirmPin) {
                            error = "PINs do not match"
                            return@launch
                        }
                        viewModel.setPin(pin)
                        onUnlocked()
                    } else {
                        val valid = viewModel.verifyPin(pin)
                        if (valid) onUnlocked() else error = "Invalid PIN"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.isNotBlank()
        ) {
            Text(if (isSetup) "Save & Continue" else "Unlock")
        }

        if (!isSetup) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { isSetup = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Setup PIN (First Time)")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUnlocked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip (Dev)")
            }
        }
    }
}
