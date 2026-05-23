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
    val dueDate: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("parent_id")
    val parentId: String?,
    @SerializedName("position")
    val position: Int,
    @SerializedName("subtasks")
    val subtasks: List<Task> = emptyList()
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
    val due_date: String?,
    @SerializedName("category_id")
    val category_id: String? = null,
    @SerializedName("parent_id")
    val parent_id: String? = null,
    @SerializedName("position")
    val position: Int? = null
)

data class TaskUpdate(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("due_date")
    val due_date: String? = null,
    @SerializedName("category_id")
    val category_id: String? = null,
    @SerializedName("parent_id")
    val parent_id: String? = null
)

data class TaskReorderItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("position")
    val position: Int,
    @SerializedName("category_id")
    val categoryId: String? = null
)
