package com.radioandroid.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.radioandroid.util.AppLogger

/**
 * Reschedules alarms after device reboot.
 * Required because Android clears all alarms when the device is turned off.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                AppLogger.init(context)
                AppLogger.d("BootReceiver", "📱 Device booted. Rescheduling alarms...")
                
                // Initialize Manager to load preferences
                RadioAlarmManager.init(context)
                
                // If an alarm is set in preferences, RadioAlarmManager needs a method to 
                // re-schedule based on the stored data.
                // We'll call a new method 'rescheduleAlarmOnBoot'
                RadioAlarmManager.rescheduleAlarmOnBoot(context)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
