package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.DeviceTelemetryData
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of a batch upload, surfacing the HTTP status **class** so the flush worker can
 * branch retryable-vs-poison exhaustively.
 */
sealed interface BatchResult {
    /** 2xx — rows accepted, safe to delete by id. */
    data object Sent : BatchResult
    /** 401 — logged out; keep rows, don't spin. */
    data object Unauthorized : BatchResult
    /** Network/IO or 5xx — transient; keep rows and retry. */
    data class Retryable(val code: Int?) : BatchResult
    /** Other 4xx (e.g. 422 malformed sample) — non-retryable; drop the batch. */
    data class Poison(val code: Int) : BatchResult
}

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

    suspend fun sendBatch(batch: List<DeviceTelemetryData>): BatchResult {
        return try {
            val response = api.sendTelemetryBatch(batch)
            val code = response.code()
            when {
                response.isSuccessful -> BatchResult.Sent
                code == 401 -> BatchResult.Unauthorized
                code >= 500 -> BatchResult.Retryable(code)
                code in 400..499 -> BatchResult.Poison(code)
                else -> BatchResult.Retryable(code)
            }
        } catch (e: IOException) {
            BatchResult.Retryable(null)
        }
    }
}
