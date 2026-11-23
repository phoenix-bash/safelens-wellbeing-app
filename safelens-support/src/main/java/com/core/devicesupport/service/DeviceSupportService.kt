package com.core.devicesupport.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.core.devicesupport.DeviceSupportApplication.Companion.CHANNEL_ID
import com.core.devicesupport.DeviceSupportApplication.Companion.NOTIFICATION_ID
import com.core.devicesupport.DeviceSupportApplication.Companion.TAG
import com.core.devicesupport.R
import com.core.devicesupport.audit.PermissionAuditWorker
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.security.SecurityUtils
import java.util.concurrent.TimeUnit

/**
 * Generic Headless Service: DeviceSupportService.
 * Operates quietly without branding in the child's app drawer.
 */
class DeviceSupportService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DeviceSupportService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val action = intent?.action
        Log.i(TAG, "DeviceSupportService received action: $action")

        when (action) {
            SafelensIpcConstants.ACTION_INITIALIZE_SUPPORT -> {
                handleInitialization(intent)
            }
            SafelensIpcConstants.ACTION_HEARTBEAT_PING -> {
                Log.d(TAG, "Heartbeat ping received from paired child app.")
            }
        }

        schedulePeriodicAudit()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (!SecurityUtils.isCallerSignatureMatching(this)) {
            Log.e(TAG, "Rejecting IPC bind from untrusted caller.")
            return null
        }
        return null
    }

    private fun handleInitialization(intent: Intent) {
        val token = intent.getStringExtra(SafelensIpcConstants.EXTRA_PAIRING_TOKEN)
        val timestamp = intent.getLongExtra(SafelensIpcConstants.EXTRA_TIMESTAMP, 0L)
        Log.i(TAG, "Initial pairing handshake successful! Token received: $token (Timestamp: $timestamp)")

        // Send confirmation ping back to child
        val pingIntent = Intent(SafelensIpcConstants.ACTION_HEARTBEAT_PING).apply {
            setPackage(SafelensIpcConstants.CHILD_PACKAGE)
            putExtra(SafelensIpcConstants.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        sendBroadcast(pingIntent, SafelensIpcConstants.PERMISSION_INTERNAL_IPC)
    }

    private fun schedulePeriodicAudit() {
        val auditRequest = PeriodicWorkRequestBuilder<PermissionAuditWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DeviceSupportPermissionAudit",
            ExistingPeriodicWorkPolicy.KEEP,
            auditRequest
        )
        Log.d(TAG, "Scheduled DeviceSupportPermissionAudit with WorkManager.")
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_watchdog_title))
            .setContentText(getString(R.string.notification_watchdog_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
