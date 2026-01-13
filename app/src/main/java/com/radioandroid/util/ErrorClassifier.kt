package com.radioandroid.util

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Classifies playback errors into actionable categories
 */
class ErrorClassifier(private val networkMonitor: NetworkMonitor) {
    
    /**
     * Classify a PlaybackException into an error type
     */
    fun classifyError(exception: PlaybackException): ErrorType {
        // First check network availability
        if (!networkMonitor.isNetworkAvailable()) {
            return ErrorType.NetworkLoss
        }
        
        // Analyze the exception
        return when (val cause = exception.cause) {
            is HttpDataSource.HttpDataSourceException -> classifyHttpError(cause)
            is UnknownHostException -> ErrorType.StationFailure(httpCode = null)
            is SocketTimeoutException -> classifyTimeoutError()
            is IOException -> classifyIOError(cause)
            else -> classifyExoPlayerError(exception)
        }
    }
    
    private fun classifyHttpError(exception: HttpDataSource.HttpDataSourceException): ErrorType {
        return when (exception) {
            is HttpDataSource.InvalidResponseCodeException -> {
                when (exception.responseCode) {
                    in 400..499 -> ErrorType.StationFailure(httpCode = exception.responseCode)
                    in 500..599 -> ErrorType.StationFailure(httpCode = exception.responseCode)
                    else -> ErrorType.TemporaryGlitch
                }
            }
            else -> {
                // Connection errors while network is available = station problem
                if (networkMonitor.isConnectedToInternet()) {
                    ErrorType.StationFailure(httpCode = null)
                } else {
                    ErrorType.TemporaryGlitch
                }
            }
        }
    }
    
    private fun classifyTimeoutError(): ErrorType {
        // If we have internet but got timeout, it's likely the station
        return if (networkMonitor.isConnectedToInternet()) {
            ErrorType.TemporaryGlitch // Give it a chance to retry
        } else {
            ErrorType.NetworkLoss
        }
    }
    
    private fun classifyIOError(exception: IOException): ErrorType {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true -> {
                if (networkMonitor.isNetworkAvailable()) {
                    ErrorType.TemporaryGlitch
                } else {
                    ErrorType.NetworkLoss
                }
            }
            exception.message?.contains("connection", ignoreCase = true) == true -> {
                ErrorType.StationFailure(httpCode = null)
            }
            else -> ErrorType.Unknown(exception)
        }
    }
    
    private fun classifyExoPlayerError(exception: PlaybackException): ErrorType {
        return when (exception.errorCode) {
            // Network connection errors - check if station or network issue
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                if (networkMonitor.isNetworkAvailable()) {
                    ErrorType.StationFailure(httpCode = null)
                } else {
                    ErrorType.NetworkLoss
                }
            }
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> ErrorType.TemporaryGlitch
            
            // HTTP and content errors - station is broken, skip immediately
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> ErrorType.StationFailure(httpCode = null)
            
            // PARSING ERRORS - Stream returns 200 but content is invalid (e.g., HTML error page)
            // These should ALWAYS skip to next station immediately
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> ErrorType.StationFailure(httpCode = null)
            
            // DECODING ERRORS - Audio format not supported or corrupted
            PlaybackException.ERROR_CODE_DECODING_FAILED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> ErrorType.StationFailure(httpCode = null)
            
            // AUDIO TRACK ERRORS - No playable audio in the stream
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> ErrorType.StationFailure(httpCode = null)
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> ErrorType.StationFailure(httpCode = null)
            
            // IO errors that indicate broken stream (not network)
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                // Check if this is really a network issue
                if (networkMonitor.isConnectedToInternet()) {
                    ErrorType.StationFailure(httpCode = null) // Network OK, stream is broken
                } else {
                    ErrorType.TemporaryGlitch // Could be network
                }
            }
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> ErrorType.StationFailure(httpCode = null)
            
            // Default: unknown error, try temporary glitch first
            else -> ErrorType.Unknown(exception)
        }
    }
    
    /**
     * Get human-readable error message based on error type
     */
    fun getErrorMessage(errorType: ErrorType, stationName: String?): String {
        return when (errorType) {
            is ErrorType.NetworkLoss -> "📶 Sin conexión - Esperando cobertura..."
            is ErrorType.StationFailure -> {
                val name = stationName ?: "Emisora"
                "⚠️ $name no disponible - Cambiando..."
            }
            is ErrorType.TemporaryGlitch -> {
                val name = stationName ?: "Emisora"
                "🔄 Reconectando $name..."
            }
            is ErrorType.Unknown -> "❌ Error de reproducción"
        }
    }
}

/**
 * Types of errors that can occur during playback
 */
sealed class ErrorType {
    /**
     * No network connection available
     * Action: Pause and wait for network to return
     */
    object NetworkLoss : ErrorType()
    
    /**
     * Station is not responding or URL is invalid
     * Action: Mark as unavailable and skip to next
     */
    data class StationFailure(val httpCode: Int?) : ErrorType()
    
    /**
     * Temporary network issue or slow connection
     * Action: Retry a few times before giving up
     */
    object TemporaryGlitch : ErrorType()
    
    /**
     * Unknown error type
     * Action: Try to recover or skip
     */
    data class Unknown(val exception: Exception) : ErrorType()
}
