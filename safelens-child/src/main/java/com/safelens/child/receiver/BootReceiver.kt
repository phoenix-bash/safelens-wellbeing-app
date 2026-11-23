package com.safelens.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.child.telephony.CallLogObserver
import com.safelens.child.media.MediaSafetyObserver

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Device rebooted or package updated (${intent.action}). Initializing observers...")
                
                // Initialize CallLogObserver
                try {
                    val callLogObserver = CallLogObserver(context)
                    context.contentResolver.registerContentObserver(
                        android.provider.CallLog.Calls.CONTENT_URI,
                        true,
                        callLogObserver
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not register CallLogObserver on boot", e)
                }

                // Initialize MediaSafetyObserver
                try {
                    val mediaSafetyObserver = MediaSafetyObserver(context)
                    context.contentResolver.registerContentObserver(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true,
                        mediaSafetyObserver
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not register MediaSafetyObserver on boot", e)
                }
            }
        }
    }
}
