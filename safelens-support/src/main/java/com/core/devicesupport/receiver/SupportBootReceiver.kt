package com.core.devicesupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.core.devicesupport.DeviceSupportApplication.Companion.TAG
import com.core.devicesupport.service.DeviceSupportService
import com.safelens.core.ipc.SafelensIpcConstants

class SupportBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Device rebooted (${intent.action}). Resuming DeviceSupportService...")
                val serviceIntent = Intent(context, DeviceSupportService::class.java).apply {
                    action = SafelensIpcConstants.ACTION_INITIALIZE_SUPPORT
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
