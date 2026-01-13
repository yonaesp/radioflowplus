package com.radioandroid.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay

/**
 * Network utilities for handling connectivity and retry logic
 */
object NetworkUtils {
    
    /**
     * Check if the device has an active network connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Retry a suspending operation with exponential backoff
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @param factor Multiplier for exponential backoff
     * @param block The operation to retry
     */
    suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 16000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                // Log the error (could be enhanced with proper logging)
                e.printStackTrace()
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
        // Last attempt - let exception propagate
        return block()
    }
    
    /**
     * Get connection quality based on network type
     */
    fun getConnectionQuality(context: Context): ConnectionQuality {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ConnectionQuality.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionQuality.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionQuality.EXCELLENT
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionQuality.EXCELLENT
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (capabilities.linkDownstreamBandwidthKbps >= 10000) {
                    ConnectionQuality.GOOD
                } else if (capabilities.linkDownstreamBandwidthKbps >= 1000) {
                    ConnectionQuality.MODERATE
                } else {
                    ConnectionQuality.POOR
                }
            }
            else -> ConnectionQuality.MODERATE
        }
    }
    
    enum class ConnectionQuality {
        NONE,
        POOR,
        MODERATE,
        GOOD,
        EXCELLENT
    }
}
