package com.example.gymmanagement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.gymmanagement.ui.screens.LoginScreen
import com.google.firebase.auth.FirebaseAuth

// --- FIX: The 'application' parameter is no longer needed ---
@Composable
fun AuthGate() {
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

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
        // --- FIX: Call AppNavigator without the 'application' parameter ---
        AppNavigator()
    } else {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    }
}
