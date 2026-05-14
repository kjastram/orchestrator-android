package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskCreate
import com.orchestrator.app.data.model.TaskUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getTasks(): Result<List<Task>> {
        return try {
            val tasks = api.getTasks()
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(title: String, description: String?, dueDate: String?): Result<Task> {
        return try {
            val task = api.createTask(TaskCreate(title = title, description = description, due_date = dueDate))
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
}
