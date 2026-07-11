package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.DeviceTelemetryData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun send(data: DeviceTelemetryData): Result<Unit> {
        return try {
            val response = api.sendTelemetry(data)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Telemetry POST failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
