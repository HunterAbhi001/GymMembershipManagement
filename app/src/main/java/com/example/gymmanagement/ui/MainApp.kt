package com.example.gymmanagement.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.gymmanagement.ui.navigation.AppNavigator
import com.example.gymmanagement.ui.screens.LockScreen
import com.example.gymmanagement.ui.utils.security.SecurePrefs

@Composable
fun MainApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val isLockEnabled = remember { SecurePrefs.isAppLockEnabled(context) }

    var isLocked by remember { mutableStateOf(isLockEnabled) }

    if (isLocked) {
        LockScreen(onUnlock = { isLocked = false })
    } else {
        AppNavigator(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle)
    }
}