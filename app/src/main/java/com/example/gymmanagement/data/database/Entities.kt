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
    val gender: String? = null
)