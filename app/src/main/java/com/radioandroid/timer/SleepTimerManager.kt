package com.radioandroid.timer

import android.content.Context
import android.media.AudioManager
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.radioandroid.premium.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Sleep Timer functionality
 * Free: 15, 30, 60 min with abrupt stop
 * Premium: Custom time (5min-4h) with fade-out
 */
object SleepTimerManager {
    
    // Predefined times (in minutes) - Free tier
    val freeTimes = listOf(15, 30, 60)
    
    // Timer state
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private var countDownTimer: CountDownTimer? = null
    private var fadeOutEnabled = false
    private var originalVolume: Int = -1
    private var audioManager: AudioManager? = null
    private var onTimerComplete: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun init(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    /**
     * Start timer with given minutes
     * @param minutes Duration in minutes
     * @param fadeOut Whether to fade out volume (premium only)
     * @param onComplete Callback when timer completes
     */
    fun startTimer(
        context: Context,
        minutes: Int,
        fadeOut: Boolean = false,
        onComplete: () -> Unit
    ): Boolean {
        // Cancel existing timer
        cancelTimer()
        
        fadeOutEnabled = fadeOut
        onTimerComplete = onComplete
        
        // Save original volume for fade-out
        if (fadeOutEnabled) {
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
        }
        onTimerComplete = onComplete
        
        // Save original volume for fade-out
        if (fadeOutEnabled) {
            originalVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
        }
        
        val totalMillis = minutes * 60 * 1000L
        
        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = millisUntilFinished / 1000
                
                // Fade out in last 2 minutes if enabled
                if (fadeOutEnabled && millisUntilFinished <= 120_000 && originalVolume > 0) {
                    val progress = millisUntilFinished / 120_000f
                    val newVolume = (originalVolume * progress).toInt().coerceAtLeast(0)
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                }
            }
            
            override fun onFinish() {
                _remainingSeconds.value = 0
                _isActive.value = false
                
                // Restore volume if faded
                if (fadeOutEnabled && originalVolume > 0) {
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                }
                
                onTimerComplete?.invoke()
                
                mainHandler.post {
                    Toast.makeText(context, "💤 Sleep Timer completado", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
        
        _isActive.value = true
        Toast.makeText(context, "💤 Timer: $minutes min", Toast.LENGTH_SHORT).show()
        return true
    }
    
    fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _remainingSeconds.value = 0
        _isActive.value = false
        
        // Restore volume if was fading
        if (fadeOutEnabled && originalVolume > 0) {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        }
        fadeOutEnabled = false
    }
    
    fun formatRemainingTime(): String {
        val seconds = _remainingSeconds.value
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
    
    fun getRemainingMinutes(): Int = (_remainingSeconds.value / 60).toInt()

    fun calculateMinutesUntil(targetHour: Int, targetMinute: Int): Int {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            set(java.util.Calendar.MINUTE, targetMinute)
            set(java.util.Calendar.SECOND, 0)
        }
        
        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        val diffMillis = target.timeInMillis - now.timeInMillis
        return (diffMillis / 60000).toInt().coerceAtLeast(1)
    }
}
