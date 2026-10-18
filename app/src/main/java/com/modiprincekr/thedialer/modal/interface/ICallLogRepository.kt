package com.modiprincekr.thedialer.modal.`interface`

import com.modiprincekr.thedialer.modal.data.CallLogEntry

interface ICallLogRepository {
    fun getCallLogs(): List<CallLogEntry>
    fun deleteCallLog(entry: CallLogEntry): Boolean
    fun deleteCallLogs(entries: Collection<CallLogEntry>): Boolean
}