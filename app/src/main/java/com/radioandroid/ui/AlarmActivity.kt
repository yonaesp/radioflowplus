package com.radioandroid.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioandroid.util.AppLogger
import com.radioandroid.data.RadioStations
import com.radioandroid.service.RadioMediaService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.radioandroid.MainActivity
import com.radioandroid.R
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    
    // BroadcastReceiver to close this activity when alarm is stopped from notification
    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppLogger.d("AlarmActivity", "Received close broadcast - finishing activity")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AppLogger.d("AlarmActivity", "onCreate started")
        
        // Register receiver to close when alarm is stopped from notification
        val filter = IntentFilter("com.radioandroid.CLOSE_ALARM_ACTIVITY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        // Keep CPU AND SCREEN running while alarm is displayed
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "RadioAndroid::AlarmActivityWakeLock"
        )
        wakeLock?.acquire(5 * 60 * 1000L) // 5 minutes max (auto-release safety)
        AppLogger.d("AlarmActivity", "WakeLock acquired for 5m")

        // Show over lock screen with all necessary flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        // Add window flags for all versions
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        
        android.util.Log.d("AlarmActivity", "Window flags set")
        


        val stationId = intent.getIntExtra(RadioMediaService.EXTRA_STATION_ID, -1)
        AppLogger.d("AlarmActivity", "Station ID: $stationId")
        
        // NOTE: We do NOT start the service here anymore - it's already running
        // The service launches this activity, not the other way around
        // Starting it again would create an infinite loop
        
        AppLogger.d("AlarmActivity", "Activity setup complete, showing UI")
        val station = RadioStations.stations.find { it.id == stationId }
        val stationName = station?.name ?: "Radio Alarma"
        val stationGenre = station?.genre ?: "Despierta con música"

        setContent {
            MaterialTheme(
               colorScheme = androidx.compose.material3.darkColorScheme()
            ) {
                AlarmScreen(
                    stationName = stationName,
                    stationGenre = stationGenre,
                    onSnoozeAlarm = {
                        snoozeAlarm()
                    },
                    onOpenApp = {
                        openApp()
                    },
                    onStopAlarm = {
                        stopAlarm()
                    }
                )
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // Hide system bars for immersive experience using WindowCompat (safer)
        // Doing this in onAttachedToWindow ensures DecorView is ready, preventing NPEs on some devices/lock screens
        try {
            // Check if window and decorView are available (redundant but safe)
            window?.decorView?.let { decorView ->
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, decorView)
                windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        } catch (e: Exception) {
            AppLogger.e("AlarmActivity", "Error setting window insets controller: ${e.message}")
        }
    }
    
    private fun snoozeAlarm() {
        AppLogger.d("AlarmActivity", "Snoozing alarm for 5 minutes")
        
        // Stop current alarm
        val stopIntent = Intent(this, RadioMediaService::class.java).apply {
            action = RadioMediaService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
        
        // Schedule new alarm for 5 minutes from now
        val snoozeIntent = Intent(this, RadioMediaService::class.java).apply {
            action = RadioMediaService.ACTION_SNOOZE_ALARM
            putExtra(RadioMediaService.EXTRA_STATION_ID, intent.getIntExtra(RadioMediaService.EXTRA_STATION_ID, -1))
        }
        startService(snoozeIntent)
        
        finish()
    }
    
    private fun openApp() {
        AppLogger.d("AlarmActivity", "Opening app from lock screen")
        
        // Open MainActivity with special action to convert alarm to normal playback
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "OPEN_FROM_ALARM"
        }
        startActivity(mainIntent)
        finish()
    }
    
    private fun stopAlarm() {
        val stopIntent = Intent(this, RadioMediaService::class.java).apply {
            action = RadioMediaService.ACTION_STOP_ALARM
        }
        AppLogger.d("AlarmActivity", "Stopping alarm via intent")
        startService(stopIntent)
        finish()
    }
    
    override fun onDestroy() {
        // Unregister the close receiver
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        
        AppLogger.d("AlarmActivity", "onDestroy called")
        
        // Release WakeLock when activity is destroyed - extra safety with try-catch
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    AppLogger.d("AlarmActivity", "WakeLock released")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AlarmActivity", "WakeLock release failed: ${e.message}")
        }
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(
    stationName: String,
    stationGenre: String,
    onSnoozeAlarm: () -> Unit,
    onOpenApp: () -> Unit,
    onStopAlarm: () -> Unit
) {
    var currentTime by remember { androidx.compose.runtime.mutableStateOf(Date()) }
    
    // Update clock every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }
    
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Deep black/grey background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = timeFormat.format(currentTime),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = dateFormat.format(currentTime).replaceFirstChar { it.uppercase() },
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Middle Section: Station Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stationName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stationGenre,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            
            // Bottom Section: Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onSnoozeAlarm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726) // Orange color for snooze
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POSPONER 5 MIN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32) // Dark Green for Open
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_radio),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ABRIR APP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Alternative: Slide to Stop
                Text(
                    text = "o desliza para detener",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SlideToStopButton(onSlideComplete = onStopAlarm)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SlideToStopButton(onSlideComplete: () -> Unit) {
    val width = 280.dp
    val dragSize = 56.dp
    val maxDrag = with(LocalDensity.current) { (width - dragSize).toPx() }
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .width(width)
            .height(dragSize)
            .background(Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        // Text behind
        Text(
            text = "Desliza para detener  >>>",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        
        // Draggable Circle
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(dragSize)
                .background(Color.White, CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newValue = offsetX + delta
                        offsetX = newValue.coerceIn(0f, maxDrag)
                        
                        if (offsetX >= maxDrag * 0.9f) {
                            onSlideComplete()
                        }
                    },
                    onDragStopped = {
                        if (offsetX < maxDrag * 0.9f) {
                            // Snap back
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Black
            )
        }
    }
}
