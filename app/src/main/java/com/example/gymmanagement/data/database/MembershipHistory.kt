package com.example.gymmanagement.data.database

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single transaction or membership period for a member.
 * This will be stored in a sub-collection within each member's document.
 */
data class MembershipHistory(
    // Details of the membership plan for this specific transaction
    val plan: String = "",
    val startDate: Long = 0L,
    val expiryDate: Long = 0L,

    // Payment details for this transaction
    val finalAmount: Double? = null,

    // --- ADDED: Field to link the history record to a specific user ---
    val userId: String = "",

    // The date this history record was created.
    @ServerTimestamp
    val transactionDate: Date? = null
)
