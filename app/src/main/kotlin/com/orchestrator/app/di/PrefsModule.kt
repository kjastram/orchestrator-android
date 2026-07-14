package com.orchestrator.app.di

import com.orchestrator.app.data.device.StepProvider
import com.orchestrator.app.data.device.StepReader
import com.orchestrator.app.data.repository.StepUploader
import com.orchestrator.app.data.repository.TelemetryRepository
import com.orchestrator.app.data.store.StepPrefs
import com.orchestrator.app.data.store.TelemetryPrefs
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PrefsModule {

    @Binds
    abstract fun bindStepPrefs(impl: TelemetryPrefs): StepPrefs

    @Binds
    abstract fun bindStepUploader(impl: TelemetryRepository): StepUploader

    @Binds
    abstract fun bindStepReader(impl: StepProvider): StepReader
}
