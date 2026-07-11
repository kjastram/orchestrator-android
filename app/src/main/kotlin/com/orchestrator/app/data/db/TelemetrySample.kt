package com.orchestrator.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single buffered telemetry row. Heterogeneous by design: location-only rows (inserted by
 * [com.orchestrator.app.service.LocationTrackingService] per GPS fix) carry null battery/steps;
 * the battery/steps row (inserted by the flush worker) carries null lat/lng. Every field is
 * nullable so both shapes are valid.
 */
@Entity(tableName = "telemetry_samples")
data class TelemetrySample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyM: Double? = null,
    val fixTimestamp: String? = null,
    val batteryPercent: Int? = null,
    val isCharging: Boolean? = null,
    val batteryTempC: Double? = null,
    val batteryHealth: String? = null,
    val stepCount: Int? = null,
    val batteryCurrentUa: Int? = null,
    val batteryVoltageMv: Int? = null,
    val chargeSource: String? = null
)
