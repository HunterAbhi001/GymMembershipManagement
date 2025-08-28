package com.example.gymmanagement.data.database

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single payment transaction.
 * This will be stored in a sub-collection within each member's document.
 */
data class Payment(
    val amount: Double = 0.0,
    val memberName: String = "",
    val type: String = "Membership", // Can be "Membership" or "Due Clearance"

    // --- ADDED: Field to link the payment to a specific user ---
    val userId: String = "",

    @ServerTimestamp
    val transactionDate: Date? = null
)
