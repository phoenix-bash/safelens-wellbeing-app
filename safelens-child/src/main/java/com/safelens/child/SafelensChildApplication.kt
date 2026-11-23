package com.safelens.child

import android.app.Application
import android.util.Log

class SafelensChildApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Safelens Child Application initialized.")
    }

    companion object {
        const val TAG = "SafelensChild"
    }
}
