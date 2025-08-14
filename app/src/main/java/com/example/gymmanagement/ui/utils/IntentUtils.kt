package com.example.gymmanagement.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
    try {
        // Ensure the phone number includes the country code and no symbols
        val formattedPhone = phone.replace("[^0-9+]".toRegex(), "")
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
        val formattedPhone = phone.replace("[^0-9+]".toRegex(), "")
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