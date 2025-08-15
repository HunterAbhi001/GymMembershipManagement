package com.example.gymmanagement.ui.utils

import android.content.Context
import android.net.Uri
import com.example.gymmanagement.data.database.Member
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {

    fun readMembersFromCsv(context: Context, uri: Uri): Pair<List<Member>, Int> {
        val members = mutableListOf<Member>()
        var failedRows = 0
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // Skip header line
                reader.readLine()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    var tokens = line!!.split(",")
                    // Handle potential semicolon delimiter
                    if (tokens.size < 5 && line!!.contains(";")) {
                        tokens = line!!.split(";")
                    }

                    try {
                        val name = tokens.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
                        val contact = tokens.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                        val plan = tokens.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() } ?: "Unspecified"
                        val startDateString = tokens.getOrNull(3)?.trim()
                        val expiryDateString = tokens.getOrNull(4)?.trim()
                        val gender = if (tokens.size > 5) tokens.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() } else null

                        if (name != null && contact != null && !expiryDateString.isNullOrEmpty()) {
                            val expiryDate = DateUtils.parseDate(expiryDateString)?.time
                            val startDate = if (!startDateString.isNullOrEmpty()) {
                                DateUtils.parseDate(startDateString)?.time
                            } else {
                                expiryDate // Default start date to expiry date if not provided
                            }

                            if (startDate != null && expiryDate != null) {
                                members.add(Member(name = name, contact = contact, plan = plan, startDate = startDate, expiryDate = expiryDate, gender = gender))
                            } else {
                                failedRows++
                            }
                        } else {
                            failedRows++
                        }
                    } catch (e: Exception) {
                        failedRows++
                        e.printStackTrace()
                    }
                }
            }
        }
        return Pair(members, failedRows)
    }

    fun writeMembersToCsv(context: Context, uri: Uri, members: List<Member>) {
        val dateFormat = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
        context.contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { fos ->
                fos.write("Name,Contact,Plan,StartDate,ExpiryDate,Gender\n".toByteArray())
                members.forEach { member ->
                    val startDateString = dateFormat.format(Date(member.startDate))
                    val expiryDateString = dateFormat.format(Date(member.expiryDate))
                    val line = "${member.name},${member.contact},${member.plan},$startDateString,$expiryDateString,${member.gender ?: ""}\n"
                    fos.write(line.toByteArray())
                }
            }
        }
    }
}