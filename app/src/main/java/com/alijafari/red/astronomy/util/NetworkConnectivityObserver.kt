package com.alijafari.red.astronomy.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.alijafari.red.astronomy.astro_engine.TimeEngine
import com.alijafari.red.astronomy.astro_engine.TleMetadata
import java.util.Locale
import kotlin.math.max

/**
 * Synchronous snapshot of current internet capability, safe in restricted or unit-test environments.
 */
fun isNetworkOnlineNow(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}

/**
 * Reactive Compose connectivity observer backed by [ConnectivityManager.NetworkCallback]
 * with safe fallback when callbacks are unavailable.
 */
@Composable
fun rememberIsNetworkOnline(context: Context): State<Boolean> {
    val appContext = remember(context) { context.applicationContext ?: context }
    val onlineState = remember(appContext) { mutableStateOf(isNetworkOnlineNow(appContext)) }

    DisposableEffect(appContext) {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            onDispose { }
        } else {
            onlineState.value = isNetworkOnlineNow(appContext)
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = runCatching { cm.getNetworkCapabilities(network) }.getOrNull()
                    onlineState.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: true
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    onlineState.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onLost(network: Network) {
                    onlineState.value = isNetworkOnlineNow(appContext)
                }

                override fun onUnavailable() {
                    onlineState.value = false
                }
            }

            var registered = false
            try {
                cm.registerDefaultNetworkCallback(callback)
                registered = true
            } catch (_: Exception) {
                try {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    cm.registerNetworkCallback(request, callback)
                    registered = true
                } catch (_: Exception) {
                    // Safe fallback in restricted/test environments
                }
            }

            onDispose {
                if (registered) {
                    runCatching { cm.unregisterNetworkCallback(callback) }
                }
            }
        }
    }

    return onlineState
}

/**
 * Formats a compact, non-intrusive TLE age / train-drift warning label matching AR Sky's
 * transparency (`> 48h` / `2.0 days` when viewing an active Starlink Train, or `> 7.0 days` overall).
 * Returns `null` when orbital elements are fresh.
 */
fun formatTleStalenessBadge(
    isViewingActiveTrain: Boolean,
    tleMetadata: TleMetadata?,
    starlinkCacheAgeDays: Double? = null,
    isFa: Boolean = false
): String? {
    val metaAgeDays = tleMetadata?.epochAgeDays?.coerceAtLeast(0.0)
    val cacheAgeDays = starlinkCacheAgeDays?.coerceAtLeast(0.0)

    val effectiveAgeDays = if (isViewingActiveTrain) {
        when {
            metaAgeDays != null && cacheAgeDays != null -> max(metaAgeDays, cacheAgeDays)
            metaAgeDays != null -> metaAgeDays
            else -> cacheAgeDays ?: return null
        }
    } else {
        metaAgeDays ?: cacheAgeDays ?: return null
    }

    if (!effectiveAgeDays.isFinite()) return null

    if (isViewingActiveTrain && effectiveAgeDays > 2.0) {
        val rawDays = String.format(Locale.US, "%.1f", effectiveAgeDays)
        return if (isFa) {
            "TLE ${TimeEngine.formatPersianNumbers(rawDays)} روز • خطای مدار"
        } else {
            "TLE ${rawDays}d • Drift"
        }
    }

    if (effectiveAgeDays > 7.0) {
        val rawDays = String.format(Locale.US, "%.1f", effectiveAgeDays)
        return if (isFa) {
            "TLE: ${TimeEngine.formatPersianNumbers(rawDays)} روزه"
        } else {
            "TLE: ${rawDays}d old"
        }
    }

    return null
}
