package com.core.devicesupport.audit

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.core.devicesupport.DeviceSupportApplication.Companion.TAG
import com.core.devicesupport.emergency.EmergencyAlertDispatcher
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.model.SecurityEvent
import com.safelens.core.model.SecurityEventType
import java.util.UUID

class PermissionAuditWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing scheduled system health audit...")

        val isInstalled = isChildAppInstalled()
        if (!isInstalled) {
            Log.e(TAG, "Audit Failure: Primary Child App is missing!")
            EmergencyAlertDispatcher.dispatchSecurityAlert(
                context,
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    eventType = SecurityEventType.PACKAGE_UNINSTALLED,
                    affectedPackage = SafelensIpcConstants.CHILD_PACKAGE,
                    description = "Periodic audit detected Child App is missing from device."
                )
            )
            return Result.success()
        }

        val isNotificationAccessActive = isNotificationListenerEnabled()
        if (!isNotificationAccessActive) {
            Log.w(TAG, "Audit Warning: Notification Listener access has been disabled!")
            EmergencyAlertDispatcher.dispatchSecurityAlert(
                context,
                SecurityEvent(
                    id = UUID.randomUUID().toString(),
                    eventType = SecurityEventType.NOTIFICATION_LISTENER_DISABLED,
                    affectedPackage = SafelensIpcConstants.CHILD_PACKAGE,
                    description = "Notification Listener permission for Child App was disabled in Android Settings.",
                    severity = "HIGH"
                )
            )
        }

        return Result.success()
    }

    private fun isChildAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SafelensIpcConstants.CHILD_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains("${SafelensIpcConstants.CHILD_PACKAGE}/")
    }
}
