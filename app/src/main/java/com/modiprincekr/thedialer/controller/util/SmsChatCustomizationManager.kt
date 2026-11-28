package com.modiprincekr.thedialer.controller.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Manages per-chat and global customization for SMS conversations:
 * - Wallpaper URI (internal storage image)
 * - Solid Background Color (ARGB Int)
 * - Outgoing Bubble Color (ARGB Int)
 * - Incoming Bubble Color (ARGB Int)
 * - Chat Text Size (sp Float)
 * - Expressive Animated PFP Shape (Shape Key String)
 */
object SmsChatCustomizationManager {

    private const val PREF_KEY_GLOBAL_WALLPAPER_URI = "sms_custom_global_wallpaper_uri"
    private const val PREF_KEY_GLOBAL_BG_COLOR = "sms_custom_global_bg_color"
    private const val PREF_KEY_GLOBAL_OUTGOING_COLOR = "sms_custom_global_outgoing_color"
    private const val PREF_KEY_GLOBAL_INCOMING_COLOR = "sms_custom_global_incoming_color"
    private const val PREF_KEY_GLOBAL_TEXT_SIZE = "sms_custom_global_text_size"
    private const val PREF_KEY_GLOBAL_PFP_SHAPE = "sms_custom_global_pfp_shape"

    fun normalizeAddress(address: String): String {
        val digits = address.filter { it.isDigit() }
        if (digits.length >= 7) return digits.takeLast(10)
        return address.filter { it.isLetterOrDigit() }.lowercase()
    }

    // ── Wallpaper URI ───────────────────────────────────────────────────────────

    fun getWallpaperUri(prefs: PreferenceManager, address: String = ""): String? {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val chatUri = prefs.getString("sms_custom_wallpaper_uri_$norm", null)
            if (!chatUri.isNullOrBlank()) return chatUri
        }
        return prefs.getString(PREF_KEY_GLOBAL_WALLPAPER_URI, null)?.takeIf { it.isNotBlank() }
    }

    fun setWallpaperUri(prefs: PreferenceManager, address: String, uri: String?) {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            if (uri != null) prefs.setString("sms_custom_wallpaper_uri_$norm", uri)
            else prefs.remove("sms_custom_wallpaper_uri_$norm")
        } else {
            if (uri != null) prefs.setString(PREF_KEY_GLOBAL_WALLPAPER_URI, uri)
            else prefs.remove(PREF_KEY_GLOBAL_WALLPAPER_URI)
        }
    }

    // ── Background Color (ARGB) ──────────────────────────────────────────────────

    fun getBackgroundColor(prefs: PreferenceManager, address: String = ""): Int? {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val key = "sms_custom_bg_color_$norm"
            val c = prefs.getInt(key, -1)
            if (c != -1) return c
        }
        val global = prefs.getInt(PREF_KEY_GLOBAL_BG_COLOR, -1)
        return if (global != -1) global else null
    }

    fun setBackgroundColor(prefs: PreferenceManager, address: String, color: Int?) {
        val norm = normalizeAddress(address)
        val key = if (norm.isNotBlank()) "sms_custom_bg_color_$norm" else PREF_KEY_GLOBAL_BG_COLOR
        if (color != null) prefs.setInt(key, color) else prefs.remove(key)
    }

    // ── Outgoing Bubble Color (ARGB) ─────────────────────────────────────────────

    fun getOutgoingBubbleColor(prefs: PreferenceManager, address: String = ""): Int? {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val key = "sms_custom_outgoing_color_$norm"
            val c = prefs.getInt(key, -1)
            if (c != -1) return c
        }
        val global = prefs.getInt(PREF_KEY_GLOBAL_OUTGOING_COLOR, -1)
        return if (global != -1) global else null
    }

    fun setOutgoingBubbleColor(prefs: PreferenceManager, address: String, color: Int?) {
        val norm = normalizeAddress(address)
        val key = if (norm.isNotBlank()) "sms_custom_outgoing_color_$norm" else PREF_KEY_GLOBAL_OUTGOING_COLOR
        if (color != null) prefs.setInt(key, color) else prefs.remove(key)
    }

    // ── Incoming Bubble Color (ARGB) ─────────────────────────────────────────────

    fun getIncomingBubbleColor(prefs: PreferenceManager, address: String = ""): Int? {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val key = "sms_custom_incoming_color_$norm"
            val c = prefs.getInt(key, -1)
            if (c != -1) return c
        }
        val global = prefs.getInt(PREF_KEY_GLOBAL_INCOMING_COLOR, -1)
        return if (global != -1) global else null
    }

    fun setIncomingBubbleColor(prefs: PreferenceManager, address: String, color: Int?) {
        val norm = normalizeAddress(address)
        val key = if (norm.isNotBlank()) "sms_custom_incoming_color_$norm" else PREF_KEY_GLOBAL_INCOMING_COLOR
        if (color != null) prefs.setInt(key, color) else prefs.remove(key)
    }

    // ── Text Size (sp) ───────────────────────────────────────────────────────────

    fun getTextSize(prefs: PreferenceManager, address: String = ""): Float {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val key = "sms_custom_text_size_$norm"
            val s = prefs.getFloat(key, -1f)
            if (s > 0f) return s
        }
        val global = prefs.getFloat(PREF_KEY_GLOBAL_TEXT_SIZE, -1f)
        if (global > 0f) return global
        return prefs.getFloat(PreferenceManager.KEY_SMS_CHAT_TEXT_SIZE, PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE)
    }

    fun setTextSize(prefs: PreferenceManager, address: String, sizeSp: Float?) {
        val norm = normalizeAddress(address)
        val key = if (norm.isNotBlank()) "sms_custom_text_size_$norm" else PREF_KEY_GLOBAL_TEXT_SIZE
        if (sizeSp != null && sizeSp > 0f) prefs.setFloat(key, sizeSp) else prefs.remove(key)
    }

    // ── PFP Shape Key ────────────────────────────────────────────────────────────

    fun getPfpShape(prefs: PreferenceManager, address: String = ""): String {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            val key = "sms_custom_pfp_shape_$norm"
            val shape = prefs.getString(key, null)
            if (!shape.isNullOrBlank()) return shape
        }
        val global = prefs.getString(PREF_KEY_GLOBAL_PFP_SHAPE, null)
        if (!global.isNullOrBlank()) return global
        return prefs.getSelectedPfpShape()
    }

    fun setPfpShape(prefs: PreferenceManager, address: String, shapeKey: String?) {
        val norm = normalizeAddress(address)
        val key = if (norm.isNotBlank()) "sms_custom_pfp_shape_$norm" else PREF_KEY_GLOBAL_PFP_SHAPE
        if (!shapeKey.isNullOrBlank()) prefs.setString(key, shapeKey) else prefs.remove(key)
    }

    // ── Reset ───────────────────────────────────────────────────────────────────

    fun resetChatCustomization(prefs: PreferenceManager, address: String) {
        val norm = normalizeAddress(address)
        if (norm.isNotBlank()) {
            prefs.remove("sms_custom_wallpaper_uri_$norm")
            prefs.remove("sms_custom_bg_color_$norm")
            prefs.remove("sms_custom_outgoing_color_$norm")
            prefs.remove("sms_custom_incoming_color_$norm")
            prefs.remove("sms_custom_text_size_$norm")
            prefs.remove("sms_custom_pfp_shape_$norm")
        } else {
            prefs.remove(PREF_KEY_GLOBAL_WALLPAPER_URI)
            prefs.remove(PREF_KEY_GLOBAL_BG_COLOR)
            prefs.remove(PREF_KEY_GLOBAL_OUTGOING_COLOR)
            prefs.remove(PREF_KEY_GLOBAL_INCOMING_COLOR)
            prefs.remove(PREF_KEY_GLOBAL_TEXT_SIZE)
            prefs.remove(PREF_KEY_GLOBAL_PFP_SHAPE)
        }
    }
}
