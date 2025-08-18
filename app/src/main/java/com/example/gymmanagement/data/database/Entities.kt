package com.example.gymmanagement.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contact: String,
    val plan: String,
    val startDate: Long,
    val expiryDate: Long,
    val gender: String? = null,
    val photoUri: String? = null,

    // New fields
    val batch: String? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val finalAmount: Double? = null,
    val purchaseDate: Long? = startDate,
    val dueAdvance: Double? = null
)
