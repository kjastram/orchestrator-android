package com.orchestrator.app.data.api

import com.orchestrator.app.data.model.Account
import com.orchestrator.app.data.model.Budget
import com.orchestrator.app.data.model.BudgetUpdateRequest
import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.CategoryCreate
import com.orchestrator.app.data.model.CategoryRule
import com.orchestrator.app.data.model.CategoryUpdate
import com.orchestrator.app.data.model.CategoryUpdateRequest
import com.orchestrator.app.data.model.LoginRequest
import com.orchestrator.app.data.model.LoginResponse
import com.orchestrator.app.data.model.NetWorth
import com.orchestrator.app.data.model.RecurringCharge
import com.orchestrator.app.data.model.RollingAverage
import com.orchestrator.app.data.model.RuleRequest
import com.orchestrator.app.data.model.SyncResult
import com.orchestrator.app.data.model.SyncStatus
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskCreate
import com.orchestrator.app.data.model.TaskMove
import com.orchestrator.app.data.model.TaskReorderItem
import com.orchestrator.app.data.model.TaskUpdate
import com.orchestrator.app.data.model.Transaction
import com.orchestrator.app.data.model.TransactionPage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @POST("api/tasks/{id}/move")
    suspend fun moveTask(@Path("id") id: String, @Body body: TaskMove): Task

    @GET("api/task-categories")
    suspend fun getCategories(): List<Category>

    @POST("api/task-categories")
    suspend fun createCategory(@Body body: CategoryCreate): Category

    @PATCH("api/task-categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryUpdate): Category

    @DELETE("api/task-categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    // ---- Finance ----

    @GET("api/accounts")
    suspend fun getAccounts(): List<Account>

    @GET("api/accounts/net-worth")
    suspend fun getNetWorth(): NetWorth

    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("account_id") accountId: String? = null,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("sort_by") sortBy: String = "date",
        @Query("sort_dir") sortDir: String = "desc"
    ): TransactionPage

    @GET("api/transactions/recurring")
    suspend fun getRecurring(): List<RecurringCharge>

    @PATCH("api/transactions/{id}")
    suspend fun updateTransactionCategory(
        @Path("id") id: String,
        @Body body: CategoryUpdateRequest
    ): Transaction

    @GET("api/budgets")
    suspend fun getBudgets(): List<Budget>

    @GET("api/budgets/rolling-averages")
    suspend fun getRollingAverages(): List<RollingAverage>

    @PUT("api/budgets/{category}")
    suspend fun upsertBudget(
        @Path("category") category: String,
        @Body body: BudgetUpdateRequest
    ): Budget

    @GET("api/rules")
    suspend fun getRules(): List<CategoryRule>

    @POST("api/rules")
    suspend fun createRule(@Body body: RuleRequest): CategoryRule

    @POST("api/plaid/sync")
    suspend fun syncTransactions(): SyncResult

    @GET("api/plaid/sync-status")
    suspend fun getSyncStatus(): SyncStatus
}
