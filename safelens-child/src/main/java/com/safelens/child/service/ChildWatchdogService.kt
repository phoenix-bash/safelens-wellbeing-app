package com.safelens.child.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.child.notification.NotificationMonitorService
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.model.DeviceHealthStatus
import com.safelens.core.security.SecurityUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * IPC Watchdog Service in Safelens Child.
 * Receives health audit pings from Safelens Support Security Module.
 * Protected by custom signature-level permission and runtime signature check.
 */
class ChildWatchdogService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ChildWatchdogService = this@ChildWatchdogService
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (!SecurityUtils.isCallerSignatureMatching(this)) {
            Log.e(TAG, "Rejecting IPC bind from unauthorized caller UID!")
            return null
        }
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SecurityUtils.isCallerSignatureMatching(this)) {
            Log.e(TAG, "Rejecting start command from untrusted caller.")
            return START_NOT_STICKY
        }

        when (intent?.action) {
            SafelensIpcConstants.ACTION_HEARTBEAT_PING -> {
                Log.i(TAG, "Heartbeat ping received from Support Security Module.")
                dispatchHealthAck()
            }
        }

        return START_NOT_STICKY
    }

    fun getDeviceHealthStatus(): DeviceHealthStatus {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = bm.isCharging

        val isNotificationEnabled = NotificationMonitorService.isServiceConnected
        val isCallLogPermitted = checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        val isMediaPermitted = checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        return DeviceHealthStatus(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            networkType = "WIFI/CELLULAR",
            isChildAppActive = true,
            isSupportModuleActive = true,
            isNotificationListenerEnabled = isNotificationEnabled,
            isCallLogPermitted = isCallLogPermitted,
            isMediaPermitted = isMediaPermitted,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun dispatchHealthAck() {
        val health = getDeviceHealthStatus()
        val jsonPayload = Json.encodeToString(health)

        val ackIntent = Intent(SafelensIpcConstants.ACTION_HEARTBEAT_ACK).apply {
            setPackage(SafelensIpcConstants.SUPPORT_PACKAGE)
            putExtra(SafelensIpcConstants.EXTRA_PAYLOAD_JSON, jsonPayload)
            putExtra(SafelensIpcConstants.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }

        sendBroadcast(ackIntent, SafelensIpcConstants.PERMISSION_INTERNAL_IPC)
        Log.i(TAG, "Dispatched health ack to Support Module.")
    }
}
