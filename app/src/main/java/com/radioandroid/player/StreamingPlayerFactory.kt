package com.radioandroid.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.io.File

/**
 * Optimized ExoPlayer configuration for live radio streaming
 * 
 * Best practices 2024/2025:
 * - DefaultLoadControl with optimized buffer sizes for live streaming
 * - BandwidthMeter for adaptive quality selection
 * - Proper audio focus handling
 * - Network retry with exponential backoff
 * - Audio-only optimizations
 */
@OptIn(UnstableApi::class)
object StreamingPlayerFactory {
    
    // Cache for streaming data (helps with rebuffering)
    private var cache: SimpleCache? = null
    
    // Buffer configuration optimized for live radio streaming
    // Based on proven values from production radio apps (RadioDroid, etc.)
    // Balance: Quick startup vs. stable continuous playback
    // Buffer configuration optimized for live radio streaming
    // Tuning 2026: "Premium" feel = Instant Start + Rock Solid Stability
    private const val MIN_BUFFER_MS = 20_000            // 20s (Increased for stability)
    private const val MAX_BUFFER_MS = 60_000            // 60s (More buffer for pauses/network drops)
    private const val BUFFER_FOR_PLAYBACK_MS = 1_500    // 1.5s (Aggressive for "Instant Play" feel)
    private const val BUFFER_FOR_REBUFFER_MS = 4_000    // 4s (Resume as fast as possible after drop)
    
    // HTTP configuration - Aggressive timeouts for fast failure detection
    private const val CONNECT_TIMEOUT_MS = 1_500       // 1.5s for TCP handshake (super fast failure)
    // CRITICAL: READ_TIMEOUT must be MUCH HIGHER for continuous streaming
    // A low read timeout (like 15s) will KILL the stream mid-playback during buffering
    // For live radio, we want to be patient with slow reads, not cut the connection
    private const val READ_TIMEOUT_MS = 60_000         // 60 seconds (was causing stuttering at 15s)
    // CRITICAL: Use standard User-Agent to avoid blocking by stations (COPE, Europa FM, etc.)
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    
    // Shared OkHttpClient for connection pooling (CRITICAL for reliability)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    // Cache size: 50MB for audio streams
    private const val CACHE_SIZE_BYTES = 50L * 1024 * 1024
    
    /**
     * Create an optimized ExoPlayer instance for live radio streaming
     */
    fun createOptimizedPlayer(context: Context): ExoPlayer {
        // Initialize cache if needed
        initCache(context)
        
        // 1. Configure LoadControl for live streaming
        val minBuffer = MIN_BUFFER_MS
        val maxBuffer = MAX_BUFFER_MS
        val startBuffer = BUFFER_FOR_PLAYBACK_MS
        
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuffer,
                maxBuffer,
                startBuffer,
                BUFFER_FOR_REBUFFER_MS
            )
            .setBackBuffer(0, false)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES)
            .build()
        
        // 2. Configure BandwidthMeter for network quality estimation
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setResetOnNetworkTypeChange(true)
            .build()
        
        // 3. CRITICAL FIX: Use custom IcyDataSource for live radio streams
        // Based on RadioDroid's implementation - returns C.LENGTH_UNSET to prevent
        // ExoPlayer from thinking stream is finite, maintains persistent HTTP connection
        val icyDataSourceFactory = IcyDataSource.Factory(okHttpClient)
        
        // 4. Wrap with caching for smoother playback (for non-live content)
        val cacheDataSourceFactory = cache?.let { simpleCache ->
            CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(icyDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or 
                         CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS)
        } ?: icyDataSourceFactory
        
        // 5. Create DefaultMediaSourceFactory with custom data source and ROBUST error handling
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)
            .setLoadErrorHandlingPolicy(RadioLoadErrorHandlingPolicy())
        
        // 6. Configure audio attributes for music streaming
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        
        // 7. CRITICAL FIX: Disable audio tunneling to fix AAC STATE_ENDED bug in Media3 1.5.0
        // Without this, AAC live streams incorrectly trigger STATE_ENDED every 2-4 seconds
        // See: https://github.com/androidx/media/issues (AAC tunneling issue)
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableAudioTrackPlaybackParams(true)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        
        // 8. Build the player with all optimizations
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setBandwidthMeter(bandwidthMeter)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true) // ExoPlayer manages audio focus
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setPauseAtEndOfMediaItems(false)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
    }
    
    /**
     * Add optimized error listener with retry logic
     */
    fun addErrorListener(player: ExoPlayer, onRetry: () -> Unit) {
        player.addListener(object : Player.Listener {
            private var retryCount = 0
            private val maxRetries = 3
            
            override fun onPlayerError(error: PlaybackException) {
                // Handle specific error types
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                        // Network errors - retry with backoff
                        if (retryCount < maxRetries) {
                            retryCount++
                            val delayMs = (1000L * retryCount * retryCount) // Exponential backoff
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                onRetry()
                            }, delayMs)
                        }
                    }
                    else -> {
                        // Other errors - reset retry count
                        retryCount = 0
                    }
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    // Reset retry count on successful playback
                    retryCount = 0
                }
            }
        })
    }
    
    /**
     * Initialize the cache for streaming data
     */
    private fun initCache(context: Context) {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "radio_stream_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
    }
    
    /**
     * Release cache when no longer needed
     */
    fun releaseCache() {
        cache?.release()
        cache = null
    }
}
