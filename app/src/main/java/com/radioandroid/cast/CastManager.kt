package com.radioandroid.cast

import android.content.Context
import android.net.Uri
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.common.collect.ImmutableList
import com.radioandroid.data.RadioStation
import com.radioandroid.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Cast sessions and CastPlayer for RadioFlow+.
 * Handles seamless switching between local and remote playback.
 */
class CastManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CastManager"
        
        @Volatile
        private var instance: CastManager? = null
        
        fun getInstance(context: Context): CastManager {
            return instance ?: synchronized(this) {
                instance ?: CastManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private var castContext: CastContext? = null
    private var castPlayer: CastPlayer? = null
    
    // State flows for UI
    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()
    
    private val _castDeviceAvailable = MutableStateFlow(false)
    val castDeviceAvailable: StateFlow<Boolean> = _castDeviceAvailable.asStateFlow()
    
    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()
    
    // Callback for when Cast session changes
    var onCastSessionStarted: (() -> Unit)? = null
    var onCastSessionEnded: (() -> Unit)? = null
    
    private var currentStation: RadioStation? = null
    
    // Cast state listener
    private val castStateListener = CastStateListener { state ->
        AppLogger.d(TAG, "Cast state changed: $state")
        _castDeviceAvailable.value = state != CastState.NO_DEVICES_AVAILABLE
    }
    
    /**
     * Initialize Cast. Call from Application or MainActivity.
     */
    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castContext?.addCastStateListener(castStateListener)
            
            // Create CastPlayer
            castPlayer = CastPlayer(castContext!!).apply {
                setSessionAvailabilityListener(object : SessionAvailabilityListener {
                    override fun onCastSessionAvailable() {
                        AppLogger.i(TAG, "✅ Cast session available")
                        _isCasting.value = true
                        _castDeviceName.value = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName
                        onCastSessionStarted?.invoke()
                        
                        // Start playing current station on Cast
                        currentStation?.let { playOnCast(it) }
                    }
                    
                    override fun onCastSessionUnavailable() {
                        AppLogger.i(TAG, "❌ Cast session ended")
                        _isCasting.value = false
                        _castDeviceName.value = null
                        onCastSessionEnded?.invoke()
                    }
                })
            }
            
            // Check initial state
            _castDeviceAvailable.value = (castContext?.castState ?: CastState.NO_DEVICES_AVAILABLE) != CastState.NO_DEVICES_AVAILABLE
            _isCasting.value = castContext?.sessionManager?.currentCastSession != null
            
            AppLogger.i(TAG, "CastManager initialized. Devices available: ${_castDeviceAvailable.value}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize Cast", e)
        }
    }
    
    /**
     * Get the CastPlayer instance (for use by RadioMediaService).
     */
    fun getCastPlayer(): CastPlayer? = castPlayer
    
    /**
     * Get the CastContext (for MediaRouteButton).
     */
    fun getCastContext(): CastContext? = castContext
    
    /**
     * Play a radio station on the Cast device.
     */
    fun playOnCast(station: RadioStation) {
        currentStation = station
        
        val player = castPlayer ?: return
        if (!_isCasting.value) return
        
        try {
            // Build MediaItem for Cast
            val mediaItem = buildCastMediaItem(station)
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            
            AppLogger.i(TAG, "🎵 Playing on Cast: ${station.name}")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error playing on Cast", e)
        }
    }
    
    /**
     * Stop Cast playback.
     */
    fun stopCastPlayback() {
        castPlayer?.stop()
    }
    
    /**
     * Build a MediaItem suitable for Cast with metadata.
     */
    private fun buildCastMediaItem(station: RadioStation): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.genre)
            .setIsPlayable(true)
            .build()
        
        return MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .setMimeType(getMimeType(station.streamUrl))
            .build()
    }
    
    /**
     * Determine MIME type from URL for Cast.
     */
    private fun getMimeType(url: String): String {
        return when {
            url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            url.contains(".aac", ignoreCase = true) -> MimeTypes.AUDIO_AAC
            url.contains(".mp3", ignoreCase = true) -> MimeTypes.AUDIO_MPEG
            url.contains(".ogg", ignoreCase = true) -> MimeTypes.AUDIO_OGG
            else -> MimeTypes.AUDIO_MPEG // Default to MP3
        }
    }
    
    /**
     * Check if currently casting.
     */
    fun isCastSessionActive(): Boolean = _isCasting.value
    
    /**
     * Update the current station (for session start).
     */
    fun setCurrentStation(station: RadioStation) {
        currentStation = station
    }
    
    /**
     * Release resources.
     */
    fun release() {
        castContext?.removeCastStateListener(castStateListener)
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        castPlayer = null
        instance = null
    }
}
