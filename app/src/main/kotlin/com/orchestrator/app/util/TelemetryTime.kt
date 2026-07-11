package com.orchestrator.app.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Shared ISO-8601 offset formatter for telemetry timestamps. Output is byte-identical to
 * the original `isoFromEpochMillis` that lived inside `TelemetryWorker`
 * (`DateTimeFormatter.ISO_OFFSET_DATE_TIME` at UTC). Both the service (writer) and the
 * flush worker (reader/passthrough) rely on this exact format so a stored fix round-trips
 * through Pydantic without a 422.
 */
fun isoFromEpochMillis(millis: Long): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))
