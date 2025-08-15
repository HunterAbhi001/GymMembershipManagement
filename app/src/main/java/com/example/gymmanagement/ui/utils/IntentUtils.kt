package com.example.gymmanagement.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

private fun formatPhoneNumber(phone: String): String {
    // Remove all non-digit characters except '+'
    var formattedPhone = phone.replace("[^0-9+]".toRegex(), "")
    // If it's a 10-digit number, assume it's a local number and add the country code
    if (formattedPhone.length == 10 && !formattedPhone.startsWith("+")) {
        formattedPhone = "+91$formattedPhone"
    }
    return formattedPhone
}

fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
    try {
        val formattedPhone = formatPhoneNumber(phone)
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "An error occurred.", Toast.LENGTH_SHORT).show()
    }
}

fun sendSmsMessage(context: Context, phone: String, message: String) {
    try {
        val formattedPhone = formatPhoneNumber(phone)
        val uri = Uri.parse("smsto:$formattedPhone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No messaging app found.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "An error occurred.", Toast.LENGTH_SHORT).show()
    }
}