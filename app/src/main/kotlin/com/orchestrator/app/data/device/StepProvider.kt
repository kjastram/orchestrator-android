package com.orchestrator.app.data.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
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
class StepProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : StepReader {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /**
     * Reads the cumulative-since-boot step count from [Sensor.TYPE_STEP_COUNTER]. The sensor
     * is async/on-change, so we register a listener and wait up to [TIMEOUT_MS] for the first
     * event. Returns null on timeout, missing sensor, or missing ACTIVITY_RECOGNITION grant.
     * Never throws.
     */
    override suspend fun readCumulative(): Int? {
        val sm = sensorManager ?: return null
        if (!hasActivityRecognitionPermission()) return null
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        return try {
            withTimeout(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val delivered = AtomicBoolean(false)
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            if (delivered.compareAndSet(false, true) && cont.isActive) {
                                cont.resume(event.values.firstOrNull()?.toInt())
                            }
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }

                    cont.invokeOnCancellation {
                        try {
                            sm.unregisterListener(listener)
                        } catch (_: Exception) {
                        }
                    }

                    val registered = sm.registerListener(
                        listener, sensor, SensorManager.SENSOR_DELAY_FASTEST
                    )
                    if (!registered && cont.isActive) {
                        cont.resume(null)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            null
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
    }
}
