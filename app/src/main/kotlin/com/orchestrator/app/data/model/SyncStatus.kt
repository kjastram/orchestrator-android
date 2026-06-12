package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Last Plaid sync state, persisted on the backend and shared across all clients
 * (web + Android). `lastRefreshedAt` only advances on a successful sync; on a
 * failed sync the backend leaves it untouched and sets [lastSyncError] true.
 */
data class SyncStatus(
    @SerializedName("last_refreshed_at")
    val lastRefreshedAt: String?,
    @SerializedName("last_sync_error")
    val lastSyncError: Boolean = false,
    @SerializedName("error_message")
    val errorMessage: String? = null
)
