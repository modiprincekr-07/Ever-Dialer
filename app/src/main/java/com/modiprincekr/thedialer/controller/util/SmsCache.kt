package com.modiprincekr.thedialer.controller.util

import android.content.Context
import com.modiprincekr.thedialer.modal.data.SmsConversation
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk- and memory-backed cache for resolved SMS conversations, mirroring [CallLogsCache]
 * and [ContactsCache].
 *
 * Keeps the last-known conversations immediately available on cold and warm starts (0ms to render),
 * preventing spinner delays and avoiding expensive full queries before the UI first displays.
 * Stored in [Context.getFilesDir] so the Android OS does not clear it under storage pressure.
 */
object SmsCache {

    private const val CACHE_FILE_NAME = "sms_conversations_cache.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var inMemoryCache: List<SmsConversation>? = null

    fun getInMemoryCache(): List<SmsConversation>? = inMemoryCache

    fun setInMemoryCache(conversations: List<SmsConversation>) {
        inMemoryCache = conversations
    }

    private fun cacheFile(context: Context): File =
        File(context.filesDir, CACHE_FILE_NAME)

    /**
     * Reads the cached SMS conversations from memory or disk. Safe to call from background thread.
     */
    fun read(context: Context): List<SmsConversation> {
        inMemoryCache?.let { if (it.isNotEmpty()) return it }
        return try {
            val file = cacheFile(context)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val list = json.decodeFromString<List<SmsConversation>>(text)
            inMemoryCache = list
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Writes [conversations] to disk and memory atomically. Safe to call from background thread.
     */
    fun write(context: Context, conversations: List<SmsConversation>) {
        inMemoryCache = conversations
        try {
            val text = json.encodeToString(conversations)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(text)
            tmp.renameTo(cacheFile(context))
        } catch (_: Exception) {
            // Best-effort
        }
    }

    fun clear(context: Context) {
        inMemoryCache = null
        try {
            cacheFile(context).delete()
        } catch (_: Exception) {}
    }
}
