package com.radioandroid.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.radioandroid.util.AppLogger
import java.io.IOException

/**
 * Custom LoadErrorHandlingPolicy for Radio Streaming.
 * 
 * Goals:
 * 1. Aggressive retry strategy for connection drops (common in mobile radio)
 * 2. Infinite retries for live streams (never give up unless fatal)
 * 3. Fast initial retry (dont wait too long to reconnect)
 */
@UnstableApi
class RadioLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val errorCount = loadErrorInfo.errorCount
        
        // Log the error
        AppLogger.w("RadioErrorPolicy", "Load error: ${exception.message}, count: $errorCount")
        
        // CRITICAL: Detect FATAL errors that should NOT be retried
        if (isFatal(exception)) {
            AppLogger.e("RadioErrorPolicy", "⛔ Fatal error detected. Stopping retries.")
            return C.TIME_UNSET // Stop retrying
        }
        
        val callType = if (errorCount == 1) "Initial retry" else "Retry #$errorCount"
        
        // For live radio, we want fast reconnection.
        // Default backoff can get very long. Cap it at 5 seconds.
        val delay = super.getRetryDelayMsFor(loadErrorInfo)
        val cappedDelay = if (delay == C.TIME_UNSET) {
             C.TIME_UNSET
        } else {
             delay.coerceAtMost(5000) // Max 5 seconds wait
        }
        
        AppLogger.d("RadioErrorPolicy", "$callType in ${cappedDelay}ms")
        return cappedDelay
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        // For live streams, use infinite retries -> BUT getRetryDelayMsFor detects fatal errors
        return Int.MAX_VALUE
    }
    
    /**
     * Check if an exception is fatal and should not be retried.
     * CRITICAL: Definitive errors should skip IMMEDIATELY (< 1 second)
     */
    /**
     * Check if an exception is fatal and should not be retried.
     * CRITICAL: Definitive errors should skip IMMEDIATELY (< 1 second)
     */
    private fun isFatal(exception: IOException): Boolean {
        // 1. Recursive check for root causes (ConnectException, UnknownHostException, etc.)
        // ExoPlayer often wraps these in HttpDataSourceException
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is java.net.ConnectException) {
                AppLogger.w("RadioErrorPolicy", "⚡ Fatal ConnectException (root cause): ${cause.message}")
                return true
            }
            if (cause is java.net.UnknownHostException) {
                AppLogger.w("RadioErrorPolicy", "⚡ Fatal DNS error (root cause): ${cause.message}")
                return true
            }
            if (cause.message?.contains("refused", ignoreCase = true) == true) {
                AppLogger.w("RadioErrorPolicy", "⚡ Connection refused (root cause): ${cause.message}")
                return true
            }
            cause = cause.cause
        }

        // 2. Unrecognized Input Format
        if (exception.javaClass.name.contains("UnrecognizedInputFormatException")) {
            return true
        }

        // 3. HTTP 4xx/5xx Errors
        if (exception is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
            val code = exception.responseCode
            if (code in 400..599) {
                AppLogger.w("RadioErrorPolicy", "⚡ Fatal HTTP error: $code")
                return true
            }
        }
        
        // 4. Parser Exception
        if (exception is androidx.media3.common.ParserException) {
            return true
        }
        
        return false
    }
}
