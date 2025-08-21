package com.example.gymmanagement.data.database

import com.google.firebase.firestore.Exclude

// --- This data class is correctly set up for Firestore ---
data class Member(
    // We use @get:Exclude to tell Firestore not to save this field,
    // as it's the document's ID itself. We'll manage it in the code.
    @get:Exclude var idString: String = "",

    // This field will link the member to a specific user account
    var userId: String = "",

    val name: String = "",
    val contact: String = "",
    val plan: String = "",
    val startDate: Long = 0L,
    val expiryDate: Long = 0L,
    val gender: String? = null,
    val photoUri: String? = null,
    val batch: String? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val finalAmount: Double? = null,
    val purchaseDate: Long? = null,
    val dueAdvance: Double? = null
)