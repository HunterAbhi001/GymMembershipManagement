package com.example.gymmanagement.data.database

// This data class is set up for Firestore.
// It does not need any Room annotations.
data class Plan(
    val planName: String = "", // e.g., "1 Month", "3 Months"
    val price: Double = 0.0
)
