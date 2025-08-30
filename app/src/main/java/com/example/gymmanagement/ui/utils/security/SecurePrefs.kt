package com.example.gymmanagement.ui.utils.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val PREFS_NAME = "secure_user_prefs"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_USER_PIN = "user_pin"

    private fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    fun isAppLockEnabled(context: Context): Boolean {
        return getEncryptedSharedPreferences(context).getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(context: Context, isEnabled: Boolean) {
        getEncryptedSharedPreferences(context).edit().putBoolean(KEY_APP_LOCK_ENABLED, isEnabled).apply()
    }

    fun savePin(context: Context, pin: String) {
        getEncryptedSharedPreferences(context).edit().putString(KEY_USER_PIN, pin).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val savedPin = getEncryptedSharedPreferences(context).getString(KEY_USER_PIN, null)
        return savedPin != null && savedPin == pin
    }

    fun hasPinSetup(context: Context): Boolean {
        return getEncryptedSharedPreferences(context).getString(KEY_USER_PIN, null) != null
    }
}