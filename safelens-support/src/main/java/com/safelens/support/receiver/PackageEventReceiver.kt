package com.safelens.support.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.model.SecurityEvent
import com.safelens.core.model.SecurityEventType
import com.safelens.support.SafelensSupportApplication.Companion.TAG
import com.safelens.support.emergency.EmergencyAlertDispatcher
import java.util.UUID

/**
 * High-Priority Tamper Detection Receiver.
 * Receives package lifecycle broadcasts from the Android OS.
 * Fires immediately if the primary child app is removed or its data is cleared.
 */
class PackageEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val affectedPackage = intent.data?.schemeSpecificPart ?: return
        val action = intent.action

        Log.w(TAG, "Package broadcast received: $action for package: $affectedPackage")

        if (affectedPackage == SafelensIpcConstants.CHILD_PACKAGE) {
            when (action) {
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    Log.e(TAG, "CRITICAL: Safelens Child App was uninstalled!")
                    val event = SecurityEvent(
                        id = UUID.randomUUID().toString(),
                        eventType = SecurityEventType.PACKAGE_UNINSTALLED,
                        affectedPackage = affectedPackage,
                        timestamp = System.currentTimeMillis(),
                        description = "Safelens Child App was completely removed from the device.",
                        severity = "CRITICAL"
                    )
                    EmergencyAlertDispatcher.dispatchSecurityAlert(context, event)
                }

                Intent.ACTION_PACKAGE_DATA_CLEARED -> {
                    Log.e(TAG, "CRITICAL: Safelens Child App data was cleared in system settings!")
                    val event = SecurityEvent(
                        id = UUID.randomUUID().toString(),
                        eventType = SecurityEventType.PACKAGE_DATA_CLEARED,
                        affectedPackage = affectedPackage,
                        timestamp = System.currentTimeMillis(),
                        description = "Safelens Child App storage/data was cleared via system settings.",
                        severity = "CRITICAL"
                    )
                    EmergencyAlertDispatcher.dispatchSecurityAlert(context, event)
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    Log.i(TAG, "Safelens Child App was updated/replaced.")
                }
            }
        }
    }
}
