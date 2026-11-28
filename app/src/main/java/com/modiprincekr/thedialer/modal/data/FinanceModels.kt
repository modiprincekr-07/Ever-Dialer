package com.modiprincekr.thedialer.modal.data

import org.json.JSONObject
import java.io.Serializable

data class FinanceTransaction(
    val id: String,
    val timestamp: Long,
    val amount: Double,
    val isDebit: Boolean,
    val currency: String = "INR",
    val accountLabel: String = "All",
    val merchantOrNote: String = "",
    val smsId: Long? = null,
    val isManualCash: Boolean = false,
    val availableBalance: Double? = null,
    val utrOrRef: String? = null
) : Serializable {

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("timestamp", timestamp)
        obj.put("amount", amount)
        obj.put("isDebit", isDebit)
        obj.put("currency", currency)
        obj.put("accountLabel", accountLabel)
        obj.put("merchantOrNote", merchantOrNote)
        if (smsId != null) obj.put("smsId", smsId)
        obj.put("isManualCash", isManualCash)
        if (availableBalance != null) obj.put("availableBalance", availableBalance)
        if (utrOrRef != null) obj.put("utrOrRef", utrOrRef)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): FinanceTransaction? {
            val id = obj.optString("id", "")
            if (id.isBlank()) return null
            return FinanceTransaction(
                id = id,
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                amount = obj.optDouble("amount", 0.0),
                isDebit = obj.optBoolean("isDebit", true),
                currency = obj.optString("currency", "INR"),
                accountLabel = obj.optString("accountLabel", "All"),
                merchantOrNote = obj.optString("merchantOrNote", ""),
                smsId = if (obj.has("smsId")) obj.optLong("smsId") else null,
                isManualCash = obj.optBoolean("isManualCash", false),
                availableBalance = if (obj.has("availableBalance")) obj.optDouble("availableBalance") else null,
                utrOrRef = if (obj.has("utrOrRef")) obj.optString("utrOrRef") else null
            )
        }
    }
}

data class DailyLedgerState(
    val dateKey: String,
    val openingBalance: Double,
    val currency: String = "INR",
    val entries: List<FinanceTransaction> = emptyList(),
    val accounts: List<String> = emptyList(),
    val latestAvailableBalance: Double? = null,
    val isCustomOpeningBalance: Boolean = false
) {
    val closingBalance: Double
        get() {
            var bal = openingBalance
            for (entry in entries) {
                if (entry.isDebit) bal -= entry.amount else bal += entry.amount
            }
            return bal
        }

    val totalDebits: Double
        get() = entries.filter { it.isDebit }.sumOf { it.amount }

    val totalCredits: Double
        get() = entries.filter { !it.isDebit }.sumOf { it.amount }
}

data class DailyHistoryRecord(
    val dateKey: String,
    val displayDate: String,
    val openingBalance: Double,
    val equationTape: String,
    val closingBalance: Double,
    val currency: String = "INR",
    val entriesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("dateKey", dateKey)
        obj.put("displayDate", displayDate)
        obj.put("openingBalance", openingBalance)
        obj.put("equationTape", equationTape)
        obj.put("closingBalance", closingBalance)
        obj.put("currency", currency)
        obj.put("entriesCount", entriesCount)
        obj.put("timestamp", timestamp)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): DailyHistoryRecord? {
            val dateKey = obj.optString("dateKey", "")
            if (dateKey.isBlank()) return null
            return DailyHistoryRecord(
                dateKey = dateKey,
                displayDate = obj.optString("displayDate", dateKey),
                openingBalance = obj.optDouble("openingBalance", 0.0),
                equationTape = obj.optString("equationTape", ""),
                closingBalance = obj.optDouble("closingBalance", 0.0),
                currency = obj.optString("currency", "INR"),
                entriesCount = obj.optInt("entriesCount", 0),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class ManualCashEntry(
    val id: String,
    val dateKey: String,
    val timestamp: Long,
    val amount: Double,
    val isDebit: Boolean,
    val note: String = "",
    val currency: String = "INR"
) : Serializable {

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("dateKey", dateKey)
        obj.put("timestamp", timestamp)
        obj.put("amount", amount)
        obj.put("isDebit", isDebit)
        obj.put("note", note)
        obj.put("currency", currency)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): ManualCashEntry? {
            val id = obj.optString("id", "")
            if (id.isBlank()) return null
            return ManualCashEntry(
                id = id,
                dateKey = obj.optString("dateKey", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                amount = obj.optDouble("amount", 0.0),
                isDebit = obj.optBoolean("isDebit", true),
                note = obj.optString("note", ""),
                currency = obj.optString("currency", "INR")
            )
        }
    }
}
