package com.custom.simswitcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            val prefsManager = PreferenceManager(context)
            if (prefsManager.isServiceRunning || prefsManager.autoRotationEnabled) {
                Log.d(TAG, "Auto-starting SimRotatorService after boot...")
                SimRotatorService.startService(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
