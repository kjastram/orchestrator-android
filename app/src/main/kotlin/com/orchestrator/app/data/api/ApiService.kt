package com.orchestrator.app.data.api

import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.CategoryCreate
import com.orchestrator.app.data.model.CategoryUpdate
import com.orchestrator.app.data.model.LoginRequest
import com.orchestrator.app.data.model.LoginResponse
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskCreate
import com.orchestrator.app.data.model.TaskReorderItem
import com.orchestrator.app.data.model.TaskUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("api/tasks")
    suspend fun getTasks(
        @Query("status") status: String? = null,
        @Query("category_id") categoryId: String? = null
    ): List<Task>

    @POST("api/tasks")
    suspend fun createTask(@Body task: TaskCreate): Task

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body update: TaskUpdate): Task

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @POST("api/tasks/reorder")
    suspend fun reorderTasks(@Body items: List<TaskReorderItem>): List<Task>

    @GET("api/task-categories")
    suspend fun getCategories(): List<Category>

    @POST("api/task-categories")
    suspend fun createCategory(@Body body: CategoryCreate): Category

    @PATCH("api/task-categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryUpdate): Category

    @DELETE("api/task-categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>
}
