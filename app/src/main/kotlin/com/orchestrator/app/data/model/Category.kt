package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("position") val position: Int,
    @SerializedName("task_count") val taskCount: Int,
    @SerializedName("created_at") val createdAt: String
)

data class CategoryCreate(
    @SerializedName("name") val name: String
)

data class CategoryUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("position") val position: Int? = null
)
