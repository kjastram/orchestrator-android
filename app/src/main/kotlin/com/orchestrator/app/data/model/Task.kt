package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

data class Task(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("status")
    val status: String, // "todo", "in_progress", "done"
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("due_date")
    val dueDate: String?
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val access_token: String,
    @SerializedName("token_type")
    val token_type: String
)

data class TaskCreate(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("due_date")
    val due_date: String?
)

data class TaskUpdate(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("due_date")
    val due_date: String? = null
)
