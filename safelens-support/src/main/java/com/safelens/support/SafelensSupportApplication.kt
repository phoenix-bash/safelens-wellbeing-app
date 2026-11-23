package com.safelens.support

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class SafelensSupportApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Safelens Support Security Module initialized.")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_watchdog_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_watchdog_desc)
                setShowBadge(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "SafelensSupport"
        const val CHANNEL_ID = "safelens_security_watchdog_channel"
        const val NOTIFICATION_ID = 2001
    }
}
