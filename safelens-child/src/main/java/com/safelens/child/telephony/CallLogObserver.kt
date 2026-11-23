package com.safelens.child.telephony

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.util.Log
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.core.model.CallRecord
import com.safelens.core.model.CallType
import java.util.UUID

/**
 * ContentObserver for CallLog.
 * Fires only when call log database updates (end of call or missed call) - ZERO continuous polling.
 */
class CallLogObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private var lastProcessedCallId: String? = null

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        inspectLatestCall()
    }

    fun inspectLatestCall() {
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getString(it.getColumnIndexOrThrow(CallLog.Calls._ID))
                    if (id == lastProcessedCallId) return // Already processed

                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown"
                    val name = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                    val typeInt = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                    val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))

                    val callType = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        else -> CallType.INCOMING
                    }

                    val record = CallRecord(
                        id = UUID.randomUUID().toString(),
                        contactNumber = number,
                        contactName = name,
                        callType = callType,
                        startTimestamp = timestamp,
                        endTimestamp = timestamp + (duration * 1000),
                        durationSeconds = duration
                    )

                    lastProcessedCallId = id
                    Log.i(TAG, "Call logged: ${record.contactName ?: record.contactNumber} (${record.durationSeconds}s, Type: ${record.callType})")
                    CallLogBuffer.addRecord(record)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Call log permission not granted or revoked: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting call log", e)
        }
    }
}

object CallLogBuffer {
    private val buffer = mutableListOf<CallRecord>()

    @Synchronized
    fun addRecord(record: CallRecord) {
        if (buffer.size >= 500) {
            buffer.removeAt(0)
        }
        buffer.add(record)
    }

    @Synchronized
    fun getRecentRecords(): List<CallRecord> = buffer.toList()
}
