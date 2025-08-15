package com.example.gymmanagement.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ComposeFileProvider : FileProvider() {

    companion object {

        /**
         * Creates a temporary image file and returns its content URI using FileProvider.
         * This is used for taking camera photos in Compose.
         */
        fun getImageUri(context: Context): Uri {
            val picturesDir = File(context.filesDir, "pictures").apply { mkdirs() }

            val imageFile = File.createTempFile(
                "member_${System.currentTimeMillis()}_",
                ".jpg",
                picturesDir
            )

            val authority = "${context.packageName}.provider"
            return getUriForFile(context, authority, imageFile)
        }
    }
}
