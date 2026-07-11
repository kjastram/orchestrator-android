package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Telemetry payload POSTed to the backend. Every field is nullable: a battery-only
 * partial row (no GPS fix) is valid, so location + [fixTimestamp] are omitted then.
 * `received_at` is server-set and deliberately absent here.
 */
data class DeviceTelemetryData(
    @SerializedName("battery_percent")
    val batteryPercent: Int? = null,
    @SerializedName("is_charging")
    val isCharging: Boolean? = null,
    @SerializedName("battery_temp_c")
    val batteryTempC: Double? = null,
    @SerializedName("battery_health")
    val batteryHealth: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("accuracy_m")
    val accuracyM: Double? = null,
    @SerializedName("fix_timestamp")
    val fixTimestamp: String? = null
)
