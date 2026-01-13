package com.radioandroid.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Validates actual internet connectivity by attempting a lightweight connection.
 * This distinguishes between "connected to WiFi but no internet" and "real internet access".
 */
object SmartNetworkValidator {

    private const val VALIDATION_HOST = "8.8.8.8" // Google DNS
    private const val VALIDATION_PORT = 53        // DNS Port (very reliable)
    private const val TIMEOUT_MS = 1500           // Fast timeout

    /**
     * Checks if the device has real internet access by attempting a socket connection.
     * This is a blocking call and should be run on IO dispatcher.
     */
    suspend fun hasRealInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(VALIDATION_HOST, VALIDATION_PORT), TIMEOUT_MS)
                true
            }
        } catch (e: IOException) {
            AppLogger.d("SmartNetworkValidator", "Internet validation failed: ${e.message}")
            false
        }
    }
}
