package com.example.budgetwisesa.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ReceiptFiles {
    // Copy into private app storage: picker URI permissions alone may expire after restart.
    suspend fun import(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri)
        require(mime in listOf("image/jpeg", "image/png", "image/webp")) { "Unsupported image" }
        val directory = File(context.filesDir, "receipts").apply { mkdirs() }
        val output = File(directory, UUID.randomUUID().toString() + "." + mime!!.substringAfter('/'))
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                output.outputStream().use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 10 * 1024 * 1024) { "Image exceeds 10 MB" }
                        target.write(buffer, 0, count)
                    }
                    require(total > 0)
                }
            }
            Uri.fromFile(output).toString()
        } catch (error: Exception) { output.delete(); throw error }
    }
}
