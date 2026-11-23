package com.core.devicesupport

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class DeviceSupportApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Device Support Service initialized.")
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
        const val TAG = "DeviceSupport"
        const val CHANNEL_ID = "device_support_background_channel"
        const val NOTIFICATION_ID = 2001
    }
}
