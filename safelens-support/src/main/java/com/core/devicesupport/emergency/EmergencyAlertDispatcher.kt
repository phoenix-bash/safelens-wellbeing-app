package com.core.devicesupport.emergency

import android.content.Context
import android.util.Log
import com.core.devicesupport.DeviceSupportApplication.Companion.TAG
import com.safelens.core.model.SecurityEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EmergencyAlertDispatcher {

    private val alertsHistory = mutableListOf<SecurityEvent>()

    fun dispatchSecurityAlert(context: Context, event: SecurityEvent) {
        val jsonPayload = Json.encodeToString(event)
        Log.e(TAG, "EMERGENCY ALERT DISPATCHED: [${event.eventType}] ${event.description}")
        Log.e(TAG, "Payload: $jsonPayload")

        synchronized(alertsHistory) {
            alertsHistory.add(event)
        }
    }

    fun getAlertsHistory(): List<SecurityEvent> {
        return synchronized(alertsHistory) {
            alertsHistory.toList()
        }
    }
}
