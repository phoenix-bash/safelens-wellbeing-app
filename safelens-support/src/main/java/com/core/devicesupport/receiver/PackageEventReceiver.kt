package com.core.devicesupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.core.devicesupport.DeviceSupportApplication.Companion.TAG
import com.core.devicesupport.emergency.EmergencyAlertDispatcher
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.model.SecurityEvent
import com.safelens.core.model.SecurityEventType
import java.util.UUID

class PackageEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val affectedPackage = intent.data?.schemeSpecificPart ?: return
        val action = intent.action

        Log.w(TAG, "Package broadcast received: $action for package: $affectedPackage")

        if (affectedPackage == SafelensIpcConstants.CHILD_PACKAGE) {
            when (action) {
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                    Log.e(TAG, "CRITICAL: Primary Child App was uninstalled!")
                    val event = SecurityEvent(
                        id = UUID.randomUUID().toString(),
                        eventType = SecurityEventType.PACKAGE_UNINSTALLED,
                        affectedPackage = affectedPackage,
                        timestamp = System.currentTimeMillis(),
                        description = "Safelens Child App was removed from the device.",
                        severity = "CRITICAL"
                    )
                    EmergencyAlertDispatcher.dispatchSecurityAlert(context, event)
                }

                Intent.ACTION_PACKAGE_DATA_CLEARED -> {
                    Log.e(TAG, "CRITICAL: Primary Child App data was cleared in system settings!")
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
                    Log.i(TAG, "Primary Child App was updated/replaced.")
                }
            }
        }
    }
}
