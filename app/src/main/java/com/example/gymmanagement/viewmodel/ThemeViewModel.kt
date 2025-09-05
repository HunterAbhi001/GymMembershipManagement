package com.example.gymmanagement.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Create the DataStore instance at the top level
private val Application.dataStore by preferencesDataStore(name = "settings")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // Define the key for our dark theme preference
    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_enabled")
    }

    // Read the theme preference from DataStore and expose it as a StateFlow.
    // Default to 'true' (dark theme) if no setting is found.
    val darkTheme = dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Function to toggle the theme and save the new preference to DataStore.
    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val currentTheme = preferences[DARK_THEME_KEY] ?: false
                preferences[DARK_THEME_KEY] = !currentTheme
            }
        }
    }
}


// Factory to create the ThemeViewModel
class ThemeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}