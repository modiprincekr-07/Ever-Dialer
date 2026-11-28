package com.modiprincekr.thedialer.controller.util

import com.modiprincekr.thedialer.modal.data.FinanceTransaction
import java.util.Locale

object FinanceSmsParser {

    private val EXCLUSION_PATTERNS = listOf(
        Regex("""\b(one[- ]time (?:password|passcode|code)|verification code|login code|security code)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(your\s+(?:\w+\s+)?(?:otp|code|pin)\s+is|is\s+your\s+(?:\w+\s+)?(?:otp|code|pin)|use\s+(?:otp|code)\s+\d{4,8})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:code|pin|otp)\s*[:=]\s*\d{4,8}\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(offer|win\b|won\b|loan up to|recharge|plan\b|registration is initiated|pre[- ]approved|apply now|exclusive deal|discount|promo)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(collect request|has requested|request to pay|pay using)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(failed|declined|unsuccessful|rejected|reversed)\b""", RegexOption.IGNORE_CASE)
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "withdrawal",
        "sent", "transferred", "purchase", "deducted", "upi/dr", "autopay"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "refund",
        "cashback", "added", "upi/cr"
    )

    private val CURRENCY_SYMBOLS = mapOf(
        "₹" to "INR",
        "rs." to "INR",
        "rs" to "INR",
        "inr" to "INR",
        "$" to "USD",
        "usd" to "USD",
        "€" to "EUR",
        "eur" to "EUR",
        "£" to "GBP",
        "gbp" to "GBP",
        "aed" to "AED",
        "sar" to "SAR",
        "jpy" to "JPY",
        "¥" to "JPY",
        "cad" to "CAD",
        "aud" to "AUD",
        "sgd" to "SGD",
        "bdt" to "BDT",
        "npr" to "NPR",
        "pkr" to "PKR"
    )

    private val CURRENCY_REGEX_PART = """(?:₹|Rs\.?|INR|\$|USD|€|EUR|£|GBP|AED|SAR|JPY|¥|CAD|AUD|SGD|BDT|NPR|PKR)"""

    // Matches e.g. "Rs. 1,350.00", "₹500", "USD 20.50", "$ 100"
    private val AMOUNT_PREFIX_REGEX = Regex(
        """($CURRENCY_REGEX_PART)\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches e.g. "1,350.00 INR", "500 Rs.", "20.50 USD"
    private val AMOUNT_SUFFIX_REGEX = Regex(
        """([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)\s*($CURRENCY_REGEX_PART)\b""",
        RegexOption.IGNORE_CASE
    )

    // Matches available balance: e.g. "Avl Bal Rs. 15,400.50", "Available Balance: INR 1200", "Bal: 1234.50"
    private val AVL_BAL_REGEX = Regex(
        """\b(?:avl(?:\.|ailable)?\s*bal(?:\.|ance)?|total\s*bal(?:\.|ance)?|bal(?:\.|ance)?)\s*(?:is|:)?\s*(?:$CURRENCY_REGEX_PART)?\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches account last 3 or 4 digits: e.g. "A/c XX4167", "a/c ending 4167", "Card ending in 1234", "A/C *4167"
    private val ACCOUNT_DIGITS_REGEX = Regex(
        """\b(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*)?(?:with|in)?\s*(?:x{1,}|[*]{1,})*([0-9]{3,4})\b""",
        RegexOption.IGNORE_CASE
    )

    // Matches reference or UTR: e.g. "UTR 123456789012", "Ref no. 987654", "UPI Ref 1234"
    private val UTR_REGEX = Regex(
        """\b(?:utr|ref(?:\.|erence)?(?:\s*no|\s*id)?|txn(?:\.|id)?|upi\s*ref(?:\s*no)?)\s*[:#\-]?\s*([a-zA-Z0-9]{6,16})\b""",
        RegexOption.IGNORE_CASE
    )

    // Matches recipient/merchant: e.g. "to VPA abc@upi", "to XYZ Store", "at Amazon"
    private val MERCHANT_REGEX = Regex(
        """\b(?:to|at|vpa|paid to|info)\s+([a-zA-Z0-9._\-@ ]{3,25})""",
        RegexOption.IGNORE_CASE
    )

    fun isFinancialSms(body: String, address: String = ""): Boolean {
        return parse(body, address) != null
    }

    fun parse(
        body: String,
        address: String = "",
        timestamp: Long = System.currentTimeMillis(),
        smsId: Long? = null
    ): FinanceTransaction? {
        if (body.isBlank()) return null

        val lowerBody = body.lowercase(Locale.ROOT)

        // 1. Strict Exclusion (OTP, Promo, Requests, Failed)
        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.containsMatchIn(body)) return null
        }

        // 2. Debit vs Credit Classification
        val isDebit = classifyDebitOrCredit(lowerBody) ?: return null

        // 3. Amount & Currency Detection
        val (amount, currency) = extractAmountAndCurrency(body) ?: return null
        if (amount <= 0.0) return null

        // 4. Available Balance Extraction
        val availableBalance = extractAvailableBalance(body)

        // 5. Account Label Extraction
        val accountLabel = extractAccountLabel(body, address)

        // 6. UTR / Ref ID
        val utr = UTR_REGEX.find(body)?.groupValues?.get(1)?.trim()

        // 7. Merchant or Note
        val merchantOrNote = extractMerchant(body)

        val id = if (smsId != null) "sms_$smsId" else "tx_${timestamp}_${(amount * 100).toLong()}_${if (isDebit) "dr" else "cr"}"

        return FinanceTransaction(
            id = id,
            timestamp = timestamp,
            amount = amount,
            isDebit = isDebit,
            currency = currency,
            accountLabel = accountLabel,
            merchantOrNote = merchantOrNote,
            smsId = smsId,
            isManualCash = false,
            availableBalance = availableBalance,
            utrOrRef = utr
        )
    }

    private fun classifyDebitOrCredit(lowerBody: String): Boolean? {
        // Specific phrases with higher confidence
        if (Regex("""\bdebited\s+from\b""").containsMatchIn(lowerBody)) return true
        if (Regex("""\bcredited\s+to\b""").containsMatchIn(lowerBody)) return false
        if (Regex("""\bspent\b""").containsMatchIn(lowerBody)) return true
        if (Regex("""\breceived\b""").containsMatchIn(lowerBody)) return false
        if (Regex("""\bpaid\b""").containsMatchIn(lowerBody)) return true
        if (Regex("""\brefund\b""").containsMatchIn(lowerBody)) return false
        if (Regex("""\bcashback\b""").containsMatchIn(lowerBody)) return false
        if (Regex("""\bwithdrawn\b""").containsMatchIn(lowerBody)) return true
        if (Regex("""\bdeposited\b""").containsMatchIn(lowerBody)) return false

        // Check general keywords
        val hasDebit = DEBIT_KEYWORDS.any { Regex("""\b$it\b""").containsMatchIn(lowerBody) } ||
                Regex("""\bdr\.?\b""").containsMatchIn(lowerBody) ||
                lowerBody.contains("upi/dr")
        val hasCredit = CREDIT_KEYWORDS.any { Regex("""\b$it\b""").containsMatchIn(lowerBody) } ||
                Regex("""\bcr\.?\b""").containsMatchIn(lowerBody) ||
                lowerBody.contains("upi/cr")

        return when {
            hasDebit && !hasCredit -> true
            hasCredit && !hasDebit -> false
            hasDebit && hasCredit -> {
                // If both are present, determine based on position or context
                val debitIdx = DEBIT_KEYWORDS.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val creditIdx = CREDIT_KEYWORDS.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                debitIdx < creditIdx
            }
            else -> null
        }
    }

    private fun extractAmountAndCurrency(body: String): Pair<Double, String>? {
        // Try prefix pattern first (e.g. "Rs. 1,350.00")
        val prefixMatch = AMOUNT_PREFIX_REGEX.find(body)
        if (prefixMatch != null) {
            val currRaw = prefixMatch.groupValues[1].lowercase(Locale.ROOT).trim()
            val amountRaw = prefixMatch.groupValues[2].replace(",", "").trim()
            val amount = amountRaw.toDoubleOrNull()
            if (amount != null && amount > 0.0) {
                val currency = CURRENCY_SYMBOLS[currRaw] ?: currRaw.uppercase(Locale.ROOT)
                return Pair(amount, currency)
            }
        }

        // Try suffix pattern (e.g. "1,350.00 INR")
        val suffixMatch = AMOUNT_SUFFIX_REGEX.find(body)
        if (suffixMatch != null) {
            val amountRaw = suffixMatch.groupValues[1].replace(",", "").trim()
            val currRaw = suffixMatch.groupValues[2].lowercase(Locale.ROOT).trim()
            val amount = amountRaw.toDoubleOrNull()
            if (amount != null && amount > 0.0) {
                val currency = CURRENCY_SYMBOLS[currRaw] ?: currRaw.uppercase(Locale.ROOT)
                return Pair(amount, currency)
            }
        }

        return null
    }

    private fun extractAvailableBalance(body: String): Double? {
        val match = AVL_BAL_REGEX.find(body) ?: return null
        val raw = match.groupValues[1].replace(",", "").trim()
        return raw.toDoubleOrNull()
    }

    private fun extractAccountLabel(body: String, address: String): String {
        val digitsMatch = ACCOUNT_DIGITS_REGEX.find(body)
        val digits = digitsMatch?.groupValues?.get(1)?.trim()

        val cleanSender = cleanSenderAddress(address)

        return when {
            cleanSender.isNotBlank() && !digits.isNullOrBlank() -> "$cleanSender •$digits"
            cleanSender.isNotBlank() -> cleanSender
            !digits.isNullOrBlank() -> "A/c •$digits"
            else -> "Bank A/c"
        }
    }

    private fun cleanSenderAddress(address: String): String {
        if (address.isBlank()) return ""
        var cleaned = address.trim()
        // Strip telecom header prefix (e.g. "VK-", "AD-", "VM-", "BZ-", "AX-")
        if (cleaned.length >= 3 && cleaned[2] == '-') {
            cleaned = cleaned.substring(3)
        }
        val upper = cleaned.uppercase(Locale.ROOT)

        // Recognize popular bank names
        return when {
            upper.contains("IPPB") -> "IPPB"
            upper.contains("HDFC") -> "HDFC"
            upper.contains("ICICI") -> "ICICI"
            upper.contains("SBI") -> "SBI"
            upper.contains("AXIS") -> "AXIS"
            upper.contains("IDFC") -> "IDFC"
            upper.contains("PNB") || upper.contains("PUNB") -> "PNB"
            upper.contains("BOB") || upper.contains("BARB") -> "BOB"
            upper.contains("KOTAK") -> "KOTAK"
            upper.contains("CANARA") || upper.contains("CNRB") -> "Canara"
            upper.contains("UNION") || upper.contains("UBIN") -> "Union"
            upper.contains("PAYTM") -> "Paytm"
            upper.contains("GPAY") || upper.contains("GOOGLE") -> "GPay"
            upper.contains("PHONEPE") -> "PhonePe"
            upper.contains("AMEX") -> "Amex"
            upper.contains("CITI") -> "Citi"
            upper.contains("CHASE") -> "Chase"
            upper.contains("BOFA") -> "BofA"
            upper.contains("WELLS") -> "Wells Fargo"
            upper.contains("HSBC") -> "HSBC"
            upper.contains("BARCLAYS") -> "Barclays"
            cleaned.length in 3..10 && cleaned.all { it.isLetterOrDigit() } -> cleaned.uppercase(Locale.ROOT)
            else -> ""
        }
    }

    private fun extractMerchant(body: String): String {
        val match = MERCHANT_REGEX.find(body) ?: return ""
        val candidate = match.groupValues[1].trim()
        val cleaned = candidate.split(Regex("""[\r\n,.;]""")).firstOrNull()?.trim() ?: candidate
        // Ignore keywords
        val lower = cleaned.lowercase(Locale.ROOT)
        if (lower.contains("your a/c") || lower.contains("account") || lower.contains("avl bal") || lower.contains("ref")) {
            return ""
        }
        return cleaned.take(25).trim()
    }

    /**
     * Deduplicates alerts for the same transaction (e.g. UPI alert + Bank alert within 2 minutes
     * or sharing the same UTR / Ref ID).
     */
    fun deduplicate(transactions: List<FinanceTransaction>): List<FinanceTransaction> {
        if (transactions.size <= 1) return transactions

        val sorted = transactions.sortedBy { it.timestamp }
        val result = mutableListOf<FinanceTransaction>()

        for (curr in sorted) {
            val isDuplicate = result.any { prev ->
                // Check exact UTR match
                val utrMatch = !curr.utrOrRef.isNullOrBlank() &&
                        !prev.utrOrRef.isNullOrBlank() &&
                        curr.utrOrRef.equals(prev.utrOrRef, ignoreCase = true)

                // Check temporal and amount match: within 120 seconds, exact amount, currency, and direction
                val temporalMatch = kotlin.math.abs(curr.timestamp - prev.timestamp) <= 120_000L &&
                        curr.amount == prev.amount &&
                        curr.isDebit == prev.isDebit &&
                        curr.currency.equals(prev.currency, ignoreCase = true)

                utrMatch || temporalMatch
            }

            if (!isDuplicate) {
                result.add(curr)
            }
        }
        return result
    }
}
