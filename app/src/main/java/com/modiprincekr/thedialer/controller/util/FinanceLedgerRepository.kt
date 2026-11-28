package com.modiprincekr.thedialer.controller.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Telephony
import com.modiprincekr.thedialer.modal.data.DailyHistoryRecord
import com.modiprincekr.thedialer.modal.data.DailyLedgerState
import com.modiprincekr.thedialer.modal.data.FinanceTransaction
import com.modiprincekr.thedialer.modal.data.ManualCashEntry
import com.modiprincekr.thedialer.modal.data.SmsConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinanceLedgerRepository {

    private const val PREFS_NAME = "finance_ledger_prefs"
    private const val KEY_LAST_DATE_KEY = "last_date_key"
    private const val KEY_OPENING_BALANCES = "opening_balances_json"
    private const val KEY_MANUAL_CASH_ENTRIES = "manual_cash_entries_json"
    private const val KEY_DAILY_HISTORY = "daily_history_json"
    private const val KEY_PRIVACY_MASK = "privacy_mask_enabled"
    private const val KEY_SELECTED_ACCOUNT = "selected_account_filter"

    private val _dailyLedgerState = MutableStateFlow(
        DailyLedgerState(
            dateKey = getTodayDateKey(),
            openingBalance = 0.0,
            currency = "INR",
            entries = emptyList(),
            accounts = listOf("All")
        )
    )
    val dailyLedgerState: StateFlow<DailyLedgerState> = _dailyLedgerState.asStateFlow()

    private val _historyFlow = MutableStateFlow<List<DailyHistoryRecord>>(emptyList())
    val historyFlow: StateFlow<List<DailyHistoryRecord>> = _historyFlow.asStateFlow()

    private val _privacyMaskFlow = MutableStateFlow(false)
    val privacyMaskFlow: StateFlow<Boolean> = _privacyMaskFlow.asStateFlow()

    private val _selectedAccountFlow = MutableStateFlow("All")
    val selectedAccountFlow: StateFlow<String> = _selectedAccountFlow.asStateFlow()

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayStartMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getTodayEndMs(): Long {
        return getTodayStartMs() + 86400000L - 1L
    }

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        _privacyMaskFlow.value = prefs.getBoolean(KEY_PRIVACY_MASK, false)
        _selectedAccountFlow.value = prefs.getString(KEY_SELECTED_ACCOUNT, "All") ?: "All"
        _historyFlow.value = loadHistory(context)
        isInitialized = true
        refresh(context)
    }

    fun togglePrivacyMask(context: Context): Boolean {
        val newVal = !_privacyMaskFlow.value
        _privacyMaskFlow.value = newVal
        getPrefs(context).edit().putBoolean(KEY_PRIVACY_MASK, newVal).apply()
        return newVal
    }

    fun selectAccount(context: Context, account: String) {
        _selectedAccountFlow.value = account
        getPrefs(context).edit().putString(KEY_SELECTED_ACCOUNT, account).apply()
        refresh(context)
    }

    fun setOpeningBalance(context: Context, balance: Double, account: String = "All") {
        val prefs = getPrefs(context)
        val todayKey = getTodayDateKey()
        val balances = loadOpeningBalances(context).toMutableMap()
        balances["${todayKey}_$account"] = balance
        balances["${todayKey}_custom"] = 1.0

        val obj = JSONObject()
        for ((k, v) in balances) {
            obj.put(k, v)
        }
        prefs.edit().putString(KEY_OPENING_BALANCES, obj.toString()).apply()
        refresh(context)
    }

    fun addManualCashEntry(
        context: Context,
        amount: Double,
        isDebit: Boolean,
        note: String,
        currency: String = "INR"
    ) {
        val todayKey = getTodayDateKey()
        val entry = ManualCashEntry(
            id = "cash_${System.currentTimeMillis()}",
            dateKey = todayKey,
            timestamp = System.currentTimeMillis(),
            amount = amount,
            isDebit = isDebit,
            note = note.trim(),
            currency = currency
        )
        val list = loadManualCashEntries(context).toMutableList()
        list.add(entry)
        saveManualCashEntries(context, list)
        refresh(context)
    }

    fun deleteManualCashEntry(context: Context, id: String) {
        val list = loadManualCashEntries(context).filterNot { it.id == id }
        saveManualCashEntries(context, list)
        refresh(context)
    }

    /**
     * Called whenever SMS changes or the screen is viewed.
     */
    fun refresh(context: Context, conversations: List<SmsConversation> = emptyList()) {
        val todayKey = getTodayDateKey()
        val todayStartMs = getTodayStartMs()

        // 1. Fetch today's SMS transactions from content provider or conversations fallback
        var smsTxs = fetchTodaySmsTransactions(context, todayStartMs)
        if (smsTxs.isEmpty() && conversations.isNotEmpty()) {
            smsTxs = conversations
                .filter { it.date >= todayStartMs }
                .mapNotNull { conv ->
                    FinanceSmsParser.parse(conv.snippet, conv.address, conv.date)
                }
        }

        // Deduplicate SMS alerts
        val dedupedSms = FinanceSmsParser.deduplicate(smsTxs)

        // 2. Fetch today's manual cash entries
        val manualCash = loadManualCashEntries(context).filter { it.dateKey == todayKey }
        val cashTxs = manualCash.map {
            FinanceTransaction(
                id = it.id,
                timestamp = it.timestamp,
                amount = it.amount,
                isDebit = it.isDebit,
                currency = it.currency,
                accountLabel = "Cash",
                merchantOrNote = it.note,
                isManualCash = true
            )
        }

        // 3. Combine and sort
        val allTodayTxs = (dedupedSms + cashTxs).sortedBy { it.timestamp }

        // Check if midnight rollover happened
        checkRollover(context, todayKey, allTodayTxs)

        // Detect all unique accounts
        val detectedAccounts = linkedSetOf("All")
        allTodayTxs.forEach { tx ->
            if (tx.accountLabel.isNotBlank()) {
                detectedAccounts.add(tx.accountLabel)
            }
        }
        val accountList = detectedAccounts.toList()

        val selectedAccount = _selectedAccountFlow.value
        val filteredTxs = if (selectedAccount == "All") {
            allTodayTxs
        } else {
            allTodayTxs.filter { it.accountLabel.equals(selectedAccount, ignoreCase = true) }
        }

        // Find primary currency
        val currency = allTodayTxs.firstOrNull()?.currency ?: "INR"

        // Determine opening balance
        val openingBal = resolveOpeningBalance(context, todayKey, selectedAccount, allTodayTxs)
        val hasCustomOpening = isCustomOpeningBalance(context, todayKey)

        // Latest available balance from SMS
        val latestAvlBal = allTodayTxs.lastOrNull { it.availableBalance != null }?.availableBalance

        _dailyLedgerState.value = DailyLedgerState(
            dateKey = todayKey,
            openingBalance = openingBal,
            currency = currency,
            entries = filteredTxs,
            accounts = accountList,
            latestAvailableBalance = latestAvlBal,
            isCustomOpeningBalance = hasCustomOpening
        )
    }

    private fun resolveOpeningBalance(
        context: Context,
        todayKey: String,
        account: String,
        todayTxs: List<FinanceTransaction>
    ): Double {
        val balances = loadOpeningBalances(context)
        val key = "${todayKey}_$account"
        val allKey = "${todayKey}_All"

        // Explicitly set opening balance
        if (balances.containsKey(key)) return balances[key] ?: 0.0
        if (account == "All" && balances.containsKey(allKey)) return balances[allKey] ?: 0.0

        // If yesterday was archived, inherit yesterday's closing balance
        val history = loadHistory(context)
        val yesterdayRecord = history.firstOrNull()
        if (yesterdayRecord != null) {
            val inherited = yesterdayRecord.closingBalance
            // Save as today's starting balance
            setOpeningBalance(context, inherited, "All")
            return inherited
        }

        // If today has an SMS with Avl Bal, deduce opening balance from it:
        // openingBalance = avlBal + sum(today's debits up to that SMS) - sum(today's credits up to that SMS)
        val lastTxWithBal = todayTxs.lastOrNull { it.availableBalance != null }
        if (lastTxWithBal != null && lastTxWithBal.availableBalance != null) {
            val avl = lastTxWithBal.availableBalance
            val idx = todayTxs.indexOf(lastTxWithBal)
            val subList = todayTxs.subList(0, idx + 1)
            val debitsBefore = subList.filter { it.isDebit }.sumOf { it.amount }
            val creditsBefore = subList.filter { !it.isDebit }.sumOf { it.amount }
            val deduced = avl + debitsBefore - creditsBefore
            val cleanDeduced = kotlin.math.max(0.0, deduced)
            setOpeningBalance(context, cleanDeduced, "All")
            return cleanDeduced
        }

        return 0.0
    }

    private fun isCustomOpeningBalance(context: Context, todayKey: String): Boolean {
        val balances = loadOpeningBalances(context)
        return balances.containsKey("${todayKey}_custom")
    }

    private fun checkRollover(
        context: Context,
        todayKey: String,
        todayTxs: List<FinanceTransaction>
    ) {
        val prefs = getPrefs(context)
        val lastDateKey = prefs.getString(KEY_LAST_DATE_KEY, null)

        if (lastDateKey == null) {
            prefs.edit().putString(KEY_LAST_DATE_KEY, todayKey).apply()
            return
        }

        if (lastDateKey < todayKey) {
            // A midnight crossover happened! Archive unarchived day into history
            val balances = loadOpeningBalances(context)
            val opening = balances["${lastDateKey}_All"] ?: 0.0

            // Find all past transactions for lastDateKey
            val pastSms = fetchPastSmsTransactions(context, lastDateKey)
            val pastCash = loadManualCashEntries(context).filter { it.dateKey == lastDateKey }
            val pastCashTxs = pastCash.map {
                FinanceTransaction(
                    id = it.id,
                    timestamp = it.timestamp,
                    amount = it.amount,
                    isDebit = it.isDebit,
                    currency = it.currency,
                    accountLabel = "Cash",
                    merchantOrNote = it.note,
                    isManualCash = true
                )
            }
            val pastAll = FinanceSmsParser.deduplicate(pastSms + pastCashTxs).sortedBy { it.timestamp }

            var closing = opening
            for (tx in pastAll) {
                if (tx.isDebit) closing -= tx.amount else closing += tx.amount
            }

            val equationTape = buildEquationTape(opening, pastAll)
            val currency = pastAll.firstOrNull()?.currency ?: "INR"
            val displayDate = formatDisplayDate(lastDateKey)

            val record = DailyHistoryRecord(
                dateKey = lastDateKey,
                displayDate = displayDate,
                openingBalance = opening,
                equationTape = equationTape,
                closingBalance = closing,
                currency = currency,
                entriesCount = pastAll.size,
                timestamp = System.currentTimeMillis()
            )

            val history = loadHistory(context).toMutableList()
            if (history.none { it.dateKey == lastDateKey }) {
                history.add(0, record)
                saveHistory(context, history)
                _historyFlow.value = history
            }

            // Start today's opening balance with yesterday's closing balance
            setOpeningBalance(context, closing, "All")
            prefs.edit().putString(KEY_LAST_DATE_KEY, todayKey).apply()
        }
    }

    fun buildEquationTape(opening: Double, entries: List<FinanceTransaction>, masked: Boolean = false): String {
        if (masked) {
            if (entries.isEmpty()) return "••••••"
            val sb = StringBuilder("••••••")
            for (e in entries) {
                sb.append(if (e.isDebit) " - •••" else " + •••")
            }
            return sb.toString()
        }
        val sb = StringBuilder(formatBalance(opening))
        for (e in entries) {
            sb.append(if (e.isDebit) " - " else " + ")
            sb.append(formatDelta(e.amount))
        }
        return sb.toString()
    }

    fun formatDelta(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    fun formatBalance(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }

    private fun formatDisplayDate(dateKey: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateKey) ?: return dateKey
            val calToday = Calendar.getInstance()
            val calTarget = Calendar.getInstance().apply { time = date }

            calToday.add(Calendar.DAY_OF_YEAR, -1)
            if (calToday.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calToday.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
            ) {
                return "Yesterday"
            }
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            dateKey
        }
    }

    private fun fetchTodaySmsTransactions(context: Context, todayStartMs: Long): List<FinanceTransaction> {
        val list = mutableListOf<FinanceTransaction>()
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(todayStartMs.toString())
            val sortOrder = "${Telephony.Sms.DATE} ASC"

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val addr = cursor.getString(addrCol) ?: ""
                    val body = cursor.getString(bodyCol) ?: ""
                    val date = cursor.getLong(dateCol)

                    FinanceSmsParser.parse(body, addr, date, id)?.let {
                        list.add(it)
                    }
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    private fun fetchPastSmsTransactions(context: Context, dateKey: String): List<FinanceTransaction> {
        val list = mutableListOf<FinanceTransaction>()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateKey) ?: return emptyList()
            val cal = Calendar.getInstance().apply { time = date }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis
            val endMs = startMs + 86400000L - 1L

            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
            val selectionArgs = arrayOf(startMs.toString(), endMs.toString())
            val sortOrder = "${Telephony.Sms.DATE} ASC"

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val addr = cursor.getString(addrCol) ?: ""
                    val body = cursor.getString(bodyCol) ?: ""
                    val date = cursor.getLong(dateCol)

                    FinanceSmsParser.parse(body, addr, date, id)?.let {
                        list.add(it)
                    }
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    private fun loadOpeningBalances(context: Context): Map<String, Double> {
        val jsonStr = getPrefs(context).getString(KEY_OPENING_BALANCES, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optDouble(k, 0.0)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun loadManualCashEntries(context: Context): List<ManualCashEntry> {
        val jsonStr = getPrefs(context).getString(KEY_MANUAL_CASH_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ManualCashEntry>()
            for (i in 0 until arr.length()) {
                ManualCashEntry.fromJson(arr.getJSONObject(i))?.let { list.add(it) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveManualCashEntries(context: Context, list: List<ManualCashEntry>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(item.toJson())
        }
        getPrefs(context).edit().putString(KEY_MANUAL_CASH_ENTRIES, arr.toString()).apply()
    }

    private fun loadHistory(context: Context): List<DailyHistoryRecord> {
        val jsonStr = getPrefs(context).getString(KEY_DAILY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<DailyHistoryRecord>()
            for (i in 0 until arr.length()) {
                DailyHistoryRecord.fromJson(arr.getJSONObject(i))?.let { list.add(it) }
            }
            list.sortedByDescending { it.dateKey }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(context: Context, list: List<DailyHistoryRecord>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(item.toJson())
        }
        getPrefs(context).edit().putString(KEY_DAILY_HISTORY, arr.toString()).apply()
    }
}
