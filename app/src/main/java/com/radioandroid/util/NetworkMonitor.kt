package com.radioandroid.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitors network connectivity and provides real-time updates
 */
class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Check if device has any network connection (WiFi or Mobile)
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Check if device is connected to internet (with validation)
     */
    fun isConnectedToInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        } && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Get current network type
     */
    fun getNetworkType(): NetworkType {
        if (!isNetworkAvailable()) return NetworkType.NONE
        
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }
    
    /**
     * Check if connection is metered (e.g., mobile data)
     */
    fun isMeteredConnection(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }
    
    /**
     * Observe network state changes as a Flow
     */
    fun observeNetworkChanges(): Flow<NetworkState> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.CONNECTED)
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.DISCONNECTED)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val isMetered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                val state = if (isMetered) NetworkState.METERED else NetworkState.UNMETERED
                trySend(state)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Send initial state
        val initialState = when {
            !isNetworkAvailable() -> NetworkState.DISCONNECTED
            isMeteredConnection() -> NetworkState.METERED
            else -> NetworkState.UNMETERED
        }
        trySend(initialState)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
    
    /**
     * Get detailed connection info
     */
    fun getConnectionInfo(): ConnectionInfo {
        return ConnectionInfo(
            isAvailable = isNetworkAvailable(),
            type = getNetworkType(),
            isMetered = isMeteredConnection(),
            hasInternet = isConnectedToInternet()
        )
    }
}

/**
 * Network connection state
 */
enum class NetworkState {
    CONNECTED,     // Any connection available
    DISCONNECTED,  // No connection
    METERED,       // Mobile data (metered)
    UNMETERED      // WiFi or unlimited (unmetered)
}

/**
 * Network connection type
 */
enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    OTHER,
    NONE
}

/**
 * Detailed connection information
 */
data class ConnectionInfo(
    val isAvailable: Boolean,
    val type: NetworkType,
    val isMetered: Boolean,
    val hasInternet: Boolean
)
