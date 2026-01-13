package com.radioandroid.util

import kotlin.math.min
import kotlin.math.pow

/**
 * Manages retry logic with exponential backoff
 */
class SmartRetryManager {
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
    }
    
    private var retryCount = 0
    private var lastErrorType: ErrorType? = null
    
    /**
     * Determine if we should retry based on error type and attempt count
     */
    fun shouldRetry(errorType: ErrorType, attemptCount: Int = retryCount): Boolean {
        return when (errorType) {
            is ErrorType.NetworkLoss -> false // Don't retry, wait for network
            is ErrorType.StationFailure -> false // Don't retry, skip station
            is ErrorType.TemporaryGlitch -> attemptCount < MAX_RETRY_ATTEMPTS
            is ErrorType.Unknown -> attemptCount < MAX_RETRY_ATTEMPTS // Give it a chance
        }
    }
    
    /**
     * Get delay before next retry with exponential backoff
     * Returns delay in milliseconds
     */
    fun getRetryDelay(attemptCount: Int = retryCount): Long {
        if (attemptCount == 0) return 0L
        
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(attemptCount - 1)).toLong()
        return min(exponentialDelay, MAX_DELAY_MS)
    }
    
    /**
     * Record a retry attempt
     */
    fun recordAttempt(errorType: ErrorType) {
        // Reset counter if error type changed
        if (lastErrorType != null && lastErrorType != errorType) {
            resetRetryCounter()
        }
        
        lastErrorType = errorType
        retryCount++
    }
    
    /**
     * Reset the retry counter
     */
    fun resetRetryCounter() {
        retryCount = 0
        lastErrorType = null
    }
    
    /**
     * Get current retry attempt count
     */
    fun getRetryCount(): Int = retryCount
    
    /**
     * Check if max retries reached
     */
    fun isMaxRetriesReached(): Boolean {
        return retryCount >= MAX_RETRY_ATTEMPTS
    }
    
    /**
     * Get retry strategy recommendation
     */
    fun getRetryStrategy(errorType: ErrorType): RetryStrategy {
        return when (errorType) {
            is ErrorType.NetworkLoss -> RetryStrategy.WaitForNetwork
            is ErrorType.StationFailure -> RetryStrategy.SkipStation
            is ErrorType.TemporaryGlitch -> {
                if (shouldRetry(errorType)) {
                    RetryStrategy.RetryWithBackoff(getRetryDelay())
                } else {
                    RetryStrategy.TreatAsStationFailure
                }
            }
            is ErrorType.Unknown -> {
                if (shouldRetry(errorType)) {
                    RetryStrategy.RetryWithBackoff(getRetryDelay())
                } else {
                    RetryStrategy.SkipStation
                }
            }
        }
    }
}

/**
 * Recommended retry strategy for an error
 */
sealed class RetryStrategy {
    /**
     * Wait for network to be restored
     */
    object WaitForNetwork : RetryStrategy()
    
    /**
     * Skip to next station immediately
     */
    object SkipStation : RetryStrategy()
    
    /**
     * Retry after specified delay
     */
    data class RetryWithBackoff(val delayMs: Long) : RetryStrategy()
    
    /**
     * Too many retries, treat as station failure
     */
    object TreatAsStationFailure : RetryStrategy()
}
