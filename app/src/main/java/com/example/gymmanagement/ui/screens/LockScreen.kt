package com.example.gymmanagement.ui.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymmanagement.ui.auth.BiometricAuthenticator
import com.example.gymmanagement.ui.utils.security.SecurePrefs

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showPinEntry by remember { mutableStateOf(!biometricAuthenticator.isBiometricAuthAvailable()) }

    val showBiometricPrompt = {
        biometricAuthenticator.promptBiometricAuth(
            title = "App Locked",
            subtitle = "Authenticate to open Gym Management",
            // The negativeButtonText parameter has been removed from this call
            activity = activity,
            onSuccess = { onUnlock() },
            onFailed = { errorText = "Biometric authentication failed." },
            onError = { _, _ ->
                showPinEntry = true
            }
        )
    }

    // Attempt biometric auth automatically when the screen first appears, if available
    LaunchedEffect(Unit) {
        if (biometricAuthenticator.isBiometricAuthAvailable()) {
            showBiometricPrompt()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "App Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = showPinEntry) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enter your 4-digit PIN to unlock.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) pin = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = errorText != null
                    )
                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (SecurePrefs.verifyPin(context, pin)) {
                                onUnlock()
                            } else {
                                errorText = "Incorrect PIN. Please try again."
                                pin = ""
                            }
                        },
                        enabled = pin.length == 4
                    ) {
                        Text("Submit PIN")
                    }
                }
            }

            if (biometricAuthenticator.isBiometricAuthAvailable()) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = showBiometricPrompt) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometrics", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics")
                }
            }
        }
    }
}