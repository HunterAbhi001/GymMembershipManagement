package com.example.gymmanagement

import android.app.Application
import com.example.gymmanagement.data.database.AppDatabase

class GymManagementApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}