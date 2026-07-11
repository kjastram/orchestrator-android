package com.orchestrator.app.di

import android.content.Context
import androidx.room.Room
import com.orchestrator.app.data.db.TelemetryDao
import com.orchestrator.app.data.db.TelemetryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides
    @Singleton
    fun provideTelemetryDatabase(@ApplicationContext context: Context): TelemetryDatabase {
        return Room.databaseBuilder(
            context,
            TelemetryDatabase::class.java,
            "telemetry.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTelemetryDao(db: TelemetryDatabase): TelemetryDao = db.telemetryDao()
}
