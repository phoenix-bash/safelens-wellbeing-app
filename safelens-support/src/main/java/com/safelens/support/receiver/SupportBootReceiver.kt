package com.safelens.support.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.safelens.core.ipc.SafelensIpcConstants
import com.safelens.support.SafelensSupportApplication.Companion.TAG
import com.safelens.support.service.HeadlessSecurityService

class SupportBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Device rebooted (${intent.action}). Resuming HeadlessSecurityService...")
                val serviceIntent = Intent(context, HeadlessSecurityService::class.java).apply {
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
