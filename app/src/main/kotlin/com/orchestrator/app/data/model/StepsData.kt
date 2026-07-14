package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Daily step total POSTed to `api/steps`. [recordedAt] is optional (server-set when omitted);
 * Gson drops null fields so leaving it null yields a `{local_date, steps_today}` body.
 */
data class StepsData(
    @SerializedName("local_date")
    val localDate: String,
    @SerializedName("steps_today")
    val stepsToday: Int,
    @SerializedName("recorded_at")
    val recordedAt: String? = null
)
