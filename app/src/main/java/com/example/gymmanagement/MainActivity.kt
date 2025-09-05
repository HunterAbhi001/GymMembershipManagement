package com.example.gymmanagement

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.gymmanagement.ui.navigation.AuthGate
import com.example.gymmanagement.ui.theme.GymManagementTheme
import com.example.gymmanagement.viewmodel.ThemeViewModel
import com.example.gymmanagement.viewmodel.ThemeViewModelFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- UPDATED: Get the ThemeViewModel instance ---
        val themeViewModel = ViewModelProvider(this, ThemeViewModelFactory(application)).get(ThemeViewModel::class.java)

        setContent {
            // --- UPDATED: Collect the theme state from the ViewModel ---
            val isDarkTheme by themeViewModel.darkTheme.collectAsState()

            GymManagementTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- UPDATED: The toggle function now calls the ViewModel ---
                    AuthGate(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { themeViewModel.toggleTheme() }
                    )
                }
            }
        }
    }
}