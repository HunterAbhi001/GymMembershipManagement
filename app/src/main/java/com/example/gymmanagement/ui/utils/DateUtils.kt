package com.example.gymmanagement.ui.utils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val formatters = listOf(
        SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
    )

    fun parseDate(dateString: String): Date? {
        for (formatter in formatters) {
            try {
                return formatter.parse(dateString)
            } catch (e: Exception) {
                // continue to next format
            }
        }
        return null // Return null if no format matches
    }

    fun Long.toDateString(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(this))
    }
}