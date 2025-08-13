package com.example.gymmanagement.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contact: String,
    val plan: String, // e.g., "1 Month", "6 Months"
    val startDate: Long,
    val expiryDate: Long
)

@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val timestamp: Long
)