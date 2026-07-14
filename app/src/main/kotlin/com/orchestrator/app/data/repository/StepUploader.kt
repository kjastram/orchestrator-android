package com.orchestrator.app.data.repository

import com.orchestrator.app.data.model.StepsData

/**
 * Narrow upload surface for daily step totals. Extracted from [TelemetryRepository] so
 * [com.orchestrator.app.data.device.DailyStepTracker] can be unit-tested with a fake that
 * records — or throws on — uploads. Implementations throw on non-2xx / IO failure; callers
 * wrap best-effort sends in try/catch.
 */
interface StepUploader {
    suspend fun sendSteps(body: StepsData)
}
