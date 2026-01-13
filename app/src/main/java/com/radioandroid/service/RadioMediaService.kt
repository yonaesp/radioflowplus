package com.radioandroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.provider.Settings
import android.net.Uri
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.content.pm.ServiceInfo
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.radioandroid.MainActivity
import com.radioandroid.R
import com.radioandroid.data.RadioStation

import com.radioandroid.data.RadioStations
import com.radioandroid.timer.RadioAlarmManager
import com.radioandroid.player.StreamingPlayerFactory
import com.radioandroid.player.IcyDataSource
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import com.radioandroid.util.SmartNetworkValidator
import com.radioandroid.util.NetworkMonitor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

import com.radioandroid.util.AppLogger
import com.radioandroid.util.NavigationUtils
import com.radioandroid.util.ErrorClassifier
import com.radioandroid.util.SmartRetryManager
import com.radioandroid.util.OverlayNotificationManager
import com.radioandroid.util.NetworkState
import com.radioandroid.util.ErrorType
import com.radioandroid.util.RetryStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
 import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.radioandroid.widget.WidgetStateManager

/**
 * MediaLibraryService for Android Auto and background playback
 * Provides media browsing and playback capabilities
 * 
 * Features robust error handling:
 * - If a station fails, reverts to previous working station
 * - Skip commands auto-skip to next available station
 * - Shows toast messages for unavailable stations
 */
@OptIn(UnstableApi::class)
class RadioMediaService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private lateinit var playbackController: Player // ForwardingPlayer with Time-Shift logic
    
    // Artwork cache to avoid repeated decoding
    private val artworkCache = mutableMapOf<Int, ByteArray>()
    
    // Artwork Fetcher (iTunes API)

    
    // Error handling state
    private var previousWorkingStation: RadioStation? = null
    private var currentAttemptedStation: RadioStation? = null
    private var isSkipping = false  // True when using skip controls
    private var skipAttempts = 0    // Counter to prevent infinite loops
    private var retryCount = 0      // Counter for playback retries (e.g. for ads)
    private var isTransitioning = false  // True during station transitions to prevent notification flicker
    private var isAacSeamlessRestart = false  // True during AAC seamless restart to suppress notification updates
    private var isWaitingForNetwork = false   // True when paused due to global network failure
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastErrorTime = 0L       // Track last error time for exponential backoff or throttling
    private val MIN_RETRY_INTERVAL = 2000L // Minimum 2 seconds between retries just in case
    private var playbackStartTime = 0L   // Track when playback started for long-session error handling
    
    // Volume fading
    private var fadeJob: Job? = null
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
            AppLogger.e("ServiceScope", "Coroutine exception: ${throwable.message}", throwable)
        }
    )
    
    // Track if using alarm audio stream (to restore media stream when paused)
    @Volatile
    private var isUsingAlarmAudio = false
    private var alarmStationId: Int = -1 // Station ID for alarm notification fullScreenIntent
    
    // Auto-stop alarm after 15 minutes to prevent forgotten playback
    private val ALARM_AUTO_STOP_MS = 15 * 60 * 1000L // 15 minutes
    private var alarmAutoStopRunnable: Runnable? = null

    // Smart Network Error Detection
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var errorClassifier: ErrorClassifier
    private lateinit var smartRetryManager: SmartRetryManager
    private lateinit var overlayManager: OverlayNotificationManager
    
    
    // Intelligent Buffering Timeout Detection (Progressive timeout for network edge cases)
    // Handles: Slow networks, tunnels, false 4G/5G, broken URLs
    private var bufferingStartTime: Long = 0
    private var bufferingTimeoutHandler: Runnable? = null
    private var lastBufferedPosition: Long = 0 // Track if data is being downloaded
    private var bufferingRetryCount: Int = 0  // How many times we've given it a second chance
    private var hasReceivedDefinitiveError: Boolean = false // Flag to cancel timeout on definitive errors
    private val BUFFERING_TIMEOUT_MS = 4_000L // Initial timeout: 4s
    private val MAX_BUFFERING_RETRIES = 1     // Give one more chance if progress detected
    private val MAX_TOTAL_BUFFERING_MS = 8_000L // Absolute maximum: 8s total

    // Audio Focus: ExoPlayer handles this natively via handleAudioFocus=true
    // CONTENT_TYPE_MUSIC enables automatic ducking when other apps request MAY_DUCK
    // Pause happens automatically when other apps request exclusive focus (GAIN/TRANSIENT)
    private var wasPlayingBeforeFocusLoss = false // Track for Jump-to-Live on resume

    private fun seekToNextStation() {
        val current = currentAttemptedStation ?: return
        val next = NavigationUtils.getNextStation(current)
        
        // Show feedback toast if filtering is active
        val useCountryFilter = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
        val useGenreFilter = com.radioandroid.data.AppPreferences.isGenreNavigationEnabled
        if (useCountryFilter || useGenreFilter) {
             val symbol = if (useCountryFilter) "🌍" else "🎵"
             Toast.makeText(this, "⏭️ $symbol ${next.name}", Toast.LENGTH_SHORT).show()
        }
        
        playStation(next)
    }
    
    // WifiLock to keep network alive during sleep
    private var wifiLock: WifiManager.WifiLock? = null
    
    // NetworkCallback for waiting for connectivity (instead of polling)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var stationWaitingForNetwork: RadioStation? = null

    // Time-Shift State
    private var pauseTimestamp: Long = 0
    private var delayTickerJob: Job? = null

    companion object {

        private const val NOTIFICATION_ID = 1001
        private const val ALARM_NOTIFICATION_ID = 1002  // Separate ID for alarms to prevent conflicts
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val ALARM_CHANNEL_ID = "radio_alarm_channel"
        
        // Media browsing tree structure
        private const val ROOT_ID = "root"
        private const val STATIONS_ID = "stations"
        
        // Genre-based browsing
        private const val GENRE_POP = "genre_pop"
        private const val GENRE_NEWS = "genre_news"
        private const val GENRE_ROCK = "genre_rock"
        private const val GENRE_DANCE = "genre_dance"
        private const val GENRE_OTHER = "genre_other"
        
        // Custom actions
        private const val ACTION_SKIP_NEXT = "com.radioandroid.SKIP_NEXT"
        private const val ACTION_SKIP_PREVIOUS = "com.radioandroid.SKIP_PREVIOUS"
        
        // Alarm action - called by AlarmReceiver
        const val ACTION_ALARM_PLAY = "com.radioandroid.ALARM_PLAY"
        const val EXTRA_STATION_ID = "station_id"
        const val ACTION_RESTORE_MEDIA_AUDIO = "com.radioandroid.RESTORE_MEDIA_AUDIO"
        const val ACTION_STOP_ALARM = "com.radioandroid.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.radioandroid.SNOOZE_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logger to catch any early crashes
        AppLogger.init(applicationContext)
        AppLogger.d("RadioMediaService", "Service onCreate")
        
        // Audio focus handled by ExoPlayer natively
        
        // Initialize network monitoring components
        networkMonitor = NetworkMonitor(this)
        errorClassifier = ErrorClassifier(networkMonitor)
        smartRetryManager = SmartRetryManager()
        overlayManager = OverlayNotificationManager(this)
        
        // Create player preferences for auto-resume
        com.radioandroid.data.AppPreferences.init(applicationContext)
        
        // Initialize WifiLock
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifiManager.createWifiLock(lockMode, "RadioAndroid::WifiLock")
        wifiLock?.setReferenceCounted(false)
        
        createNotificationChannel()
        initializePlayer()
        initializeMediaSession()
        
        // Start monitoring network changes for auto-resume
        serviceScope.launch {
            networkMonitor.observeNetworkChanges().collect { state ->
                val isConnected = state != NetworkState.DISCONNECTED
                if (isConnected && isWaitingForNetwork) {
                    AppLogger.d("RadioMediaService", "✅ Network restored! Auto-resuming...")
                    showToast("✅ Red restaurada - Reanudando...")
                    isWaitingForNetwork = false
                    currentAttemptedStation?.let { playStation(it) }
                }
            }
        }


    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Radio stream playback controls"
                setShowBadge(false)
            }
            
            // High priority channel for Alarms to ensure FullScreenIntent and heads-up display
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Radio Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                setShowBadge(true)
                setSound(null, null) // We'll set sound on notification itself
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // CRITICAL: Allow alarm notifications to bypass Do Not Disturb
                setBypassDnd(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(alarmChannel)
            
            // Log channel importance to verify configuration
            val createdChannel = notificationManager.getNotificationChannel(ALARM_CHANNEL_ID)
            AppLogger.d("RadioMediaService", "Alarm channel created with importance: ${createdChannel?.importance}")
        }
    }


    private fun initializePlayer() {
        // Use optimized streaming player factory for live radio
        player = StreamingPlayerFactory.createOptimizedPlayer(
            this
        )
        
        // Audio focus is handled natively by ExoPlayer with CONTENT_TYPE_MUSIC
        // Ducking happens automatically when other apps request MAY_DUCK
        
        // SMART AUTO-RESUME LOGIC (Spotify-style)
        // Only if enabled in settings
        if (com.radioandroid.data.AppPreferences.isAutoResumeEnabled) {
            val lastTime = com.radioandroid.data.AppPreferences.lastPlaybackTime
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastTime
            
            // Only resume if within 48 hours
            if (timeDiff < 48 * 60 * 60 * 1000L) {
                val lastStationId = com.radioandroid.data.AppPreferences.lastStationId
                val station = RadioStations.stations.find { it.id == lastStationId }
                
                if (station != null) {
                    AppLogger.d("RadioMediaService", "Auto-Resume: Restoring session for ${station.name}")
                    
                    // Restore PREPARED state but DO NOT PLAY (wait for Android Auto/User)
                    currentAttemptedStation = station
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(station.id.toString())
                        .setUri(station.streamUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setSubtitle(station.genre)
                                .setArtist(station.genre)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                                .setArtworkData(
                                    null, // Will be loaded async if needed
                                    MediaMetadata.PICTURE_TYPE_FRONT_COVER
                                )
                                .build()
                        )
                        .build()
                        
                    player.setMediaItem(mediaItem)
                    player.prepare() // Buffer and get ready
                    // remove player.play() -> This is the key for "Smart Resume"
                    
                    // Show notification in paused state so it appears in AA
                    updateNotification()
                }
            } else {
                AppLogger.d("RadioMediaService", "Auto-Resume: Last session expired (>48h), starting fresh")
            }
        }

        // Add notification update listener
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                try {
                    val reasonString = when (reason) {
                        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
                        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
                        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
                        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
                        else -> "UNKNOWN($reason)"
                    }
                    AppLogger.d("RadioMediaService", "onMediaItemTransition: reason=$reasonString, isTransitioning=$isTransitioning, newStation=${mediaItem?.mediaMetadata?.title}")
                    
                    // CRITICAL: Update currentAttemptedStation from mediaId
                    // This ensures error handling works even when playback started via MediaController (not playStationInternal)
                    mediaItem?.mediaId?.let { mediaId ->
                        val station = RadioStations.stations.find { it.id.toString() == mediaId }
                        if (station != null && currentAttemptedStation?.id != station.id) {
                            AppLogger.d("RadioMediaService", "onMediaItemTransition: Setting currentAttemptedStation to ${station.name}")
                            currentAttemptedStation = station
                            retryCount = 0 // Reset retries for new station
                        }
                    }
                    
                    // Mark as transitioning if not already set (catches all transition routes)
                    if (!isTransitioning && mediaItem != null) {
                        AppLogger.d("RadioMediaService", "onMediaItemTransition: Detected transition not set by playStationInternal, activating isTransitioning=true")
                        isTransitioning = true
                    }
                    
                    // Use forcePlayingState when in alarm mode OR during transition to maintain notification
                    if (isUsingAlarmAudio || isTransitioning) {
                        updateNotification(forcePlayingState = true)
                    } else {
                        updateNotification()
                    }
                    updateAppWidgetWithColor()
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onMediaItemTransition", e)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                try {
                    AppLogger.d("RadioMediaService", "onIsPlayingChanged: isPlaying=$isPlaying, isTransitioning=$isTransitioning, isUsingAlarmAudio=$isUsingAlarmAudio")
                    
                    // SKIP notification updates during AAC seamless restart to prevent flicker
                    if (isAacSeamlessRestart) {
                        AppLogger.d("AAC_DEBUG", "Skipping notification update during AAC seamless restart")
                        // Still manage WifiLock
                        if (isPlaying && wifiLock?.isHeld == false) wifiLock?.acquire()
                        return
                    }
                    
                    // If using alarm audio, force playing state in notification
                    if (isUsingAlarmAudio) {
                        updateNotification(forcePlayingState = true)
                    } else {
                        updateNotification()
                    }
                    updateAppWidgetWithColor()
                    
                    // Manage WifiLock based on playback
                    if (isPlaying) {
                         if (wifiLock?.isHeld == false) wifiLock?.acquire()
                    } else {
                         if (wifiLock?.isHeld == true) wifiLock?.release()
                    }
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onIsPlayingChanged", e)
                }
            }
            
            /**
             * CRITICAL: Detect when ExoPlayer stops suppressing playback after focus regain
             * This is where we implement "Jump to Live" for non-TimeShift users
             * 
             * With handleAudioFocus=true, ExoPlayer doesn't change playWhenReady.
             * Instead it uses "playback suppression" internally. This callback detects
             * when suppression ends (focus regained) so we can trigger Jump-to-Live.
             */
            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                try {
                    val reasonString = when (playbackSuppressionReason) {
                        Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "NONE"
                        Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS -> "TRANSIENT_AUDIO_FOCUS_LOSS"
                        else -> "UNKNOWN($playbackSuppressionReason)"
                    }
                    AppLogger.d("RadioMediaService", "onPlaybackSuppressionReasonChanged: reason=$reasonString, wasSuppressed=$wasPlayingBeforeFocusLoss")
                    
                    // Track when playback is suppressed due to focus loss
                    if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
                        wasPlayingBeforeFocusLoss = true
                        AppLogger.d("RadioMediaService", "Playback suppressed due to focus loss - marking for Jump-to-Live")
                    }
                    
                    // Detect when suppression ends (focus regained)
                    if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE && wasPlayingBeforeFocusLoss) {
                        wasPlayingBeforeFocusLoss = false
                        
                        val isPremium = com.radioandroid.premium.PremiumManager.isPremium.value
                        val isTimeShiftEnabled = com.radioandroid.data.AppPreferences.isTimeShiftEnabled
                        
                        // If Time-Shift is OFF: Jump to live stream
                        if (!isPremium || !isTimeShiftEnabled) {
                            AppLogger.d("RadioMediaService", "Focus regained + TimeShift OFF -> Jumping to LIVE")
                            com.radioandroid.player.PlayerState.setDelay(0)
                            player.seekToDefaultPosition()
                        } else {
                            AppLogger.d("RadioMediaService", "Focus regained + TimeShift ON -> Resuming from buffer")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onPlaybackSuppressionReasonChanged", e)
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    val stateString = when (playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN($playbackState)"
                    }
                    AppLogger.d("RadioMediaService", "onPlaybackStateChanged: state=$stateString, isTransitioning=$isTransitioning, isPlaying=${player.isPlaying}")
                    
                     // Save state for Auto-Resume
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                        currentAttemptedStation?.let {
                            com.radioandroid.data.AppPreferences.lastStationId = it.id
                            com.radioandroid.data.AppPreferences.lastPlaybackTime = System.currentTimeMillis()
                            com.radioandroid.data.AppPreferences.wasPlaying = player.isPlaying
                        }
                    }
                    
                    when (playbackState) {
                        Player.STATE_READY -> {
                            
                            // Cancel buffering timeout - station loaded successfully
                            bufferingTimeoutHandler?.let { mainHandler.removeCallbacks(it) }
                            bufferingTimeoutHandler = null
                            bufferingStartTime = 0
                            lastBufferedPosition = 0
                            bufferingRetryCount = 0
                            
                            // Reset AAC seamless restart flag - ready means restart was successful
                            if (isAacSeamlessRestart) {
                                AppLogger.d("AAC_DEBUG", "AAC seamless restart complete - clearing flag")
                                isAacSeamlessRestart = false
                            }
                            
                            // Station is working, save it as the previous working station
                            currentAttemptedStation?.let {
                                previousWorkingStation = it
                                skipAttempts = 0  // Reset skip counter
                                retryCount = 0    // Reset retry counter on successful playback
                            }
                            // Record playback start time for long-session tracking
                            if (playbackStartTime == 0L) {
                                playbackStartTime = System.currentTimeMillis()
                                AppLogger.d("RadioMediaService", "Playback session started at $playbackStartTime")
                            }
                            // End transition - station is ready
                            AppLogger.d("RadioMediaService", "STATE_READY: Setting isTransitioning=false")
                            isTransitioning = false
                            
                            // Force playing state if alarm is active
                            if (isUsingAlarmAudio) {
                                updateNotification(forcePlayingState = true)
                            } else {
                                updateNotification()
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            AppLogger.d("RadioMediaService", "STATE_BUFFERING: isTransitioning=$isTransitioning, will force=${isTransitioning || isUsingAlarmAudio}")
                            
                            // INTELLIGENT PROGRESSIVE TIMEOUT SYSTEM
                            // Handles: Slow networks, tunnels, false 4G/5G, broken URLs
                            if (bufferingStartTime == 0L) {
                                bufferingStartTime = System.currentTimeMillis()
                                lastBufferedPosition = player.bufferedPosition
                                bufferingRetryCount = 0
                                hasReceivedDefinitiveError = false // Reset on new station attempt
                                
                                // Create timeout check function
                                fun scheduleBufferingCheck() {
                                    bufferingTimeoutHandler = Runnable {
                                        val bufferingDuration = System.currentTimeMillis() - bufferingStartTime
                                        val currentBufferedPosition = player.bufferedPosition
                                        val hasProgress = currentBufferedPosition > lastBufferedPosition
                                        
                                        if (player.playbackState == Player.STATE_BUFFERING) {
                                            AppLogger.w("RadioMediaService", "⏰ Buffering check: ${bufferingDuration}ms, " +
                                                    "progress=${hasProgress}, buffered=${currentBufferedPosition}ms, retry=${bufferingRetryCount}")
                                            
                                            // Check if we have validated internet connection
                                            val hasInternet = networkMonitor.isConnectedToInternet()
                                            
                                            // DECISION TREE (Expert Android developer logic):
                                            // 1. If buffer is progressing (downloading data) → Give more time
                                            // 2. If no progress but haven't reached max time → Give one more chance
                                            // 3. If max time reached or no internet → Skip or wait
                                            
                                            when {
                                                // Case 1: Data is downloading (progressing) - be patient
                                                hasProgress && bufferingDuration < MAX_TOTAL_BUFFERING_MS -> {
                                                    AppLogger.i("RadioMediaService", "📊 Buffer progressing → Extending timeout")
                                                    lastBufferedPosition = currentBufferedPosition
                                                    bufferingRetryCount++
                                                    scheduleBufferingCheck() // Recursive: Schedule another check
                                                }
                                                
                                                // Case 2: No progress, but not maxed out retries - one more chance
                                                !hasProgress && bufferingRetryCount < MAX_BUFFERING_RETRIES && hasInternet -> {
                                                    AppLogger.w("RadioMediaService", "⏳ No progress but giving one more chance...")
                                                    lastBufferedPosition = currentBufferedPosition
                                                    bufferingRetryCount++
                                                    scheduleBufferingCheck() // Recursive: Schedule another check
                                                }
                                                
                                                // Case 3: Max retries or no internet → Skip or wait
                                                hasInternet -> {
                                                    // Internet OK but station not loading → skip to next
                                                    AppLogger.w("RadioMediaService", "❌ Max buffering time reached → Auto-skip")
                                                    currentAttemptedStation?.let { failedStation ->
                                                        showToast("⏭️ ${failedStation.name} no responde")
                                                        
                                                        // Use visual order for skip
                                                        val visualOrder = getVisualOrderStations()
                                                        val currentIndex = visualOrder.indexOfFirst { it.id == failedStation.id }
                                                        if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
                                                            val nextIndex = (currentIndex + 1) % visualOrder.size
                                                            val nextStation = visualOrder[nextIndex]
                                                            AppLogger.d("RadioMediaService", "→ Skipping to: ${nextStation.name}")
                                                            playStationInternal(nextStation)
                                                        }
                                                    }
                                                }
                                                
                                                else -> {
                                                    // No validated internet (tunnel, false 4G, etc.) → Wait indefinitely
                                                    AppLogger.i("RadioMediaService", "📶 No validated internet → Waiting for connection...")
                                                    showToast("📶 Sin conexión...")
                                                }
                                            }
                                        }
                                    }.also { mainHandler.postDelayed(it, BUFFERING_TIMEOUT_MS) }
                                }
                                
                                // Start the progressive timeout chain
                                scheduleBufferingCheck()
                            }
                            
                            // During transition or alarm, force playing state to prevent notification flicker
                            if (isTransitioning || isUsingAlarmAudio) {
                                updateNotification(forcePlayingState = true)
                            } else {
                                updateNotification()
                            }
                        }
                        Player.STATE_ENDED -> {
                            
                            val currentStation = currentAttemptedStation
                            
                            // CRITICAL FIX: StreamTheWorld AAC streams incorrectly trigger STATE_ENDED
                            // every 2-4 seconds due to how they transmit ADTS frames.
                            // For AAC streams, do a SEAMLESS instant restart without any visible indication.
                            val isAacStream = currentStation?.streamUrl?.let { url ->
                                url.endsWith(".aac", ignoreCase = true) || 
                                url.contains("AAC", ignoreCase = true) ||
                                url.contains("streamtheworld", ignoreCase = true)
                            } == true
                            
                            if (isAacStream && player.playWhenReady) {
                                // AAC DIAGNOSTIC LOGGING
                                val position = player.currentPosition
                                val duration = player.duration
                                val bufferedPosition = player.bufferedPosition
                                AppLogger.w("AAC_DEBUG", "⚠️ STATE_ENDED on AAC stream: ${currentStation.name}")
                                AppLogger.w("AAC_DEBUG", "   Position: ${position}ms, Duration: ${duration}ms, Buffered: ${bufferedPosition}ms")
                                AppLogger.w("AAC_DEBUG", "   URL: ${currentStation.streamUrl}")
                                
                                // SET FLAG TO SUPPRESS NOTIFICATION UPDATES
                                isAacSeamlessRestart = true
                                
                                // SEAMLESS RESTART: No retry counting, no notification change
                                mainHandler.post {
                                    try {
                                        player.seekTo(0)
                                        player.prepare()
                                        player.play()
                                    } catch (e: Exception) {
                                        isAacSeamlessRestart = false
                                        AppLogger.w("RadioMediaService", "AAC seamless restart failed: ${e.message}")
                                    }
                                }
                                return // Don't process further - seamless restart handled it
                            }
                            
                            // --- Normal STATE_ENDED handling for non-AAC streams ---
                            AppLogger.d("RadioMediaService", "STATE_ENDED received. Checking retry status.")
                            
                            // Check if we ran out of retries
                            if (retryCount >= 3) {
                                 AppLogger.e("RadioMediaService", "STATE_ENDED: Max retries reached ($retryCount). Stopping.")
                                 showToast("La emisora dejó de responder.")
                                 retryCount = 0
                                 isTransitioning = false
                                 updateNotification()
                                 return
                            }
                            
                            // Try to restart playback if we were supposed to be playing
                            if (currentStation != null && (player.playWhenReady || isUsingAlarmAudio)) {
                                 retryCount++
                                 AppLogger.d("RadioMediaService", "STATE_ENDED: Auto-restarting attempt $retryCount/3 for ${currentStation.name}")
                                 
                                 mainHandler.postDelayed({
                                    if (currentAttemptedStation?.id == currentStation.id) {
                                        restartStream()
                                    }
                                 }, 500)
                            } else {
                                isTransitioning = false
                                updateNotification()
                            }
                        }
                        Player.STATE_IDLE -> {
                            
                            AppLogger.d("RadioMediaService", "STATE_IDLE: Setting isTransitioning=false")
                            // Reset transition flag on idle
                            isTransitioning = false
                        }
                    }
                    updateAppWidgetWithColor()
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onPlaybackStateChanged", e)
                }
            }
            
            /**
             * CRITICAL: Log position discontinuities to capture audio "jumps" and rebuffering events.
             * This helps diagnose the reported audio glitch before crash.
             */
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                try {
                    val reasonString = when (reason) {
                        Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
                        Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
                        Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
                        Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
                        Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
                        Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
                        else -> "UNKNOWN($reason)"
                    }
                    val jumpMs = newPosition.positionMs - oldPosition.positionMs
                    AppLogger.i("RadioMediaService", "⏩ onPositionDiscontinuity: reason=$reasonString, jump=${jumpMs}ms, station=${currentAttemptedStation?.name}")
                    
                    // Log significant jumps (potential rebuffer or stream skip)
                    if (kotlin.math.abs(jumpMs) > 5000) {
                        AppLogger.w("RadioMediaService", "⚠️ Significant position jump detected: ${jumpMs}ms (>5s)")
                    }
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onPositionDiscontinuity", e)
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                try {
                    // DETAILED ERROR LOGGING for diagnosis
                    val station = currentAttemptedStation
                    AppLogger.e("RadioMediaService", buildString {
                        append("⚠️ PlayerError: ${station?.name ?: "unknown"}\n")
                        append("  ErrorCode: ${error.errorCodeName} (${error.errorCode})\n")
                        append("  Message: ${error.message}\n")
                        append("  Cause: ${error.cause?.javaClass?.simpleName}: ${error.cause?.message}")
                    })
                    
                    // GLOBAL CONNECTION CHECK: If NO INTERNET, pause immediately and wait.
                    // Do NOT skip to next station, do NOT retry.
                    // This fixes the issue of infinite skipping when data/wifi is off.
                    if (!networkMonitor.isNetworkAvailable()) {
                        AppLogger.w("RadioMediaService", "❌ GLOBAL NETWORK FAILURE: Device offline. Entering WAITING state.")
                        if (!isWaitingForNetwork) {
                             showToast("⚠️ Sin conexión. Esperando red...")
                             isWaitingForNetwork = true
                        }
                        // Stop player to clear error state and prevent internal retries
                        player.stop()
                        return
                    }
                    
                    // SMART ERROR CLASSIFICATION: Definitive vs Ambiguous
                    val cause = error.cause
                    val isDefinitiveError = when {
                        // HTTP errors (404, 500, etc.) - URL definitively broken
                        error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> true
                        
                        // DNS failed - domain does not exist (detected via UnknownHostException)
                        cause is java.net.UnknownHostException -> true
                        
                        // Invalid content type - not audio
                        error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> true
                        
                        // Malformed manifest/parsing error
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> true
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> true
                        
                        // Connection actively refused or failed to connect (Deep inspection)
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED && isConnectionRefused(error) -> true
                        
                        // SSL/Cleartext error
                        error.errorCode == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> true
                        
                        // Unrecognized format
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> true
                        
                        else -> false
                    }
                    
                    if (isDefinitiveError && station != null) {
                        // DEFINITIVE ERROR: Skip IMMEDIATELY (<1 second)
                        AppLogger.w("RadioMediaService", "⚡ DEFINITIVE ERROR - Instant skip: ${error.errorCodeName}")
                        
                        // Cancel any pending buffering timeout - not needed anymore
                        cancelBufferingTimeout()
                        hasReceivedDefinitiveError = true
                        
                        // Skip to next station IMMEDIATELY (no delay!)
                        showToast("❌ ${station.name} no disponible")
                        val visualOrder = getVisualOrderStations()
                        val currentIndex = visualOrder.indexOfFirst { it.id == station.id }
                        if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
                            val nextIndex = (currentIndex + 1) % visualOrder.size
                            val nextStation = visualOrder[nextIndex]
                            AppLogger.d("RadioMediaService", "⏭️ Instant skip to: ${nextStation.name}")
                            playStationInternal(nextStation, R.string.icon_next_station)
                        }
                        return
                    }
                    
                    // AMBIGUOUS ERROR: Let existing timeout/retry system handle it
                    AppLogger.d("RadioMediaService", "Error ambiguo, delegando a handlePlaybackError: ${error.errorCodeName}")
                    handlePlaybackError(error)
                } catch (e: Exception) {
                    AppLogger.e("PlayerListener", "Error in onPlayerError handler", e)
                }
            }
            
            // Metadata handling removed - Now Playing feature deprecated

        })
    }
    


    /**
     * Restart the current stream from scratch.
     * Useful for recovering from deep playback errors or long-session timeouts.
     */
    private fun restartStream() {
        currentAttemptedStation?.let { station ->
            AppLogger.w("RadioMediaService", "🔄 Restarting stream: ${station.name}")
            
            // Reset playback tracking for new session
            playbackStartTime = 0L
            
            // Full player reset to ensure clean state
            player.stop()
            player.clearMediaItems()
            
            // Rebuild and set the media item fresh
            playStationInternal(station)
        }
    }
    
    /**
     * Cancel the buffering timeout - called when we get a definitive error 
     * and don't need to wait for the timeout anymore.
     */
    private fun cancelBufferingTimeout() {
        bufferingTimeoutHandler?.let { mainHandler.removeCallbacks(it) }
        bufferingTimeoutHandler = null
        bufferingStartTime = 0
        lastBufferedPosition = 0
        bufferingRetryCount = 0
    }

    private fun handlePlaybackError(error: PlaybackException) {
        val failedStation = currentAttemptedStation
        val sessionDurationMs = if (playbackStartTime > 0) System.currentTimeMillis() - playbackStartTime else 0
        val sessionMinutes = sessionDurationMs / 60_000
        
        // DETAILED ERROR LOGGING
        val errorDetails = buildString {
            appendLine("⚠️ PLAYBACK ERROR")
            appendLine("  Station: ${failedStation?.name ?: "unknown"} (ID: ${failedStation?.id})")
            appendLine("  URL: ${failedStation?.streamUrl?.take(80)}...")
            appendLine("  Session Duration: ${sessionMinutes}min (${sessionDurationMs}ms)")
            appendLine("  Error Code: ${error.errorCode}")
            appendLine("  Message: ${error.message}")
        }
        AppLogger.e("RadioMediaService", errorDetails)
        
        // GENERIC STREAM RECOVERY
        // If the stream dies (Source Error or Network Error that isn't device offline), try to restart it.
        val isSourceError = error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                           error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                           error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        
        if (isSourceError && failedStation != null && retryCount < 1 && !isConnectionRefused(error)) {
            AppLogger.w("RadioMediaService", "⚡ Source error detected - attempting immediate restart")
            retryCount++
            showToast("🔄 Reconectando...")
            restartStream()
            return
        }
        
        // LONG SESSION RECOVERY (>10 minutes)
        // If we had successful playback for a long time, just restart.
        val isLongSession = sessionMinutes >= 10
        if (isLongSession && isSourceError && failedStation != null) {
            AppLogger.w("RadioMediaService", "🔄 Long-session recovery (${sessionMinutes}min)...")
            showToast("🔄 Reconectando...")
            
            // Reset for fresh start
            playbackStartTime = 0L
            retryCount = 0 // Allow retries for this new "session"
            
            mainHandler.postDelayed({
                restartStream()
            }, 500)
            return
        }
        
        // CRITICAL: If using Alarm, DO NOT STOP SERVICE on error
        // Re-try or stay alive to show the notification
        if (isUsingAlarmAudio) {
            // Log error but keep foreground service alive
            AppLogger.e("RadioMediaService", "Alarm playback error - keeping service alive")
            showToast("⚠️ Error de conexión en Alarma. Reintentando...")
            
            // Try to restart playback after 5 seconds
            mainHandler.postDelayed({
                if (isUsingAlarmAudio && currentAttemptedStation != null) {
                    player.prepare()
                    player.play()
                }
            }, 5000)
            
            // Ensure notification still shows "Alarm" even if errored
            updateNotification(forcePlayingState = true) 
        }
        
        // SIMPLIFIED ERROR HANDLING:
        // - No network? Wait for connection (don't skip)
        // - Network OK but station fails? Skip to next (station is broken, retrying won't help)
        
        if (failedStation == null) {
            AppLogger.e("RadioMediaService", "Cannot handle error: failedStation is null")
            return
        }
        
        // ===== SMART ERROR DETECTION =====
        val errorType = errorClassifier.classifyError(error)
        val errorMessage = errorClassifier.getErrorMessage(errorType, failedStation.name)
        AppLogger.i("RadioMediaService", "Error classified as: $errorType")
        
        // Show overlay notification
        serviceScope.launch {
            overlayManager.showOverlay(errorMessage, duration = 5000)
        }
        
        // Get retry strategy based on error type
        val retryStrategy = smartRetryManager.getRetryStrategy(errorType)
        
        when (retryStrategy) {
            is RetryStrategy.WaitForNetwork -> {
                // Network loss - pause and wait for recovery
                AppLogger.i("RadioMediaService", "📶 Network loss - waiting for recovery")
                showToast("📶 Sin conexión - Esperando cobertura...")
                player.pause()
                
                // Observe network changes and resume when connected
                serviceScope.launch {
                    networkMonitor.observeNetworkChanges()
                        .collect { networkState ->
                            if (networkState == com.radioandroid.util.NetworkState.CONNECTED ||
                                networkState == com.radioandroid.util.NetworkState.UNMETERED) {
                                AppLogger.i("RadioMediaService", "✅ Network recovered - resuming playback")
                                overlayManager.showOverlay("✅ Conexión recuperada", duration = 3000)
                                smartRetryManager.resetRetryCounter()
                                player.prepare()
                                player.play()
                            }
                        }
                }
                return
            }
            
            is RetryStrategy.SkipStation -> {
                // Station failure - skip to next
                AppLogger.w("RadioMediaService", "⚠️ Station failure - skipping to next")
                showToast("⚠️ ${failedStation.name} no disponible")
                smartRetryManager.resetRetryCounter()
                
                // Mark as unavailable and skip
                mainHandler.postDelayed({
                    // Use visual order for skip (same as UI)
                    val visualOrder = getVisualOrderStations()
                    val currentIndex = visualOrder.indexOfFirst { it.id == failedStation.id }
                    if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
                        val nextIndex = (currentIndex + 1) % visualOrder.size
                        val nextStation = visualOrder[nextIndex]
                        AppLogger.d("RadioMediaService", "Auto-skipping to: ${nextStation.name}")
                        playStationInternal(nextStation)
                    }
                }, 2000)
                return
            }
            
            is RetryStrategy.RetryWithBackoff -> {
                // Temporary glitch - retry with backoff
                smartRetryManager.recordAttempt(errorType)
                val delay = retryStrategy.delayMs
                
                AppLogger.i("RadioMediaService", "🔄 Retrying after ${delay}ms (attempt ${smartRetryManager.getRetryCount()})")
                showToast("🔄 Reconectando...")
                
                mainHandler.postDelayed({
                    restartStream()
                }, delay)
                return
            }
            
            is RetryStrategy.TreatAsStationFailure -> {
                // Max retries reached - treat as station failure
                AppLogger.w("RadioMediaService", "❌ Max retries reached - treating as station failure")
                showToast("❌ ${failedStation.name} no responde")
                smartRetryManager.resetRetryCounter()
                
                mainHandler.postDelayed({
                    // Use visual order for skip (same as UI)
                    val visualOrder = getVisualOrderStations()
                    val currentIndex = visualOrder.indexOfFirst { it.id == failedStation.id }
                    if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
                        val nextIndex = (currentIndex + 1) % visualOrder.size
                        val nextStation = visualOrder[nextIndex]
                        AppLogger.d("RadioMediaService", "Auto-skipping to: ${nextStation.name}")
                        playStationInternal(nextStation)
                    }
                }, 2000)
                return
            }
        }
        
        // === FALLBACK TO ORIGINAL LOGIC ===
        val hasNetwork = isNetworkAvailable()
        AppLogger.d("RadioMediaService", "Network available: $hasNetwork, failed station: ${failedStation.name}")
        
        if (!hasNetwork) {

            // NO NETWORK: Register callback to wait for connection, don't poll endlessly
            AppLogger.i("RadioMediaService", "📶 No network - registering callback to wait for connection")
            showToast("📶 Sin conexión. Esperando señal...")
            
            // Store station to retry when network comes back
            stationWaitingForNetwork = failedStation
            
            // Register network callback (if not already registered)
            registerNetworkCallback(failedStation)
            return
        }
        
        // NETWORK IS OK
        
        // Retry logic for unstable streams (e.g. Rock FM ads)
        if (retryCount < 3) {
            retryCount++
            AppLogger.w("RadioMediaService", "Playback Error: Retrying attempt $retryCount/3 for ${failedStation.name}")
            // Only show toast on first retry to avoid spam, or show specialized message
            if (retryCount == 1) showToast("Reconectando ${failedStation.name}...")
            
            mainHandler.postDelayed({
                if (currentAttemptedStation?.id == failedStation.id) {
                    playStationInternal(failedStation)
                }
            }, 1000)
            return
        }

        // Station is truly broken (max retries reached), skip to next immediately
        AppLogger.i("RadioMediaService", "⏭️ Network OK but ${failedStation.name} failed (max retries) - skipping to next")
        showToast("${failedStation.name} no disponible, pasando a la siguiente...")
        
        // Reset retry count for the new station
        retryCount = 0
        
        // Use visual order for skip (same as UI)
        val visualOrder = getVisualOrderStations()
        val currentIndex = visualOrder.indexOfFirst { it.id == failedStation.id }
        if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % visualOrder.size
            val nextStation = visualOrder[nextIndex]
            AppLogger.d("RadioMediaService", "Auto-skipping to: ${nextStation.name}")
            playStationInternal(nextStation)
        }
    }
    
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(this@RadioMediaService, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if device has active network connectivity.
     * Used to distinguish between network issues vs station issues.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Register a NetworkCallback to be notified when network becomes available.
     * This is more efficient than polling and only triggers when connectivity actually changes.
     */
    private fun registerNetworkCallback(stationToRetry: RadioStation) {
        // Unregister any existing callback first
        unregisterNetworkCallback()
        
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                AppLogger.d("RadioMediaService", "📶 Network became available!")
                
                // Verify it's actually usable (has internet capability)
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                
                if (hasInternet && isValidated) {
                    mainHandler.post {
                        val station = stationWaitingForNetwork
                        if (station != null) {
                            AppLogger.d("RadioMediaService", "📶 Network restored, retrying ${station.name}")
                            showToast("📶 Conexión restaurada, reanudando...")
                            stationWaitingForNetwork = null
                            unregisterNetworkCallback()
                            playStationInternal(station)
                        }
                    }
                }
            }
            
            override fun onLost(network: android.net.Network) {
                AppLogger.d("RadioMediaService", "📶 Network lost")
            }
        }
        
        try {
            val request = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            AppLogger.d("RadioMediaService", "NetworkCallback registered, waiting for connectivity...")
        } catch (e: Exception) {
            AppLogger.e("RadioMediaService", "Failed to register NetworkCallback: ${e.message}")
        }
    }
    
    /**
     * Unregister the network callback to stop listening for connectivity changes.
     */
    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            try {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(callback)
                AppLogger.d("RadioMediaService", "NetworkCallback unregistered")
            } catch (e: Exception) {
                // Ignore - callback may not have been registered
            }
        }
        networkCallback = null
        stationWaitingForNetwork = null
    }

    private fun initializeMediaSession() {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create a ForwardingPlayer to intercept skip commands and implement Time-Shift logic
        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun seekToNext() {
                skipToNext()
            }
            
            override fun seekToPrevious() {
                skipToPrevious()
            }
            
            override fun seekToNextMediaItem() {
                skipToNext()
            }
            
            override fun seekToPreviousMediaItem() {
                skipToPrevious()
            }
            
            override fun hasNextMediaItem(): Boolean = true
            
            override fun hasPreviousMediaItem(): Boolean = true

            override fun pause() {
                pauseTimestamp = System.currentTimeMillis()
                stopDelayTicker()
                super.setPlayWhenReady(false)
                // ExoPlayer handles audio focus natively - no manual abandon needed
            }

            override fun play() {
                executePlayLogic()
            }
            
            override fun setPlayWhenReady(playWhenReady: Boolean) {
                AppLogger.d("RadioMediaService", "setPlayWhenReady: $playWhenReady")
                if (playWhenReady) {
                    executePlayLogic()
                } else {
                    pause()
                }
            }
            
            private fun executePlayLogic() {
                val msSincePause = if (pauseTimestamp > 0) System.currentTimeMillis() - pauseTimestamp else 0
                pauseTimestamp = 0 // Reset immediately
                
                val isPremium = com.radioandroid.premium.PremiumManager.isPremium.value
                val isTimeShiftEnabled = com.radioandroid.data.AppPreferences.isTimeShiftEnabled
                
                // If Time-Shift is OFF (Free or Disabled): ALWAYS go to live
                if (!isPremium || !isTimeShiftEnabled) {
                    // Clear any accumulated delay and go to live edge
                    if (com.radioandroid.player.PlayerState.accumulatedDelay.value > 0 || msSincePause > 0) {
                        goToLiveInternal()
                        return
                    }
                    // No pause happened, just play
                    super.setPlayWhenReady(true)
                    return
                }
                
                // Premium + Time-Shift Enabled: Accumulate delay
                if (msSincePause > 0) {
                    val currentDelay = com.radioandroid.player.PlayerState.accumulatedDelay.value
                    val newDelay = currentDelay + msSincePause
                    
                    if (newDelay > 120_000) {
                        // Exceeded 2 min limit - go to live
                        goToLiveInternal()
                        return
                    }
                    
                    // Accumulate the delay
                    com.radioandroid.player.PlayerState.setDelay(newDelay)
                    
                    super.setPlayWhenReady(true)
                    startDelayTicker()
                    return
                }
                
                // No pause, just resume
                if (com.radioandroid.player.PlayerState.accumulatedDelay.value > 0) {
                    startDelayTicker()
                }
                
                // ExoPlayer handles focus natively - just play
                super.setPlayWhenReady(true)
            }

            override fun seekToDefaultPosition() {
                com.radioandroid.player.PlayerState.setDelay(0)
                stopDelayTicker()
                super.seekToDefaultPosition()
            }
            
            // Helper to go to live AND start playing
            private fun goToLiveInternal() {
                com.radioandroid.player.PlayerState.setDelay(0)
                stopDelayTicker()
                super.seekToDefaultPosition()
                super.setPlayWhenReady(true)
            }
            
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }
        }

        // Store reference for use in onStartCommand and playStationInternal
        playbackController = forwardingPlayer

        mediaLibrarySession = MediaLibrarySession.Builder(this, forwardingPlayer, LibrarySessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player != null && !player.playWhenReady) {
            // Only stop if NOT in alarm mode
            if (!isUsingAlarmAudio) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        // Update widget to show stopped state BEFORE cleanup
        WidgetStateManager.updateState(
            applicationContext,
            station = null,  // Clear station when stopped
            isPlaying = false,
            isLoading = false,
            color = 0xFF1C1B1F.toInt()
        )
        
        // Cancel coroutines first to prevent post-mortem execution
        fadeJob?.cancel()
        serviceScope.cancel()
        
        // Clean up network callback
        unregisterNetworkCallback()
        
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        
        artworkCache.clear()
        StreamingPlayerFactory.releaseCache()
        stopDelayTicker()
        
        // Cancel watchdog
        cancelBufferingWatchdog()
        
        super.onDestroy()
    }
    
    // Watchdog for infinite buffering
    private var bufferingWatchdogRunnable: Runnable? = null

    private fun startBufferingWatchdog() {
        // Cancel existing
        cancelBufferingWatchdog()
        
        AppLogger.d("RadioMediaService", "⏳ Watchdog: Started buffering timer (3s)")
        
        val runnable = Runnable {
            AppLogger.w("RadioMediaService", "⚠️ Watchdog: Buffering timeout allowed (3s exceeded)")
            
            // TIMEOUT LOGIC with SMART VALIDATION:
            serviceScope.launch {
                val hasRealInternet = SmartNetworkValidator.hasRealInternetConnection()
                
                if (hasRealInternet) {
                    // We have REAL internet but stream is stuck -> Station is dead -> SKIP
                    AppLogger.e("RadioMediaService", "⚠️ Watchdog: Internet Verified & Stream Stuck -> Auto-skipping")
                    withContext(Dispatchers.Main) {
                        showToast("⚠️ La emisora no responde - Saltando...")
                        
                        // Force skip logic strictly
                        val failedStation = currentAttemptedStation
                        if (failedStation != null) {
                             val visualOrder = getVisualOrderStations()
                             val currentIndex = visualOrder.indexOfFirst { it.id == failedStation.id }
                             if (currentIndex >= 0 && visualOrder.isNotEmpty()) {
                                 val nextIndex = (currentIndex + 1) % visualOrder.size
                                 val nextStation = visualOrder[nextIndex]
                                 AppLogger.d("RadioMediaService", "Watchdog Auto-skipping to: ${nextStation.name}")
                                 playStationInternal(nextStation)
                             }
                        }
                    }
                } else {
                    // No internet -> Just pause (don't skip, maybe user enters elevator)
                    AppLogger.w("RadioMediaService", "⚠️ Watchdog: Timeout but NO REAL INTERNET -> Pausing")
                    withContext(Dispatchers.Main) {
                        showToast("📶 Conexión inestable - Pausado")
                        player.pause() // Pause instead of stop to allow auto-resume easier
                        
                        // CRITICAL: Start monitoring to resume when user leaves elevator/tunnel
                        startNetworkRecoveryMonitor()
                    }
                }
            }
        }
        
        bufferingWatchdogRunnable = runnable
        mainHandler.postDelayed(runnable, 3_000) // 3 seconds timeout
    }
    
    private fun cancelBufferingWatchdog() {
        bufferingWatchdogRunnable?.let { 
            mainHandler.removeCallbacks(it) 
            bufferingWatchdogRunnable = null
            AppLogger.d("RadioMediaService", "Watchdog: Cancelled")
        }
    }
    
    /**
     * Handle metadata updates from stream (Now Playing info)
     * Premium Feature: Only processes and displays for premium users
     */

    
    private fun updateNotification(forcePlayingState: Boolean? = null) {
        try {
            val actualIsPlaying = player.isPlaying
            val finalIsPlaying = forcePlayingState ?: actualIsPlaying
            AppLogger.d("RadioMediaService", "updateNotification: forcePlayingState=$forcePlayingState, player.isPlaying=$actualIsPlaying, finalIsPlaying=$finalIsPlaying, isTransitioning=$isTransitioning, isUsingAlarmAudio=$isUsingAlarmAudio")
            
            val currentItem = player.currentMediaItem
            val metadata = currentItem?.mediaMetadata
            
            // Use forced state if provided, otherwise check player
            val isPlaying = finalIsPlaying
            
            // Determine visual state
            val playbackState = player.playbackState
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val showPauseButton = isPlaying || isBuffering
            
            // Fallback title if metadata is null (e.g. during alarm startup)
            val contentTitle = metadata?.title ?: currentAttemptedStation?.name ?: "RadioFlow+"
            
            // Notification content text
            val contentText = if (isUsingAlarmAudio) {
                "⏰ Alarma activa"
            } else {
                when {
                    isBuffering -> "Cargando..."
                    isPlaying -> "En directo"
                    else -> metadata?.subtitle ?: "Streaming"
                }
            }
            
            android.util.Log.d("RadioMediaService", "Notification: title=$contentTitle, text=$contentText, isBuffering=$isBuffering")
            
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // ALWAYS use regular channel for service notification
            val targetChannelId = CHANNEL_ID
            
            // Build basic service notification (keeps service alive)
            val serviceNotificationBuilder = NotificationCompat.Builder(this, targetChannelId)
                .setContentTitle(contentTitle)
                .setSmallIcon(R.drawable.ic_radio)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(showPauseButton || isTransitioning)  // Keep ongoing during transitions or buffering
                .setContentText(contentText)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)  // Changed from LOW to DEFAULT for better visibility
            
            AppLogger.d("RadioMediaService", "Notification built: isPlaying=$isPlaying, isBuffering=$isBuffering, isTransitioning=$isTransitioning")
            
            // Add playback controls to service notification
            if (!isUsingAlarmAudio) {
                serviceNotificationBuilder
                    // Previous action
                    .addAction(
                        R.drawable.ic_previous,
                        "Anterior",
                        createActionPendingIntent(ACTION_SKIP_PREVIOUS)
                    )
                    // Play/Pause action (Show Pause if playing OR buffering)
                    .addAction(
                        if (showPauseButton) R.drawable.ic_pause else R.drawable.ic_play,
                        if (showPauseButton) "Pausar" else "Reproducir",
                        createPlayPauseAction()
                    )
                    // Next action
                    .addAction(
                        R.drawable.ic_next,
                        "Siguiente",
                        createActionPendingIntent(ACTION_SKIP_NEXT)
                    )
                
                // CRITICAL: Add MediaStyle to properly integrate with MediaSession
                // This is what prevents Android from reclassifying the notification
                mediaLibrarySession?.let { session ->
                    val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(session.sessionCompatToken)
                        .setShowActionsInCompactView(0, 1, 2)  // Show all 3 controls in compact view
                        .setShowCancelButton(false)
                    
                    serviceNotificationBuilder.setStyle(mediaStyle)
                    AppLogger.d("RadioMediaService", "MediaStyle added to notification with session token")
                }
            }

            // CRITICAL: Always call startForeground with service notification (ID 1001)
            val serviceNotification = serviceNotificationBuilder.build()
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, serviceNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, serviceNotification)
            }
            android.util.Log.d("RadioMediaService", "startForeground completed with service notification ID=$NOTIFICATION_ID")
            
            // If alarm is active, ALSO show separate standalone alarm notification (ID 1002)
            if (isUsingAlarmAudio) {
                try {
                    android.util.Log.d("RadioMediaService", "Creating STANDALONE alarm notification")
                    
                    // Create explicit intent for AlarmActivity (for locked screens)
                    val alarmActivityIntent = Intent(this, com.radioandroid.ui.AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(EXTRA_STATION_ID, alarmStationId)
                    }
                    
                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        this, 
                        1002, 
                        alarmActivityIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    
                    // Create intent to open MainActivity and convert alarm to normal playback (for unlocked screens)
                    val openAppIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        action = "OPEN_FROM_ALARM"  // Special action to indicate coming from alarm
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        this,
                        1004,
                        openAppIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    
                    // Create stop action using BroadcastReceiver
                    val stopAlarmIntent = Intent(com.radioandroid.timer.StopAlarmReceiver.ACTION_STOP_ALARM).apply {
                        setPackage(packageName)
                    }
                    val stopAlarmPendingIntent = PendingIntent.getBroadcast(
                        this,
                        1003,
                        stopAlarmIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    
                    // Create snooze action
                    val snoozeIntent = Intent(this, RadioMediaService::class.java).apply {
                        action = ACTION_SNOOZE_ALARM
                        putExtra(EXTRA_STATION_ID, alarmStationId)
                    }
                    val snoozePendingIntent = PendingIntent.getService(
                        this,
                        1005,
                        snoozeIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    // Build standalone alarm notification (NOT tied to startForeground)
                    val alarmNotificationBuilder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_radio)
                        .setContentTitle("⏰ Alarma")
                        .setContentText("Alarma de $contentTitle")
                        .setOngoing(true)  // Cannot be dismissed by user
                        .setAutoCancel(false)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setFullScreenIntent(fullScreenPendingIntent, true)  // For locked screens
                        .setContentIntent(openAppPendingIntent)  // For unlocked screens - open app
                        // Sound and vibration for heads-up
                        .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                        .setVibrate(longArrayOf(0, 500, 200, 500))
                        // BigTextStyle for visibility
                        .setStyle(NotificationCompat.BigTextStyle()
                            .bigText("Tu alarma está sonando. Posponer, detener o abrir app."))
                        // Action 1: Snooze
                        .addAction(
                            android.R.drawable.ic_lock_idle_alarm,
                            "🔔 POSPONER",
                            snoozePendingIntent
                        )
                        // Action 2: Stop
                        .addAction(
                            android.R.drawable.ic_delete,
                            "⏹ DETENER",
                            stopAlarmPendingIntent
                        )
                        // Action 3: Open app
                        .addAction(
                            R.drawable.ic_radio,
                            "ABRIR",
                            openAppPendingIntent
                        )
                    
                    // Show standalone alarm notification using notify() NOT startForeground()
                    val alarmNotification = alarmNotificationBuilder.build()
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(ALARM_NOTIFICATION_ID, alarmNotification)
                    android.util.Log.d("RadioMediaService", "Standalone alarm notification displayed with ID=$ALARM_NOTIFICATION_ID")
                    
                } catch (e: Exception) {
                    android.util.Log.e("RadioMediaService", "ERROR creating standalone alarm notification: ${e.message}", e)
                }
            }
            
        } catch (e: Exception) {
            AppLogger.e("RadioMediaService", "ERROR in updateNotification: ${e.message}", e)
            // Try to show a basic notification as fallback
            try {
                val basicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("RadioFlow+")
                    .setContentText("Reproduciendo")
                    .setSmallIcon(R.drawable.ic_radio)
                    .build()
                
                startForeground(NOTIFICATION_ID, basicNotification)
            } catch (e2: Exception) {
                AppLogger.e("RadioMediaService", "CRITICAL: Could not show any notification: ${e2.message}", e2)
            }
        }
    }

    private fun createPlayPauseAction(): PendingIntent {
        val intent = Intent(this, RadioMediaService::class.java).apply {
            action = "TOGGLE_PLAY_PAUSE"
        }
        return PendingIntent.getService(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, RadioMediaService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    /**
     * Explicitly launch AlarmActivity - main method to show alarm UI
     * Works on both locked and unlocked screens
     */
    private fun launchAlarmActivityDirectly(stationId: Int) {
        // Optimized delay - Android foreground service grace period is usually sufficient
        mainHandler.postDelayed({
            var wakeLock: android.os.PowerManager.WakeLock? = null
            try {
                AppLogger.d("RadioMediaService", "Launching AlarmActivity for station $stationId")
                
                // Wake up the screen first
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                @Suppress("DEPRECATION")
                wakeLock = powerManager.newWakeLock(
                    android.os.PowerManager.FULL_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
                    "RadioAndroid::AlarmWakeLock"
                )
                wakeLock.acquire(10 * 1000L) // 10 seconds max
                AppLogger.d("RadioMediaService", "WakeLock acquired to wake screen")
                
                // Check if screen is locked - only show full AlarmActivity on lock screen
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                val isScreenLocked = keyguardManager.isKeyguardLocked
                
                if (isScreenLocked) {
                    // Screen is locked - launch full-screen AlarmActivity
                    AppLogger.d("RadioMediaService", "Screen is LOCKED - launching full-screen AlarmActivity")
                    
                    val alarmActivityIntent = Intent(this, com.radioandroid.ui.AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                                Intent.FLAG_RECEIVER_FOREGROUND
                        putExtra(EXTRA_STATION_ID, stationId)
                    }
                    
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        1005,
                        alarmActivityIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    
                    try {
                        pendingIntent.send()
                        AppLogger.d("RadioMediaService", "AlarmActivity pendingIntent sent")
                    } catch (e: Exception) {
                        AppLogger.e("RadioMediaService", "PendingIntent send failed, trying direct startActivity: ${e.message}")
                        startActivity(alarmActivityIntent)
                    }
                } else {
                    // Screen is unlocked - just rely on heads-up notification (less intrusive)
                    AppLogger.d("RadioMediaService", "Screen is UNLOCKED - using heads-up notification only (less intrusive)")
                }
                
            } catch (e: Exception) {
                AppLogger.e("RadioMediaService", "Failed to launch AlarmActivity: ${e.message}")
            } finally {
                // Ensure WakeLock is released even if exception occurs
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                        AppLogger.d("RadioMediaService", "Launch WakeLock released")
                    }
                } catch (e: Exception) {
                    AppLogger.e("RadioMediaService", "WakeLock release failed: ${e.message}")
                }
            }
        }, 300) // Reduced from 500ms - foreground service grace period is sufficient
    }
    
    /**
     * Get stations in the same visual order as the UI displays them.
     * Order: Favorites (in UI order) → Local country (sorted by name) → International (grouped by country, preserving list order)
     * This ensures skip buttons navigate in the order the user sees on screen.
     */
    private fun getVisualOrderStations(): List<RadioStation> {
        val locale = java.util.Locale.getDefault()
        val homeCountry = when (locale.country.uppercase()) {
            "MX" -> "México"
            "GB", "UK" -> "United Kingdom"
            else -> "España"
        }
        
        val favoriteIds = com.radioandroid.data.FavoritesRepository.favoriteIds.value
        
        // Favorites first (preserving the order they appear in the stations list)
        val favoriteStations = RadioStations.stations.filter { it.id in favoriteIds }
        
        // Local country stations (non-favorites, preserving popularity order from RadioStations.kt)
        val localStations = RadioStations.stations.filter { 
            it.country == homeCountry && it.id !in favoriteIds 
        }
        // Removed .sortedBy { it.name } to preserve popularity order from RadioStations.kt
        
        // International stations (non-favorites, grouped by country with countries sorted alphabetically)
        // Within each country, preserve original list order (no name sorting) - matching UI
        val internationalByCountry = RadioStations.stations.filter { 
            it.country != homeCountry && it.id !in favoriteIds 
        }.groupBy { it.country }.toSortedMap()
        
        val internationalStations = internationalByCountry.values.flatten()
        
        return favoriteStations + localStations + internationalStations
    }
    
    /**
     * Get stations for navigation (next/previous buttons).
     * If single-country navigation is enabled, filters to only stations from the current country.
     * Otherwise, returns all stations in visual order.
     */
    private fun getStationsForNavigation(): List<RadioStation> {
        val visualOrder = getVisualOrderStations()
        
        // Check preference 
        val isSingleCountryEnabled = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
        AppLogger.d("RadioMediaService", "getStationsForNavigation: isSingleCountryEnabled=$isSingleCountryEnabled")
        
        if (!isSingleCountryEnabled) {
            AppLogger.d("RadioMediaService", "Single-country navigation DISABLED, returning all ${visualOrder.size} stations")
            return visualOrder
        }
        
        // Get the country of the current station
        val currentMediaId = player.currentMediaItem?.mediaId
        AppLogger.d("RadioMediaService", "Current mediaId: $currentMediaId")
        
        val currentStation = currentMediaId?.let { id ->
            RadioStations.stations.find { it.id.toString() == id }
        }
        
        if (currentStation == null) {
            AppLogger.d("RadioMediaService", "Current station not found, returning all stations")
            return visualOrder
        }
        
        val currentCountry = currentStation.country
        AppLogger.d("RadioMediaService", "Current station: ${currentStation.name}, country: $currentCountry")
        
        // Filter to only stations from the same country, maintaining visual order
        val filteredStations = visualOrder.filter { it.country == currentCountry }
        
        AppLogger.d("RadioMediaService", "Single-country navigation ENABLED: filtering to '$currentCountry' -> ${filteredStations.size} stations")
        filteredStations.take(5).forEach { 
            AppLogger.d("RadioMediaService", "  - ${it.name} (${it.country})")
        }
        
        return if (filteredStations.isNotEmpty()) filteredStations else visualOrder
    }
    
    private fun skipToNext() {
        isSkipping = true
        skipAttempts = 0
        
        val current = currentAttemptedStation ?: run {
             // Fallback if no station playing
             val first = NavigationUtils.getAllOrderedStations().firstOrNull()
             if (first != null) playStationInternal(first)
             return
        }
        
        val next = NavigationUtils.getNextStation(current)
        
        // Notify user about filter if applicable
        val useCountryFilter = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
        val useGenreFilter = com.radioandroid.data.AppPreferences.isGenreNavigationEnabled
        if (useCountryFilter || useGenreFilter) {
             val symbol = if (useCountryFilter) "🌍" else "🎵"
             android.widget.Toast.makeText(this, "⏭️ $symbol ${next.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        AppLogger.d("RadioMediaService", "skipToNext: ${current.name} -> ${next.name}")
        playStationInternal(next, R.string.icon_next_station)
    }
    
    private fun skipToPrevious() {
        isSkipping = true
        skipAttempts = 0
        
        val current = currentAttemptedStation ?: run {
             // Fallback if no station playing
             val last = NavigationUtils.getAllOrderedStations().lastOrNull()
             if (last != null) playStationInternal(last)
             return
        }
        
        val prev = NavigationUtils.getPreviousStation(current)
        
        // Notify user about filter if applicable
        val useCountryFilter = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
        val useGenreFilter = com.radioandroid.data.AppPreferences.isGenreNavigationEnabled
        if (useCountryFilter || useGenreFilter) {
             val symbol = if (useCountryFilter) "🌍" else "🎵"
             android.widget.Toast.makeText(this, "⏮️ $symbol ${prev.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        AppLogger.d("RadioMediaService", "skipToPrevious: ${current.name} -> ${prev.name}")
        playStationInternal(prev, R.string.icon_previous_station)
    }
    
    private fun playStation(station: RadioStation) {
        // Manual selection - not skipping
        isSkipping = false
        
        // Update preferences immediately on manual play
        com.radioandroid.data.AppPreferences.lastStationId = station.id
        com.radioandroid.data.AppPreferences.lastPlaybackTime = System.currentTimeMillis()
        com.radioandroid.data.AppPreferences.wasPlaying = true
        
        // Restore media audio if we were using alarm audio
        if (isUsingAlarmAudio) {
            restoreMediaAudioAttributes()
        }
        // Ensure volume is reset to 100% for manual playback
        cancelAlarmFadeIn()
        player.volume = 1.0f
        
        playStationInternal(station)
    }
    
    private fun playStationInternal(station: RadioStation, actionIconResId: Int? = null) {
        // DETAILED PLAYBACK LOGGING
        AppLogger.playback("▶️ Starting: ${station.name} (ID: ${station.id})")
        AppLogger.d("RadioMediaService", "Stream URL: ${station.streamUrl}")
        
        // Mark as transitioning to maintain notification state
        AppLogger.d("RadioMediaService", "playStationInternal: Setting isTransitioning=true")
        isTransitioning = true
        
        // Reset retries on manual station change
        retryCount = 0
        
        // Save the station being attempted for error handling
        currentAttemptedStation = station
        
        // Detect AAC live streams which need special configuration
        val isAacStream = station.streamUrl.endsWith(".aac", ignoreCase = true) || 
                          station.streamUrl.contains("AAC", ignoreCase = true) ||
                          station.streamUrl.contains("ADTS", ignoreCase = true)
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.id.toString())
            .setUri(station.streamUrl)
            .apply {
                // CRITICAL: AAC live streams need LiveConfiguration to prevent STATE_ENDED
                // Without this, ExoPlayer treats them as finite files and stops every few seconds
                if (isAacStream) {
                    AppLogger.d("RadioMediaService", "Configuring AAC live stream with LiveConfiguration")
                    setMimeType(androidx.media3.common.MimeTypes.AUDIO_AAC)
                    setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setTargetOffsetMs(androidx.media3.common.C.TIME_UNSET)  // Use server-defined offset
                            .setMinPlaybackSpeed(1.0f)  // No speed adjustment for radio
                            .setMaxPlaybackSpeed(1.0f)
                            .build()
                    )
                }
            }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setSubtitle(station.genre)
                    .setArtist(station.genre)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setArtworkData(
                        getCachedStationArtwork(station),
                        MediaMetadata.PICTURE_TYPE_FRONT_COVER
                    )
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        
        // Reset Time-Shift delay BEFORE play so new station starts fresh
        com.radioandroid.player.PlayerState.setDelay(0)
        pauseTimestamp = 0
        stopDelayTicker()
        
        playbackController.play()
        
        // Show station change overlay if enabled (with context-aware icon)
        if (com.radioandroid.data.AppPreferences.isStationChangeOverlayEnabled) {
            val icon = actionIconResId ?: R.string.icon_resume_play
            overlayManager.showStationChangeOverlay(station.name, icon)
        }
        
        // Force update with playing state to prevent notification flicker
        AppLogger.d("RadioMediaService", "playStationInternal: Calling updateNotification with forcePlayingState=true")
        updateNotification(forcePlayingState = true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE_PLAY_PAUSE", "ACTION_TOGGLE_PLAYBACK" -> {
                if (playbackController.isPlaying) {
                    playbackController.pause()
                    // Restore media audio if we were using alarm audio
                    if (isUsingAlarmAudio) {
                        restoreMediaAudioAttributes()
                    }
                } else {
                    // Smart Play: If no media item loaded (app killed), play last station or first
                    if (playbackController.currentMediaItem == null) {
                         val lastId = com.radioandroid.data.AppPreferences.lastStationId
                         val station = RadioStations.stations.find {it.id == lastId} ?: RadioStations.stations.firstOrNull()
                         if (station != null) {
                             playStation(station)
                         } else {
                             // Fallback just in case list is empty
                             playbackController.play()
                         }
                    } else {
                        playbackController.play()
                    }
                }
                updateAppWidgetWithColor()
            }
            ACTION_SKIP_NEXT, "ACTION_NEXT_STATION" -> {
                skipToNext()
                updateAppWidgetWithColor()
            }
            ACTION_SKIP_PREVIOUS -> skipToPrevious()

            ACTION_ALARM_PLAY -> {
                // 1. FAST PATH: Start Foreground IMMEDIATELY to satisfy Android 14+ background start restrictions
                // We don't wait for station lookup or logging. We just claim the foreground slot.
                try {
                    isUsingAlarmAudio = true
                    val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
                    
                    // Cleanup previous (just in case) - using modern API
                    try { 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    } catch (e: Exception) {}

                    // Create PendingIntent for FullScreenIntent (CRITICAL for lock screen)
                    val alarmActivityIntent = Intent(this, com.radioandroid.ui.AlarmActivity::class.java).apply {
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        putExtra(EXTRA_STATION_ID, stationId)
                    }
                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        this, 
                        1005, 
                        alarmActivityIntent, 
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    
                    // Build preliminary High Priority Notification
                    val notificationBuilder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_radio)
                        .setContentTitle("⏰ Alarma")
                        .setContentText("Iniciando alarma...")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setOngoing(true)
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                    } else {
                        startForeground(NOTIFICATION_ID, notificationBuilder.build())
                    }
                    
                    AppLogger.d("RadioMediaService", "ALARM FOREGROUND STARTED (Fast Path)")
                    
                    // 2. NOW continue with logic
                    val station = RadioStations.stations.find { it.id == stationId }
                        ?: RadioStations.stations.firstOrNull()
                    
                    if (station != null) {
                        AppLogger.d("RadioMediaService", "Found station: ${station.name}")
                        alarmStationId = station.id
                        currentAttemptedStation = station
                        
                        // Launch UI explicitly (as backup for unlocked screen)
                        launchAlarmActivityDirectly(station.id)
                        
                        // Setup Audio
                        val alarmAudioAttributes = AudioAttributes.Builder()
                            .setUsage(C.USAGE_ALARM)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build()
                        player.setAudioAttributes(alarmAudioAttributes, false)
                        player.volume = 0.05f
                        
                        // Setup Media Item
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(station.id.toString())
                            .setUri(station.streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(station.name)
                                    .setSubtitle("⏰ Alarma")
                                    .setArtist(station.genre)
                                    .setIsPlayable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                                    .build()
                            )
                            .build()
                        
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true
                        startAlarmFadeIn()
                        ensureAlarmStreamVolume()
                        
                        // Schedule Stop
                        scheduleAlarmAutoStop()
                        
                        // Update Notification with Real Info (using same ID, so it just updates text)
                        updateNotification(forcePlayingState = true)
                        
                    } else {
                        AppLogger.e("RadioMediaService", "No station found!")
                    }
                } catch (e: Exception) {
                    AppLogger.e("RadioMediaService", "CRASH in ACTION_ALARM_PLAY: ${e.message}")
                    e.printStackTrace()
                }
            }
            ACTION_RESTORE_MEDIA_AUDIO -> {
                restoreMediaAudioAttributes()
                cancelAlarmFadeIn()
                player.volume = 1.0f
                
                // Force cancel alarm notification (ID 1002) just in case
                try {
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(ALARM_NOTIFICATION_ID)
                    AppLogger.d("RadioMediaService", "Force cancelled alarm notification in handler")
                } catch (e: Exception) {
                    AppLogger.e("RadioMediaService", "Failed to cancel alarm notification: ${e.message}")
                }
            }
            ACTION_STOP_ALARM -> {
                // Stop alarm playback from lock screen button
                player.stop()
                player.clearMediaItems()  // CRITICAL: Clear media items to fully stop, not just pause
                currentAttemptedStation = null  // Clear current station
                cancelAlarmAutoStop()
                restoreMediaAudioAttributes()
                
                // CRITICAL: Cancel alarm notification (ID 1002)
                try {
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(ALARM_NOTIFICATION_ID)
                    AppLogger.d("RadioMediaService", "Cancelled alarm notification ID=$ALARM_NOTIFICATION_ID")
                } catch (e: Exception) {
                    AppLogger.e("RadioMediaService", "Failed to cancel alarm notification: ${e.message}")
                }
                
                AppLogger.d("RadioMediaService", "Alarm stopped by user")
                showToast("⏰ Alarma detenida")
                
                // Cerrar AlarmActivity si está abierta enviando broadcast
                val closeIntent = Intent("com.radioandroid.CLOSE_ALARM_ACTIVITY").apply {
                    setPackage(packageName)
                }
                sendBroadcast(closeIntent)
                
                // FULL STOP: Stop foreground service and remove notification (like X button)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                    AppLogger.d("RadioMediaService", "Service fully stopped and destroyed")
                } catch (e: Exception) {
                    AppLogger.e("RadioMediaService", "Error stopping service: ${e.message}")
                }
            }
            ACTION_SNOOZE_ALARM -> {
                val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
                if (stationId != -1) {
                    AppLogger.d("RadioMediaService", "Snoozing alarm - will trigger again in 5 minutes")
                    
                    // CRITICAL: Stop current playback first
                    player.stop()
                    player.clearMediaItems()
                    currentAttemptedStation = null
                    cancelAlarmAutoStop()
                    restoreMediaAudioAttributes()
                    
                    // Cancel alarm notification
                    try {
                        val notificationManager = getSystemService(NotificationManager::class.java)
                        notificationManager.cancel(ALARM_NOTIFICATION_ID)
                        AppLogger.d("RadioMediaService", "Cancelled alarm notification on snooze")
                    } catch (e: Exception) {
                        AppLogger.e("RadioMediaService", "Failed to cancel alarm notification: ${e.message}")
                    }
                    
                    // Schedule alarm for 5 minutes from now
                    val calendar = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.MINUTE, 5)
                    }
                    RadioAlarmManager.scheduleAlarm(
                        context = applicationContext,
                        hour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        minute = calendar.get(java.util.Calendar.MINUTE),
                        stationId = stationId
                    )
                    
                    showToast("⏰ Alarma pospuesta 5 minutos")
                    
                    // Cerrar AlarmActivity
                    val closeIntent = Intent("com.radioandroid.CLOSE_ALARM_ACTIVITY").apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(closeIntent)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun restoreMediaAudioAttributes() {
        if (isUsingAlarmAudio) {
            isUsingAlarmAudio = false
            cancelAlarmAutoStop() // Cancel auto-stop when restoring media audio
            
            // CRITICAL: Cancel alarm notification (ID 1002) when restoring media audio
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(ALARM_NOTIFICATION_ID)
                AppLogger.d("RadioMediaService", "Cancelled alarm notification when restoring media audio")
            } catch (e: Exception) {
                AppLogger.e("RadioMediaService", "Failed to cancel alarm notification: ${e.message}")
            }
            
            val mediaAudioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(mediaAudioAttributes, true)
            // Ensure volume is back to 100%
            player.volume = 1.0f
            cancelAlarmFadeIn()
            
            // Update to normal notification (ID 1001)
            updateNotification()
        }
    }
    
    private fun scheduleAlarmAutoStop() {
        cancelAlarmAutoStop() // Cancel any existing auto-stop
        
        alarmAutoStopRunnable = Runnable {
            // Stop playback
            player.stop()
            restoreMediaAudioAttributes()
            
            // Show auto-stop notification
            showAutoStopNotification()
            
            showToast("⏰ Radio detenida automáticamente tras 15 minutos")
        }
        
        mainHandler.postDelayed(alarmAutoStopRunnable!!, ALARM_AUTO_STOP_MS)
    }
    
    private fun cancelAlarmAutoStop() {
        alarmAutoStopRunnable?.let { 
            mainHandler.removeCallbacks(it)
            alarmAutoStopRunnable = null
        }
    }
    
    private fun showAutoStopNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radio detenida automáticamente")
            .setContentText("La alarma ha sonado durante 15 minutos. Toca para reanudar.")
            .setSmallIcon(R.drawable.ic_radio)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun startDelayTicker() {
        stopDelayTicker()
        delayTickerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                // Keep the delay constant while playing (user is constantly X seconds behind live)
                // But we must check if we are still playing.
                if (!player.isPlaying && pauseTimestamp == 0L) {
                    stopDelayTicker()
                    break
                }
            }
        }
    }
    
    private fun stopDelayTicker() {
        delayTickerJob?.cancel()
        delayTickerJob = null
    }

    /**
     * Callback for MediaLibrarySession - handles content browsing for Android Auto
     */
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Log connection attempts - critical for debugging Android Auto issues
            AppLogger.d("RadioMediaService", "onGetLibraryRoot called from package: ${browser.packageName}, uid: ${browser.uid}")
            
            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("RadioFlow+")
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
                
            // Use params if available, but ensure result is robust
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                ROOT_ID -> {
                    // Return genre folders and all stations folder
                    listOf(
                        createBrowsableItem(STATIONS_ID, "🎵 Todas las Emisoras", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS),
                        createBrowsableItem(GENRE_POP, "🎤 Pop", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS),
                        createBrowsableItem(GENRE_NEWS, "📰 Noticias", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS),
                        createBrowsableItem(GENRE_ROCK, "🎸 Rock", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS),
                        createBrowsableItem(GENRE_DANCE, "💃 Dance", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                    )
                }
                STATIONS_ID -> {
                    // Return all radio stations ORDERED by User Preferences (Favorites First)
                    NavigationUtils.getAllOrderedStations().map { station ->
                        createMediaItemFromStation(station)
                    }
                }
                // Use getAllOrderedStations() for genres too, so Favorites appear at top of Genre lists
                GENRE_POP -> getStationsByGenre(listOf("Pop", "Latino"))
                GENRE_NEWS -> getStationsByGenre(listOf("Noticias"))
                GENRE_ROCK -> getStationsByGenre(listOf("Rock", "Pop/Rock"))
                GENRE_DANCE -> getStationsByGenre(listOf("Dance"))
                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
        }
        
        private fun createBrowsableItem(mediaId: String, title: String, mediaType: Int): MediaItem {
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(mediaType)
                        .build()
                )
                .build()
        }
        
        private fun getStationsByGenre(genres: List<String>): List<MediaItem> {
            // Use NavigationUtils.getAllOrderedStations() to respect Favorites logic inside Genres
            return NavigationUtils.getAllOrderedStations()
                .filter { station -> genres.any { genre -> station.genre.contains(genre, ignoreCase = true) } }
                .map { createMediaItemFromStation(it) }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = RadioStations.stations.find { it.id.toString() == mediaId }
            return if (station != null) {
                Futures.immediateFuture(LibraryResult.ofItem(createMediaItemFromStation(station), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Resolve media items with full URIs for playback
            val resolvedItems = mediaItems.map { item ->
                val station = RadioStations.stations.find { it.id.toString() == item.mediaId }
                if (station != null) {
                    createPlayableMediaItem(station)
                } else {
                    item
                }
            }.toMutableList()
            return Futures.immediateFuture(resolvedItems)
        }

        private fun createMediaItemFromStation(station: RadioStation): MediaItem {
            return MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setSubtitle(station.genre)
                        .setArtist(station.genre)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .setArtworkData(
                            getCachedStationArtwork(station),
                            MediaMetadata.PICTURE_TYPE_FRONT_COVER
                        )
                        .build()
                )
                .build()
        }

        private fun createPlayableMediaItem(station: RadioStation): MediaItem {
            return MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setSubtitle(station.genre)
                        .setArtist(station.genre)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .setArtworkData(
                            getCachedStationArtwork(station),
                            MediaMetadata.PICTURE_TYPE_FRONT_COVER
                        )
                        .build()
                )
                .build()
        }
    }
    
    private fun calculateStationColor(station: RadioStation?): Int {
        if (station == null) return 0xFF1C1B1F.toInt()
        
        try {
            // Get bitmap
            val bitmap = getCachedStationArtworkBitmap(station) ?: return 0xFF1C1B1F.toInt()
            
            // Extract dominant color synchronously (we are in background service)
            val palette = Palette.from(bitmap).generate()
            
            // Prefer Dark Vibrant for readability on white text, or Dominant
            // Defaults to Dark Grey if nothing found
            val defaultColor = 0xFF1C1B1F.toInt()
            return palette.getDarkVibrantColor(
                palette.getVibrantColor(
                    palette.getDominantColor(defaultColor)
                )
            )
        } catch (e: Exception) {
             AppLogger.e("RadioMediaService", "Error extracting color", e)
             return 0xFF1C1B1F.toInt()
        }
    }

    private fun getCachedStationArtworkBitmap(station: RadioStation): Bitmap? {
         return try {
             // Try cache first
             val bytes = artworkCache[station.logoResId]
             if (bytes != null) {
                 BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
             } else {
                 // Fallback to resource
                 BitmapFactory.decodeResource(resources, station.logoResId)
             }
         } catch (e: Exception) {
             null
         }
    }

    private fun getCachedStationArtwork(station: RadioStation): ByteArray? {
        // Check cache first
        artworkCache[station.logoResId]?.let { return it }
        
        return try {
            val originalBitmap = BitmapFactory.decodeResource(resources, station.logoResId)
            
            // Scale down for Android media widget (256px max dimension)
            // This prevents oversized logos in notification and lock screen media controls
            val maxSize = 256
            val bitmap = if (originalBitmap.width > maxSize || originalBitmap.height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }
            
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val bytes = stream.toByteArray()
            
            // Cache the result
            artworkCache[station.logoResId] = bytes
            bytes
        } catch (e: Exception) {
            null
        }
    }

    
    private fun startAlarmFadeIn() {
        cancelAlarmFadeIn()
        
        // Start from 0% volume (silence)
        player.volume = 0.0f
        
        fadeJob = serviceScope.launch {
            val duration = 45_000L // 45 seconds (very smooth)
            val steps = 30
            val stepTime = duration / steps
            
            // Initial delay to let the stream buffer silently
            delay(500)
            
            // Ramp from 0.0 to 1.0
            for (i in 1..steps) {
                // Exponential-like curve (x^2) sounds more linear to human ear than linear volume
                val progress = i / steps.toFloat()
                val volume = progress * progress 
                
                player.volume = volume.coerceAtLeast(0.02f) // Minimum audible level
                delay(stepTime)
            }
            player.volume = 1.0f
        }
    }
    
    private fun cancelAlarmFadeIn() {
        if (fadeJob != null) AppLogger.d("RadioMediaService", "Fade-in job cancelled")
        fadeJob?.cancel()
        fadeJob = null
    }
    
    private fun ensureAlarmStreamVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            
            // If volume is too low (< 30%), boost it
            // This prevents alarms from being silent
            val minVolume = (maxVolume * 0.3).toInt()
            if (currentVolume < minVolume) {
                AppLogger.w("RadioMediaService", "Alarm volume too low ($currentVolume), boosting to $minVolume")
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_ALARM, 
                    minVolume, 
                    0
                )
            }
            
            // ALWAYS force alarm volume to 90% - the user needs to wake up!
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_ALARM,
                (maxVolume * 0.9).toInt(),
                0
            )
        } catch (e: Exception) {
            AppLogger.e("RadioMediaService", "Error checking alarm volume", e)
        }
    }

    private var networkRecoveryJob: Job? = null

    /**
     * Starts monitoring for network recovery.
     * When network returns, it automatically resumes playback and cancels itself.
     */
    private fun startNetworkRecoveryMonitor() {
        // Cancel any existing monitor to avoid duplicates
        networkRecoveryJob?.cancel()
        
        AppLogger.i("RadioMediaService", "📡 Starting Network Recovery Monitor...")
        
        networkRecoveryJob = serviceScope.launch {
            networkMonitor.observeNetworkChanges()
                .collect { networkState ->
                    // Check if we have a usable connection
                    val isConnected = networkState == com.radioandroid.util.NetworkState.CONNECTED ||
                                     networkState == com.radioandroid.util.NetworkState.UNMETERED
                    
                    if (isConnected) {
                        // Double check with real ping before resuming
                        val hasRealInternet = SmartNetworkValidator.hasRealInternetConnection()
                        
                        if (hasRealInternet) {
                            AppLogger.i("RadioMediaService", "✅ Network recovered & Verified - Resuming playback")
                            
                            withContext(Dispatchers.Main) {
                                overlayManager.showOverlay("✅ Conexión recuperada", duration = 3000)
                                showToast("▶️ Reanudando...")
                                smartRetryManager.resetRetryCounter()
                                
                                // Resume playback
                                player.prepare()
                                player.play()
                                
                                // Stop monitoring since we are back online
                                networkRecoveryJob?.cancel()
                                networkRecoveryJob = null
                            }
                        } else {
                            AppLogger.d("RadioMediaService", "Network connected but no internet yet (ping failed)")
                        }
                    }
                }
        }
    }

    private fun updateAppWidgetWithColor() {
        val isLoading = if (::player.isInitialized) player.playbackState == Player.STATE_BUFFERING else false
        val isPlaying = if (::player.isInitialized) player.isPlaying else false
        
        // Calculate dynamic color from current station logo
        val dynamicColor = calculateStationColor(currentAttemptedStation)
        
        WidgetStateManager.updateState(
            applicationContext, 
            currentAttemptedStation, 
            isPlaying,
            isLoading,
            dynamicColor
        )
    }
    private fun isConnectionRefused(error: PlaybackException): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is java.net.ConnectException || 
                cause is java.net.UnknownHostException ||
                cause.message?.contains("refused", ignoreCase = true) == true ||
                cause.message?.contains("failed to connect", ignoreCase = true) == true) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}
