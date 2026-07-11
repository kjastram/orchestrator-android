package com.orchestrator.app.data.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /**
     * Requests a single fresh fix, GPS first then NETWORK, each capped at [TIMEOUT_MS].
     * Falls back to the most recent [LocationManager.getLastKnownLocation]. Returns null
     * if no permission, no enabled provider, or everything times out. Never throws.
     */
    suspend fun getFix(): Location? {
        val lm = locationManager ?: return null
        if (!hasLocationPermission()) return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        for (provider in providers) {
            if (!isProviderEnabled(lm, provider)) continue
            val fix = requestSingle(lm, provider)
            if (fix != null) return fix
        }

        // Fallback: last known from any provider.
        for (provider in providers) {
            val last = lastKnown(lm, provider)
            if (last != null) return last
        }
        return null
    }

    private suspend fun requestSingle(lm: LocationManager, provider: String): Location? {
        return try {
            withTimeout(TIMEOUT_MS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    getCurrentLocationApi30(lm, provider)
                } else {
                    requestSingleUpdateLegacy(lm, provider)
                }
            }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocationApi30(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            lm.getCurrentLocation(
                provider,
                signal,
                context.mainExecutor
            ) { location ->
                if (cont.isActive) cont.resume(location)
            }
        }

    @Suppress("MissingPermission", "DEPRECATION")
    private suspend fun requestSingleUpdateLegacy(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val delivered = AtomicBoolean(false)
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (delivered.compareAndSet(false, true) && cont.isActive) {
                        cont.resume(location)
                    }
                }

                override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {
                    if (delivered.compareAndSet(false, true) && cont.isActive) {
                        cont.resume(null)
                    }
                }
            }

            cont.invokeOnCancellation {
                try {
                    lm.removeUpdates(listener)
                } catch (_: SecurityException) {
                }
            }

            try {
                lm.requestSingleUpdate(provider, listener, context.mainLooper)
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }

    @Suppress("MissingPermission")
    private fun lastKnown(lm: LocationManager, provider: String): Location? = try {
        lm.getLastKnownLocation(provider)
    } catch (e: SecurityException) {
        null
    }

    private fun isProviderEnabled(lm: LocationManager, provider: String): Boolean = try {
        lm.isProviderEnabled(provider)
    } catch (e: Exception) {
        false
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    companion object {
        private const val TIMEOUT_MS = 20_000L
    }
}
