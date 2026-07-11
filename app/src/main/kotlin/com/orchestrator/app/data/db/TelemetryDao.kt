package com.orchestrator.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TelemetryDao {

    @Insert
    suspend fun insert(sample: TelemetrySample)

    @Query("SELECT * FROM telemetry_samples")
    suspend fun getAll(): List<TelemetrySample>

    @Query("DELETE FROM telemetry_samples WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
