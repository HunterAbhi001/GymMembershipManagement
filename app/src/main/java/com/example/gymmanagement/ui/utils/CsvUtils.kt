package com.example.gymmanagement.ui.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.gymmanagement.data.database.Member
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {

    // This is the primary format for writing dates out to the CSV.
    private val primaryDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    /**
     * Reads a CSV file from a given URI and converts its rows into a list of Member objects.
     * This function is specifically tailored to the user's 12-column CSV format and
     * uses flexible parsing for dates and lines.
     */
    fun readMembersFromCsv(context: Context, uri: Uri): List<Member> {
        val members = mutableListOf<Member>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLine() // Skip header line
                var lineCount = 1
                reader.forEachLine { line ->
                    // --- FIXED: Ignore any lines that are completely blank ---
                    if (line.isBlank()) {
                        return@forEachLine // Skips to the next line
                    }

                    val tokens = parseCsvLine(line)
                    // Check if the row has the correct number of columns
                    if (tokens.size == 12) {
                        try {
                            // --- FIXED: Add a check to ensure the name field is not blank ---
                            val name = tokens[0]
                            if (name.isNotBlank()) {
                                val member = Member(
                                    name = name,
                                    contact = tokens[1], // "Mobile" column
                                    plan = tokens[2],
                                    startDate = parseDateFlexible(tokens[3]) ?: 0L,
                                    expiryDate = parseDateFlexible(tokens[4]) ?: 0L,
                                    gender = tokens[5].takeIf { it.isNotBlank() },
                                    batch = tokens[6].takeIf { it.isNotBlank() },
                                    price = tokens[7].toDoubleOrNull() ?: 0.0,
                                    discount = tokens[8].toDoubleOrNull() ?: 0.0,
                                    finalAmount = tokens[9].toDoubleOrNull() ?: 0.0,
                                    purchaseDate = parseDateFlexible(tokens[10]) ?: 0L,
                                    dueAdvance = tokens[11].toDoubleOrNull() ?: 0.0
                                )
                                members.add(member)
                            } else {
                                Log.w("CsvUtils", "Skipping row $lineCount because name is blank.")
                            }
                        } catch (e: Exception) {
                            Log.e("CsvUtils", "Error parsing row $lineCount: $line", e)
                        }
                    } else {
                        Log.w("CsvUtils", "Skipping malformed row $lineCount: Expected 12 columns, but found ${tokens.size}. Line: $line")
                    }
                    lineCount++
                }
            }
        }
        return members
    }

    /**
     * Writes a list of Member objects to a CSV file at the given URI, matching the 12-column format.
     */
    fun writeMembersToCsv(context: Context, uri: Uri, members: List<Member>) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                // Write Header matching the 12-column format
                writer.write("Name,Mobile,Plan,Start Date,Expiry Date,Gender,Batch,Price,Discount,Final Amount,Purchase Date,Due/Advance\n")

                members.forEach { member ->
                    val line = listOf(
                        member.name,
                        member.contact,
                        member.plan,
                        formatLongToDate(member.startDate),
                        formatLongToDate(member.expiryDate),
                        member.gender ?: "",
                        member.batch ?: "",
                        member.price?.toString() ?: "0.0",
                        member.discount?.toString() ?: "0.0",
                        member.finalAmount?.toString() ?: "0.0",
                        formatLongToDate(member.purchaseDate),
                        member.dueAdvance?.toString() ?: "0.0"
                    ).joinToString(",") { escapeCsv(it) }
                    writer.write("$line\n")
                }
                writer.flush()
            }
        }
    }

    // --- Helper Functions from your provided code ---

    private fun formatLongToDate(dateLong: Long?): String {
        if (dateLong == null || dateLong == 0L) return ""
        return try {
            primaryDateFormat.format(Date(dateLong))
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseDateFlexible(s: String): Long? {
        val trimmed = s.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.contains("(") && trimmed.contains(")")) {
            val inside = trimmed.substringAfter("(").substringBefore(")")
            inside.toLongOrNull()?.let { return it }
        }

        trimmed.toLongOrNull()?.let {
            if (it > 10_000_000_000L) return it
        }

        val candidates = listOf("dd/MM/yyyy", "dd-MMM-yy", "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yy", "MM/dd/yy", "MM-dd-yy")
        for (fmt in candidates) {
            try {
                val df = SimpleDateFormat(fmt, Locale.ENGLISH)
                df.isLenient = false
                val date = df.parse(trimmed) ?: continue
                val cal = Calendar.getInstance().apply { time = date }
                val year = cal.get(Calendar.YEAR)
                if (year in 0..99) {
                    cal.set(Calendar.YEAR, year + 2000)
                }
                return cal.timeInMillis
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val c = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> if (!inQuotes) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
                else -> current.append(c)
            }
            i++
        }
        tokens.add(current.toString())
        return tokens
    }
}
