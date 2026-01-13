package com.radioandroid.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import com.radioandroid.data.RadioStation
import com.radioandroid.data.RadioStations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import com.radioandroid.util.AppLogger
import org.json.JSONArray
import org.json.JSONException

/**
 * Manages Radio Alarm functionality with fade-in and persistence
 * Features:
 * - Schedule alarm for specific time
 * - Recurring alarms (specific days)
 * - Persistent configuration across reboots/app restarts
 * - Select favorite station to wake up with
 */
object RadioAlarmManager {
    
    private const val ALARM_REQUEST_CODE = 9001
    private const val PREFS_NAME = "radio_alarm_prefs"
    private const val KEY_ALARM_SET = "is_alarm_set"
    private const val KEY_ALARM_TIME = "alarm_time"
    private const val KEY_ALARM_HOUR = "alarm_hour"
    private const val KEY_ALARM_MINUTE = "alarm_minute"
    private const val KEY_STATION_ID = "station_id"
    private const val KEY_REPEAT_DAYS = "repeat_days" // JSON Array of integers
    
    // Alarm state
    private val _isAlarmSet = MutableStateFlow(false)
    val isAlarmSet: StateFlow<Boolean> = _isAlarmSet.asStateFlow()
    
    private val _alarmTime = MutableStateFlow<Long?>(null)
    val alarmTime: StateFlow<Long?> = _alarmTime.asStateFlow()
    
    private val _alarmStationId = MutableStateFlow<Int?>(null)
    val alarmStationId: StateFlow<Int?> = _alarmStationId.asStateFlow()
    
    private val _repeatDays = MutableStateFlow<List<Int>>(emptyList())
    val repeatDays: StateFlow<List<Int>> = _repeatDays.asStateFlow()
    
    private var prefs: SharedPreferences? = null
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadState()
        
        // If alarm was supposed to be set but time passed (and it's not recurring or persistence failed to re-schedule),
        // we might want to check validity. For now, we trust the persisted state.
        // In a production app, we should check BootReceiver to reschedule system alarms on reboot.
    }
    
    private fun loadState() {
        prefs?.let {
            val isSet = it.getBoolean(KEY_ALARM_SET, false)
            val time = it.getLong(KEY_ALARM_TIME, -1)
            val stationId = it.getInt(KEY_STATION_ID, -1)
            val repeatJson = it.getString(KEY_REPEAT_DAYS, "[]")
            
            val days = mutableListOf<Int>()
            try {
                val jsonArray = JSONArray(repeatJson)
                for (i in 0 until jsonArray.length()) {
                    days.add(jsonArray.getInt(i))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            
            if (isSet && time != -1L && stationId != -1) {
                _isAlarmSet.value = true
                _alarmTime.value = time
                _alarmStationId.value = stationId
                _repeatDays.value = days
                AppLogger.d("RadioAlarmManager", "State loaded: Alarm set for ${formatAlarmTime()} (Station $stationId)")
            } else {
                _isAlarmSet.value = false
                _alarmTime.value = null
                _alarmStationId.value = null
                _repeatDays.value = emptyList()
            }
        }
    }
    
    private fun saveState(isSet: Boolean, time: Long, hour: Int, minute: Int, stationId: Int, days: List<Int>) {
        prefs?.edit()?.apply {
            putBoolean(KEY_ALARM_SET, isSet)
            putLong(KEY_ALARM_TIME, time)
            putInt(KEY_ALARM_HOUR, hour)
            putInt(KEY_ALARM_MINUTE, minute)
            putInt(KEY_STATION_ID, stationId)
            putString(KEY_REPEAT_DAYS, JSONArray(days).toString()) // Store as JSON
            apply()
        }
    }
    
    private fun clearState() {
        prefs?.edit()?.apply {
            putBoolean(KEY_ALARM_SET, false)
            remove(KEY_ALARM_TIME)
            remove(KEY_ALARM_HOUR)
            remove(KEY_ALARM_MINUTE)
            remove(KEY_STATION_ID)
            remove(KEY_REPEAT_DAYS)
            apply()
        }
    }
    
    /**
     * Check if the app has permission to schedule exact alarms
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Schedule an alarm
     */
    fun scheduleAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        stationId: Int,
        repeatDays: List<Int> = emptyList()
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Calculate next alarm time
        val nextTime = calculateNextAlarmTime(hour, minute, repeatDays)
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("station_id", stationId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTime, pendingIntent)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
            
            // Save State
            _isAlarmSet.value = true
            _alarmTime.value = nextTime
            _alarmStationId.value = stationId
            _repeatDays.value = repeatDays
            
            saveState(true, nextTime, hour, minute, stationId, repeatDays)
            
            val calendar = Calendar.getInstance().apply { timeInMillis = nextTime }
            val dayName = when(calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> ""
            }
            
            Toast.makeText(
                context,
                "⏰ Alarma: $dayName ${String.format("%02d:%02d", hour, minute)}",
                Toast.LENGTH_SHORT
            ).show()
            
            AppLogger.d("RadioAlarmManager", "Alarm scheduled for ${formatAlarmTime()} (Station $stationId, Days: $repeatDays)")
            
            return true
        } catch (e: SecurityException) {
            AppLogger.e("RadioAlarmManager", "Permission denied: ${e.message}")
            Toast.makeText(context, "Error: Permiso de alarma denegado", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    private fun calculateNextAlarmTime(hour: Int, minute: Int, days: List<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        if (days.isEmpty()) {
            // One-time alarm
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            // Recurring alarm
            // Find the next day in the list that is valid
            // Check up to 7 days ahead
            for (i in 0..7) {
                val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
                
                // If this day is in our list AND (it's not today OR it is today but time is in future)
                // Note: If i=0 (today), we check if target > now. 
                // If i>0, any match is valid because we already added days.
                if (days.contains(dayOfWeek)) {
                    if (i == 0 && target.after(now)) {
                        return target.timeInMillis // Today later
                    } else if (i > 0) {
                        return target.timeInMillis // Future day
                    }
                }
                
                // Move to next day
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            // Should not happen if list is not empty, but fallback to tomorrow
            target.timeInMillis = System.currentTimeMillis() + 86400000
            return target.timeInMillis
        }
    }
    
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        
        _isAlarmSet.value = false
        _alarmTime.value = null
        // _alarmStationId.value = null // Optional: keep selection
        
        clearState()
        
        AppLogger.d("RadioAlarmManager", "Alarm cancelled")
        Toast.makeText(context, "Alarma cancelada", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Called when alarm triggers
     * If recurring, reschedule for next occurrence.
     * If onetime, clear state.
     */
    fun onAlarmTriggered(context: Context) {
        val days = _repeatDays.value
        val stationId = _alarmStationId.value
        
        // Need to read hour/minute from stored prefs because we only have next timestamp
        var storedHour = -1
        var storedMinute = -1
        
        prefs?.let {
            storedHour = it.getInt(KEY_ALARM_HOUR, -1)
            storedMinute = it.getInt(KEY_ALARM_MINUTE, -1)
        }
        
        if (days.isNotEmpty() && stationId != null && storedHour != -1) {
            AppLogger.d("RadioAlarmManager", "Rescheduling recurring alarm...")
            scheduleAlarm(context, storedHour, storedMinute, stationId, days)
        } else {
            // One-time alarm finished
            _isAlarmSet.value = false
            _alarmTime.value = null
            clearState() // Clear perstisted "isSet" flag
        }
    }
    
    fun getAlarmStation(): RadioStation? {
        val stationId = _alarmStationId.value ?: return null
        return RadioStations.stations.find { it.id == stationId }
    }
    
    fun formatAlarmTime(): String {
        val time = _alarmTime.value ?: return ""
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        return String.format(
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
    
    fun formatRepeatDays(): String {
        val days = _repeatDays.value
        if (days.isEmpty()) return "Solo una vez"
        if (days.size == 7) return "Todos los días"
        
        if (days.containsAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) && days.size == 5) {
            return "Lun-Vie"
        }
        
        val dayNames = days.sorted().map { day ->
            when (day) {
                Calendar.MONDAY -> "Lun"
                Calendar.TUESDAY -> "Mar"
                Calendar.WEDNESDAY -> "Mié"
                Calendar.THURSDAY -> "Jue"
                Calendar.FRIDAY -> "Vie"
                Calendar.SATURDAY -> "Sáb"
                Calendar.SUNDAY -> "Dom"
                else -> ""
            }
        }
        return dayNames.joinToString(", ")
    }
    
    /**
     * Reschedules alarm after boot using stored preferences
     */
    fun rescheduleAlarmOnBoot(context: Context) {
        val isSet = _isAlarmSet.value
        val stationId = _alarmStationId.value
        val days = _repeatDays.value
        
        if (isSet && stationId != null) {
            // Need to read hour/minute from stored prefs
            var storedHour = -1
            var storedMinute = -1
            
            prefs?.let {
                storedHour = it.getInt(KEY_ALARM_HOUR, -1)
                storedMinute = it.getInt(KEY_ALARM_MINUTE, -1)
            }
            
            if (storedHour != -1 && storedMinute != -1) {
                AppLogger.d("RadioAlarmManager", "Boot: Rescheduling alarm for $storedHour:$storedMinute")
                scheduleAlarm(context, storedHour, storedMinute, stationId, days)
                
                Toast.makeText(context, "⏰ Alarma restaurada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * BroadcastReceiver that handles alarm trigger
 * Starts RadioMediaService with the station to play
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Init logger defensively
        try { AppLogger.init(context) } catch (e: Exception) { e.printStackTrace() }
        
        AppLogger.d(TAG, "⏰ ALARM RECEIVED at ${System.currentTimeMillis()}")
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RadioAndroid::AlarmReceiverWakeLock"
        )
        
        try {
            wakeLock.acquire(30 * 1000L) // 30 seconds safety window
            AppLogger.d(TAG, "WakeLock acquired")
            
            val stationId = intent.getIntExtra("station_id", -1)
            
            // Start Service IMMEDIATELY
            val serviceIntent = Intent(context, com.radioandroid.service.RadioMediaService::class.java).apply {
                action = com.radioandroid.service.RadioMediaService.ACTION_ALARM_PLAY
                putExtra(com.radioandroid.service.RadioMediaService.EXTRA_STATION_ID, stationId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppLogger.d(TAG, "Calling startForegroundService")
                context.startForegroundService(serviceIntent)
            } else {
                AppLogger.d(TAG, "Calling startService")
                context.startService(serviceIntent)
            }
            
            // Handle recurrence or cleanup
            // Need to pass context to reschedule
            RadioAlarmManager.onAlarmTriggered(context)
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "CRITICAL FAILURE in AlarmReceiver: ${e.message}")
            e.printStackTrace()
        } finally {
            // CRITICAL: Always release WakeLock to prevent battery drain
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    AppLogger.d(TAG, "WakeLock released in finally")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error releasing WakeLock: ${e.message}")
            }
        }
    }
}
