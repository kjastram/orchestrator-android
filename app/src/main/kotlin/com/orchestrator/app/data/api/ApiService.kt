package com.orchestrator.app.data.api

import com.orchestrator.app.data.model.LoginRequest
import com.orchestrator.app.data.model.LoginResponse
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskCreate
import com.orchestrator.app.data.model.TaskUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("api/tasks")
    suspend fun getTasks(): List<Task>

    @POST("api/tasks")
    suspend fun createTask(@Body task: TaskCreate): Task

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body update: TaskUpdate): Task

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>
}
