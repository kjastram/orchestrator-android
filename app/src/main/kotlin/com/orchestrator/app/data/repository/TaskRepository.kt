package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskCreate
import com.orchestrator.app.data.model.TaskReorderItem
import com.orchestrator.app.data.model.TaskUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getTasks(status: String? = null, categoryId: String? = null): Result<List<Task>> {
        return try {
            val tasks = api.getTasks(status, categoryId)
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(
        title: String,
        description: String?,
        dueDate: String?,
        categoryId: String? = null,
        parentId: String? = null
    ): Result<Task> {
        return try {
            val task = api.createTask(
                TaskCreate(
                    title = title,
                    description = description,
                    due_date = dueDate,
                    category_id = categoryId,
                    parent_id = parentId
                )
            )
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(id: String, update: TaskUpdate): Result<Task> {
        return try {
            val task = api.updateTask(id, update)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            val response = api.deleteTask(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reorderTasks(items: List<TaskReorderItem>): Result<List<Task>> {
        return try {
            Result.success(api.reorderTasks(items))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
