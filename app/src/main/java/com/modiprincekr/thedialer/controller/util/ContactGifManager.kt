package com.modiprincekr.thedialer.controller.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import java.io.File

/**
 * Manages custom looping GIF backgrounds for contacts (up to 15 seconds duration).
 * Handles duration parsing from GIF binary structure, local caching to internal storage,
 * and retrieval via contact ID or associated phone numbers.
 */
object ContactGifManager {

    private const val MAX_DURATION_MS = 15_000L // 15 seconds limit

    /**
     * Builds an [ImageLoader] configured with [ImageDecoderDecoder] (API 28+) or
     * [GifDecoder] (legacy) for continuous animated GIF playback.
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context.applicationContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    /**
     * Parses the total playback duration of a GIF from its raw bytes in milliseconds.
     * Evaluates standard Graphic Control Extension delay times (in 1/100s of a second).
     */
    fun parseGifDurationMs(bytes: ByteArray): Long {
        if (bytes.size < 6) return 0L
        val magic = String(bytes, 0, 6, Charsets.US_ASCII)
        if (!magic.startsWith("GIF")) return 0L

        var totalDelayCentiseconds = 0L
        var i = 6
        if (i + 7 > bytes.size) return 0L

        val packed = bytes[i + 4].toInt() and 0xFF
        val hasGct = (packed and 0x80) != 0
        val gctSize = 2 shl (packed and 0x07)
        i += 7
        if (hasGct) {
            i += 3 * gctSize
        }

        while (i < bytes.size) {
            val blockType = bytes[i].toInt() and 0xFF
            if (blockType == 0x3B) { // GIF Trailer
                break
            }
            if (blockType == 0x21) { // Extension Introducer
                if (i + 1 >= bytes.size) break
                val label = bytes[i + 1].toInt() and 0xFF
                if (label == 0xF9) { // Graphic Control Extension
                    if (i + 6 < bytes.size && (bytes[i + 2].toInt() and 0xFF) == 4) {
                        val delayLow = bytes[i + 4].toInt() and 0xFF
                        val delayHigh = bytes[i + 5].toInt() and 0xFF
                        val delay = delayLow or (delayHigh shl 8)
                        totalDelayCentiseconds += if (delay <= 1) 10 else delay
                    }
                }
                i += 2
                while (i < bytes.size) {
                    val subBlockSize = bytes[i].toInt() and 0xFF
                    i++
                    if (subBlockSize == 0) break
                    i += subBlockSize
                }
            } else if (blockType == 0x2C) { // Image Descriptor
                if (i + 9 >= bytes.size) break
                val imgPacked = bytes[i + 9].toInt() and 0xFF
                val hasLct = (imgPacked and 0x80) != 0
                val lctSize = 2 shl (imgPacked and 0x07)
                i += 10
                if (hasLct) {
                    i += 3 * lctSize
                }
                if (i >= bytes.size) break
                i++ // Skip LZW minimum code size
                while (i < bytes.size) {
                    val subBlockSize = bytes[i].toInt() and 0xFF
                    i++
                    if (subBlockSize == 0) break
                    i += subBlockSize
                }
            } else {
                i++
            }
        }
        return totalDelayCentiseconds * 10L
    }

    /**
     * Reads, validates, duration-checks, and saves a custom background GIF for a contact.
     * Enforces the 15-second duration cap. Returns [Result.success] with the absolute file path
     * or [Result.failure] on violation or I/O error.
     */
    fun saveContactGif(
        context: Context,
        contactKey: String,
        phoneNumbers: List<String>?,
        sourceUri: Uri
    ): Result<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Could not read GIF from selected URI"))

            if (bytes.size < 6 || !String(bytes, 0, 3, Charsets.US_ASCII).equals("GIF", ignoreCase = true)) {
                return Result.failure(IllegalArgumentException("Selected file is not a valid GIF image"))
            }

            val durationMs = parseGifDurationMs(bytes)
            if (durationMs > MAX_DURATION_MS) {
                val sec = (durationMs + 999) / 1000
                return Result.failure(IllegalArgumentException("GIF duration (${sec}s) exceeds the 15-second limit"))
            }

            val dir = File(context.filesDir, "contact_gifs")
            if (!dir.exists()) dir.mkdirs()

            val sanitizedKey = contactKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val targetFile = File(dir, "gif_${sanitizedKey}.gif")
            targetFile.writeBytes(bytes)

            val savedPath = targetFile.absolutePath
            val prefs = PreferenceManager(context)
            prefs.setString("contact_${contactKey}_gif_bg", savedPath)

            phoneNumbers?.forEach { phone ->
                val clean = normalizeNumber(phone)
                if (clean.isNotEmpty()) {
                    prefs.setString("contact_num_${clean}_gif_bg", savedPath)
                }
            }
            prefs.notifyChanged()

            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves the saved GIF background path for a contact by contact ID or phone number.
     */
    fun getContactGif(context: Context, contactId: String?, phoneNumber: String?): String? {
        val prefs = PreferenceManager(context)
        if (!contactId.isNullOrBlank()) {
            val path = prefs.getString("contact_${contactId}_gif_bg", null)
            if (!path.isNullOrBlank() && File(path).exists()) return path
        }
        if (!phoneNumber.isNullOrBlank()) {
            val clean = normalizeNumber(phoneNumber)
            if (clean.isNotEmpty()) {
                val path = prefs.getString("contact_num_${clean}_gif_bg", null)
                if (!path.isNullOrBlank() && File(path).exists()) return path
            }
            // Fallback: look up contact ID via PhoneLookup
            try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val resolvedId = cursor.getString(0)
                        if (!resolvedId.isNullOrBlank()) {
                            val path = prefs.getString("contact_${resolvedId}_gif_bg", null)
                            if (!path.isNullOrBlank() && File(path).exists()) return path
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Removes the contact GIF background, deleting the stored private file and preference entries.
     */
    fun removeContactGif(context: Context, contactKey: String, phoneNumbers: List<String>?) {
        try {
            val prefs = PreferenceManager(context)
            val currentPath = prefs.getString("contact_${contactKey}_gif_bg", null)
            if (!currentPath.isNullOrBlank()) {
                val file = File(currentPath)
                if (file.exists()) file.delete()
            }
            prefs.remove("contact_${contactKey}_gif_bg")

            phoneNumbers?.forEach { phone ->
                val clean = normalizeNumber(phone)
                if (clean.isNotEmpty()) {
                    prefs.remove("contact_num_${clean}_gif_bg")
                }
            }
            prefs.notifyChanged()
        } catch (_: Exception) {}
    }

    private fun normalizeNumber(number: String): String {
        return number.filter { it.isDigit() }.takeLast(10)
    }
}

/**
 * Returns a remembered [ImageLoader] tailored for animated GIF playback.
 */
@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ContactGifManager.createImageLoader(context)
    }
}
