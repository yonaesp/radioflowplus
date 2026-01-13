package com.radioandroid

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.radioandroid.data.FavoritesRepository
import com.radioandroid.premium.PremiumManager
import com.radioandroid.timer.RadioAlarmManager
import com.radioandroid.timer.SleepTimerManager
import com.radioandroid.service.RadioMediaService
import com.radioandroid.ui.RadioApp
import com.radioandroid.ui.components.NotificationPermissionDialog
import com.radioandroid.ui.theme.RadioAndroidTheme
import com.radioandroid.util.AppLogger

class MainActivity : ComponentActivity() {
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    // First launch tracking
    private val PREFS_NAME = "app_prefs"
    private val KEY_FIRST_LAUNCH = "first_launch"
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted or denied - the app will work either way
        // but notifications won't show if denied
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle lock screen visibility if launched from Alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // requestKeyguardDismissKeyguard requires 26+
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        enableEdgeToEdge()
        
        // Initialize managers and logger handled in RadioApplication
        // AppLogger.init(applicationContext)
        // PremiumManager.init(applicationContext)
        // FavoritesRepository.init(applicationContext)
        // SleepTimerManager.init(applicationContext)
        // RadioAlarmManager.init(applicationContext)
        // com.radioandroid.data.ThemePreferences.init(applicationContext)

        
        // Handle intent actions
        handleIntent(intent)
        
        // Check if first launch
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        
        setContent {
            var controller by remember { mutableStateOf<MediaController?>(null) }
            var showNotificationDialog by remember { mutableStateOf(false) }
            
            // Check for first launch and notification permission
            LaunchedEffect(Unit) {
                if (isFirstLaunch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        showNotificationDialog = true
                    }
                    // Mark as not first launch
                    prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                }
            }
            
            // Connect to MediaService
            DisposableEffect(Unit) {
                val sessionToken = SessionToken(
                    applicationContext,
                    ComponentName(applicationContext, RadioMediaService::class.java)
                )
                controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
                controllerFuture?.addListener(
                    {
                        mediaController = controllerFuture?.get()
                        controller = mediaController
                    },
                    MoreExecutors.directExecutor()
                )
                
                onDispose {
                    controllerFuture?.let { MediaController.releaseFuture(it) }
                    mediaController = null
                }
            }
            
            // Notification permission dialog
            if (showNotificationDialog) {
                NotificationPermissionDialog(
                    onConfirm = {
                        showNotificationDialog = false
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onDismiss = {
                        showNotificationDialog = false
                    }
                )
            }
            
            // Dynamic theme detection
            var themeMode by remember { mutableStateOf(com.radioandroid.data.ThemePreferences.getThemeMode()) }
            val systemDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
            
            val isDarkTheme = when (themeMode) {
                com.radioandroid.data.ThemeMode.SYSTEM -> systemDarkMode
                com.radioandroid.data.ThemeMode.LIGHT -> false
                com.radioandroid.data.ThemeMode.DARK -> true
            }
            
            RadioAndroidTheme(darkTheme = isDarkTheme) {
                RadioApp(
                    mediaController = controller,
                    modifier = Modifier.fillMaxSize(),
                    onThemeChanged = { newMode ->
                        com.radioandroid.data.ThemePreferences.setThemeMode(newMode)
                        themeMode = newMode
                    }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == "OPEN_FROM_ALARM") {
            AppLogger.d("MainActivity", "Opened from alarm notification (onNewIntent) - converting to normal playback")
            // Send intent to service to restore media audio attributes
            val serviceIntent = android.content.Intent(this, RadioMediaService::class.java).apply {
                action = RadioMediaService.ACTION_RESTORE_MEDIA_AUDIO
            }
            startService(serviceIntent)
        }
    }
}
