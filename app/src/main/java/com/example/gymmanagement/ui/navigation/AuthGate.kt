package com.example.gymmanagement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.gymmanagement.ui.MainApp // --- ADD THIS IMPORT ---
import com.example.gymmanagement.ui.screens.LoginScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthGate(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    // This is your robust, real-time listener. We will keep it.
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (isLoggedIn) {
        // --- THIS IS THE ONLY CHANGE ---
        // Instead of AppNavigator, we call MainApp.
        // MainApp will then handle the lock screen before showing the AppNavigator.
        MainApp(
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle
        )
    } else {
        // We keep your original LoginScreen call.
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    }
}