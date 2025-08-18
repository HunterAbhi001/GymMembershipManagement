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

    // Preferred header order we will write and also try to read by header name (case-insensitive)
    private val csvHeaderOrdered = listOf(
        "Name", "Contact", "Plan", "StartDate", "ExpiryDate", "Gender",
        "Batch", "Price", "Discount", "FinalAmount", "PurchaseDate", "DueAdvance"
    )

    // Accept multiple synonyms for some headers (lowercase keys)
    private val headerSynonyms = mapOf(
        "name" to listOf("name"),
        "contact" to listOf("contact", "mobile", "phone"),
        "plan" to listOf("plan"),
        "startdate" to listOf("startdate", "start_date", "start"),
        "expirydate" to listOf("expirydate", "expiry_date", "expiry", "enddate"),
        "gender" to listOf("gender", "sex"),
        "batch" to listOf("batch"),
        "price" to listOf("price", "amount"),
        "discount" to listOf("discount"),
        "finalamount" to listOf("finalamount", "final_amount", "final"),
        "purchasedate" to listOf("purchasedate", "purchase_date", "purchase"),
        "dueadvance" to listOf("dueadvance", "due_advance", "due")
    )

    fun readMembersFromCsv(context: Context, uri: Uri): Pair<List<Member>, Int> {
        val members = mutableListOf<Member>()
        var failedRows = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // Read first line as header if possible
                val firstLine = reader.readLine() ?: return Pair(emptyList(), 0)
                val delimiter = if (firstLine.contains(";") && firstLine.split(";").size > firstLine.split(",").size) ";" else ","
                val headerTokens = firstLine.split(delimiter).map { it.trim().trim('"') }

                // Build a map of column name (normalized) -> index
                val headerIndex = mutableMapOf<String, Int>()
                headerTokens.forEachIndexed { idx, tok ->
                    val lower = tok.lowercase(Locale.ROOT)
                    headerSynonyms.forEach { (key, synonyms) ->
                        if (lower in synonyms) headerIndex[key] = idx
                    }
                }

                // If headerIndex is empty (no recognized headers), we'll fallback to positional parsing
                var line: String? = reader.readLine()
                while (line != null) {
                    var tokens = line.split(delimiter).map { it.trim().trim('"') }.toMutableList()
                    // If tokens fewer than header tokens, try splitting by comma as fallback
                    if (tokens.size < headerTokens.size && delimiter == ";") {
                        tokens = line.split(",").map { it.trim().trim('"') }.toMutableList()
                    }

                    try {
                        fun String?.safeAt(idx: Int) = this ?: ""

                        // Helper to retrieve either by header index or fallback position
                        fun valueAt(key: String, fallbackPos: Int): String? {
                            val idx = headerIndex[key]
                            return if (idx != null && idx < tokens.size) tokens[idx].ifBlank { null } else tokens.getOrNull(fallbackPos)?.ifBlank { null }
                        }

                        // Positional fallback mapping (if no header recognized)
                        // Order: Name(0),Contact(1),Plan(2),StartDate(3),ExpiryDate(4),Gender(5),
                        // Batch(6), Price(7), Discount(8), FinalAmount(9), PurchaseDate(10), DueAdvance(11)
                        val name = (valueAt("name", 0))?.trim()?.takeIf { it.isNotEmpty() }
                        val contact = (valueAt("contact", 1))?.trim()?.takeIf { it.isNotEmpty() }
                        val plan = (valueAt("plan", 2))?.trim()?.takeIf { it.isNotEmpty() } ?: "Unspecified"
                        val startDateString = valueAt("startdate", 3)
                        val expiryDateString = valueAt("expirydate", 4)
                        val gender = valueAt("gender", 5)?.takeIf { it.isNotEmpty() }
                        val batch = valueAt("batch", 6)?.takeIf { it.isNotEmpty() }
                        val price = valueAt("price", 7)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()
                        val discount = valueAt("discount", 8)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()
                        var finalAmount = valueAt("finalamount", 9)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()
                        val purchaseDateString = valueAt("purchasedate", 10)
                        val dueAdvance = valueAt("dueadvance", 11)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull()

                        if (name != null && contact != null && !expiryDateString.isNullOrEmpty()) {
                            val expiryDate = DateUtils.parseDate(expiryDateString)?.time

                            val parsedStart = if (!startDateString.isNullOrEmpty()) DateUtils.parseDate(startDateString)?.time else null
                            val parsedPurchase = if (!purchaseDateString.isNullOrEmpty()) DateUtils.parseDate(purchaseDateString)?.time else null

                            // Apply defaulting rules:
                            // 1) If start missing but purchase present -> start = purchase
                            // 2) If purchase missing but start present -> purchase = start
                            // 3) If both missing -> start = expiry, purchase = start (legacy behavior)
                            var startDateValue: Long? = parsedStart
                            var purchaseDateValue: Long? = parsedPurchase

                            if (startDateValue == null && purchaseDateValue != null) {
                                startDateValue = purchaseDateValue
                            }
                            if (purchaseDateValue == null && startDateValue != null) {
                                purchaseDateValue = startDateValue
                            }

                            if (startDateValue == null && expiryDate != null) {
                                startDateValue = expiryDate
                                purchaseDateValue = startDateValue
                            } else if (purchaseDateValue == null && startDateValue != null) {
                                purchaseDateValue = startDateValue
                            }

                            if (startDateValue != null && expiryDate != null) {
                                // compute finalAmount if not provided
                                if (finalAmount == null && price != null) {
                                    finalAmount = price - (discount ?: 0.0)
                                }

                                members.add(
                                    Member(
                                        name = name,
                                        contact = contact,
                                        plan = plan,
                                        startDate = startDateValue,
                                        expiryDate = expiryDate,
                                        gender = gender,
                                        photoUri = null,
                                        batch = batch,
                                        price = price,
                                        discount = discount,
                                        finalAmount = finalAmount,
                                        purchaseDate = purchaseDateValue,
                                        dueAdvance = dueAdvance
                                    )
                                )
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

                    line = reader.readLine()
                }
            }
        }

        return Pair(members, failedRows)
    }

    fun writeMembersToCsv(context: Context, uri: Uri, members: List<Member>) {
        // We'll write dates in dd-MMM-yy format
        val dateFormat = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
        context.contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { fos ->
                // header
                fos.write(csvHeaderOrdered.joinToString(",").plus("\n").toByteArray())

                members.forEach { member ->
                    val startDateString = dateFormat.format(Date(member.startDate))
                    val expiryDateString = dateFormat.format(Date(member.expiryDate))
                    val purchaseDateString = dateFormat.format(Date(member.purchaseDate ?: member.startDate))
                    val priceStr = member.price?.toString() ?: ""
                    val discountStr = member.discount?.toString() ?: ""
                    val finalAmountStr = member.finalAmount?.toString() ?: ""
                    val dueAdvanceStr = member.dueAdvance?.toString() ?: ""
                    val batchStr = member.batch ?: ""
                    val genderStr = member.gender ?: ""

                    val line = listOf(
                        member.name,
                        member.contact,
                        member.plan,
                        startDateString,
                        expiryDateString,
                        genderStr,
                        batchStr,
                        priceStr,
                        discountStr,
                        finalAmountStr,
                        purchaseDateString,
                        dueAdvanceStr
                    ).joinToString(",") { it.replace(",", " ") } + "\n"

                    fos.write(line.toByteArray())
                }
            }
        }
    }
}
