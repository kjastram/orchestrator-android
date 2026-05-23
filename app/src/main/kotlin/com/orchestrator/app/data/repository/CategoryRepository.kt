package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.CategoryCreate
import com.orchestrator.app.data.model.CategoryUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getCategories(): Result<List<Category>> {
        return try { Result.success(api.getCategories()) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createCategory(name: String): Result<Category> {
        return try {
            Result.success(api.createCategory(CategoryCreate(name)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateCategory(id: String, update: CategoryUpdate): Result<Category> {
        return try { Result.success(api.updateCategory(id, update)) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteCategory(id: String): Result<Unit> {
        return try {
            val response = api.deleteCategory(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete failed: ${response.code()}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
