package avinash.app.audiocallapp.webrtc

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity changes and notifies listeners.
 * Used to trigger ICE restart when network type changes (Wi-Fi ↔ mobile data).
 */
@Singleton
class NetworkChangeHandler @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioCallApp"
    }

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoring = false

    /**
     * Observe network changes as a Flow.
     * Emits NetworkChangeEvent when network connectivity changes.
     */
    fun observeNetworkChanges(): Flow<NetworkChangeEvent> = callbackFlow {
        // If already monitoring, unregister the old callback first
        if (isMonitoring && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
                Log.d(TAG, "Unregistered previous network callback")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister existing callback", e)
            }
            networkCallback = null
            isMonitoring = false
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                val networkType = getNetworkType(network)
                trySend(NetworkChangeEvent.NetworkAvailable(networkType))
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                trySend(NetworkChangeEvent.NetworkLost)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                if (hasInternet && isValidated) {
                    val networkType = getNetworkType(network)
                    val previousType = getCurrentNetworkType()
                    
                    // Only emit if network type actually changed
                    if (previousType != networkType) {
                        Log.d(TAG, "Network type changed: $previousType -> $networkType")
                        trySend(NetworkChangeEvent.NetworkTypeChanged(previousType, networkType))
                    }
                }
            }

            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                trySend(NetworkChangeEvent.NetworkUnavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)
            networkCallback = callback
            isMonitoring = true
            Log.d(TAG, "Network monitoring started")

            // Send initial network state
            val currentNetwork = connectivityManager.activeNetwork
            if (currentNetwork != null) {
                val networkType = getNetworkType(currentNetwork)
                trySend(NetworkChangeEvent.NetworkAvailable(networkType))
            } else {
                trySend(NetworkChangeEvent.NetworkUnavailable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            close(e)
            return@callbackFlow
        }

        awaitClose {
            networkCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                    Log.d(TAG, "Network monitoring stopped")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to unregister network callback", e)
                }
            }
            networkCallback = null
            isMonitoring = false
        }
    }

    /**
     * Get the type of the current active network.
     */
    fun getCurrentNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        return getNetworkType(network)
    }

    /**
     * Check if device has internet connectivity.
     */
    fun hasInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun getNetworkType(network: Network): NetworkType {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
    }

    /**
     * Stop monitoring network changes.
     */
    fun stop() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
                Log.d(TAG, "Network monitoring stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
        networkCallback = null
        isMonitoring = false
    }
}

/**
 * Represents different types of network connections.
 */
enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    VPN,
    UNKNOWN,
    NONE
}

/**
 * Events emitted when network connectivity changes.
 */
sealed class NetworkChangeEvent {
    data class NetworkAvailable(val networkType: NetworkType) : NetworkChangeEvent()
    data class NetworkTypeChanged(val from: NetworkType, val to: NetworkType) : NetworkChangeEvent()
    object NetworkLost : NetworkChangeEvent()
    object NetworkUnavailable : NetworkChangeEvent()
}
