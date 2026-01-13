package com.radioandroid.player

import com.radioandroid.data.RadioStation

/**
 * Sealed class representing all possible playback states.
 * Following Kotlin/Android best practices 2024/2025 for state management.
 * 
 * Benefits:
 * - Exhaustive when expressions
 * - Type-safe state handling
 * - Clear state transitions
 * - Better Compose stability (immutable)
 */
sealed class PlaybackState {
    
    /**
     * Initial state - no station selected, player idle
     */
    data object Idle : PlaybackState()
    
    /**
     * Loading/buffering a station
     */
    data class Loading(val station: RadioStation) : PlaybackState()
    
    /**
     * Playing audio successfully
     */
    data class Playing(val station: RadioStation) : PlaybackState()
    
    /**
     * Paused by user
     */
    data class Paused(val station: RadioStation) : PlaybackState()
    
    /**
     * Error occurred during playback
     */
    data class Error(
        val station: RadioStation,
        val message: String,
        val cause: Throwable? = null
    ) : PlaybackState()
    
    // Helper properties for UI state checks
    val isPlaying: Boolean get() = this is Playing
    val isLoading: Boolean get() = this is Loading
    val hasError: Boolean get() = this is Error
    
    val currentStation: RadioStation? get() = when (this) {
        is Idle -> null
        is Loading -> station
        is Playing -> station
        is Paused -> station
        is Error -> station
    }
}

/**
 * Sealed class for favorite toggle results
 * Clean error handling following Kotlin best practices
 */
sealed class FavoriteResult {
    data class Added(val stationId: Int) : FavoriteResult()
    data class Removed(val stationId: Int) : FavoriteResult()
    data class LimitReached(val limit: Int) : FavoriteResult()
}

/**
 * Sealed class for timer states
 */
sealed class TimerState {
    data object Inactive : TimerState()
    data class Active(val remainingSeconds: Long) : TimerState()
    data object Completed : TimerState()
}

/**
 * Sealed class for alarm states
 */
sealed class AlarmState {
    data object NotSet : AlarmState()
    data class Scheduled(
        val hour: Int,
        val minute: Int,
        val stationId: Int
    ) : AlarmState()
    data class Ringing(val stationId: Int) : AlarmState()
}
