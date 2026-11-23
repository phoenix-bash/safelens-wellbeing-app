package com.safelens.support.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.core.security.SecurityUtils
import com.safelens.support.R
import com.safelens.support.SafelensSupportApplication
import com.safelens.support.SafelensSupportApplication.Companion.CHANNEL_ID
import com.safelens.support.SafelensSupportApplication.Companion.NOTIFICATION_ID
import com.safelens.support.SafelensSupportApplication.Companion.TAG
import com.safelens.support.audit.PermissionAuditWorker
import java.util.concurrent.TimeUnit

/**
 * Headless Security Watchdog Service.
 * Manages background health audits and watchdog keepalives.
 * Protected by signature-level IPC validation.
 */
class HeadlessSecurityService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "HeadlessSecurityService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Run as foreground service to satisfy modern Android background restrictions
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val action = intent?.action
        Log.i(TAG, "HeadlessSecurityService received action: $action")

        when (action) {
            SafelensIpcConstants.ACTION_INITIALIZE_SUPPORT -> {
                handleInitialization(intent)
            }
            SafelensIpcConstants.ACTION_HEARTBEAT_PING -> {
                Log.d(TAG, "Heartbeat ping received from child app.")
            }
        }

        // Schedule periodic background audits using WorkManager
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
            "SafelensPermissionAudit",
            ExistingPeriodicWorkPolicy.KEEP,
            auditRequest
        )
        Log.d(TAG, "Scheduled SafelensPermissionAudit with WorkManager.")
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
