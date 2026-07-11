package com.orchestrator.app.data.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryReading(
    val percent: Int?,
    val isCharging: Boolean?,
    val tempC: Double?,
    val health: String?,
    val currentUa: Int?,
    val voltageMv: Int?,
    val chargeSource: String?
)

@Singleton
class BatteryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Reads the current battery state from the sticky ACTION_BATTERY_CHANGED broadcast.
     * Tolerates missing/absent extras (returns nulls) — the intent itself can be null
     * before the framework has published a value.
     */
    fun read(): BatteryReading {
        val intent: Intent? =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val currentUa = readCurrentUa()

        if (intent == null) {
            return BatteryReading(null, null, null, null, currentUa, null, null)
        }

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = when (status) {
            -1 -> null
            else -> status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tempC = if (tempTenths > 0) tempTenths / 10.0 else null

        val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val health = mapHealth(healthRaw)

        val voltageRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val voltageMv = if (voltageRaw > 0) voltageRaw else null

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val chargeSource = mapChargeSource(plugged)

        return BatteryReading(percent, isCharging, tempC, health, currentUa, voltageMv, chargeSource)
    }

    /**
     * Instantaneous battery current in microamps. Units/sign are OEM-specific — store raw.
     * Guards the sentinel [Long.MIN_VALUE] (returned when unsupported) and 0 → null.
     */
    private fun readCurrentUa(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val raw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return if (raw == Long.MIN_VALUE || raw == 0L) null else raw.toInt()
    }

    private fun mapChargeSource(plugged: Int): String? = when (plugged) {
        -1 -> null
        0 -> "none"
        BatteryManager.BATTERY_PLUGGED_AC -> "ac"
        BatteryManager.BATTERY_PLUGGED_USB -> "usb"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
        else -> "none"
    }

    private fun mapHealth(raw: Int): String? = when (raw) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "unspecified_failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "cold"
        BatteryManager.BATTERY_HEALTH_UNKNOWN -> "unknown"
        else -> null
    }
}
