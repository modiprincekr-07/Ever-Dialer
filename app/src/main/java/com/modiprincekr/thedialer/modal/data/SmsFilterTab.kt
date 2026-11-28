package com.modiprincekr.thedialer.modal.data

enum class SmsFilterTab(val id: String, val displayName: String) {
    ALL("all", "All"),
    FAVOURITES("favourites", "Favourites"),
    STARRED("starred", "Starred"),
    CONTACTS("contacts", "Contacts"),
    UNKNOWN("unknown", "Unknown"),
    PAYMENTS("payments", "Payments"),
    RECHARGE("recharge", "Recharge"),
    OTP("otp", "OTP"),
    FINANCE("finance", "Finance");

    companion object {
        val DEFAULT_ORDER: List<SmsFilterTab> = listOf(
            ALL,
            OTP,
            FINANCE,
            UNKNOWN,
            STARRED,
            CONTACTS,
            FAVOURITES,
            PAYMENTS,
            RECHARGE
        )

        fun fromId(id: String): SmsFilterTab {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: ALL
        }

        fun parseOrder(saved: String?): List<SmsFilterTab> {
            if (saved.isNullOrBlank()) return DEFAULT_ORDER
            val savedTabs = saved.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { id -> entries.find { it.id.equals(id, ignoreCase = true) } }
                .distinct()
                .toMutableList()

            // Migration Guard: automatically inject "Finance" right after "OTP" if not yet present
            if (!savedTabs.contains(FINANCE)) {
                val otpIndex = savedTabs.indexOf(OTP)
                if (otpIndex >= 0) {
                    savedTabs.add(otpIndex + 1, FINANCE)
                } else {
                    savedTabs.add(FINANCE)
                }
            }

            val missingTabs = DEFAULT_ORDER.filter { it !in savedTabs }
            return (savedTabs + missingTabs).distinct()
        }

        fun serializeOrder(order: List<SmsFilterTab>): String {
            return order.joinToString(",") { it.id }
        }
    }
}
