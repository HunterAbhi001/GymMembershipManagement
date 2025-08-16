package com.example.gymmanagement.ui.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

    /**
     * Start of the day in millis (00:00:00.000) for the provided time (or now by default).
     */
    fun startOfDayMillis(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * End of the day in millis (23:59:59.999) for the provided time (or now by default).
     */
    fun endOfDayMillis(timeMillis: Long = System.currentTimeMillis()): Long {
        return startOfDayMillis(timeMillis) + TimeUnit.DAYS.toMillis(1) - 1
    }

    fun Long.toDateString(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(this))
    }
}
