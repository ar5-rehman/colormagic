package com.colormagic.kids.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports whether the device currently has a usable internet connection.
 *
 * The app is online-first — generating a coloring page, refreshing credits and
 * signing in all need the network. This monitor lets the UI shimmer/disable the
 * right things and auto-recover (re-sign-in, refresh quota) the moment the
 * connection returns, instead of leaving the user on a stale error.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Emits the current connectivity, then every change. Starts with the
     *  device's present state so the first frame is already accurate. */
    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)

        fun online(): Boolean {
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(online()) }
            override fun onLost(network: Network) { trySend(online()) }
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) {
                trySend(
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }
        }

        // Seed the current state so collectors don't wait for the first change.
        trySend(online())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, callback)

        awaitClose { cm?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
