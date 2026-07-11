package com.orchestrator.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TelemetrySample::class], version = 1, exportSchema = false)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
