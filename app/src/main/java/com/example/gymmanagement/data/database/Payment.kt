package com.example.gymmanagement.data.database

import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a single payment transaction.
 * This will be stored in a sub-collection within each member's document.
 */
data class Payment(
    @DocumentId val paymentId: String = "",
    val amount: Double = 0.0,
    val memberName: String = "",
    val type: String = "Membership", // Can be "Membership" or "Due Clearance"
    val userId: String = "",

    // --- ADDED: Field to link the payment to a specific member document ---
    val memberId: String = "",

    @ServerTimestamp
    val transactionDate: Date? = null
)
