package com.orchestrator.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.CategoryUpdate
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskReorderItem
import com.orchestrator.app.data.model.TaskUpdate
import com.orchestrator.app.data.repository.AuthRepository
import com.orchestrator.app.data.repository.CategoryRepository
import com.orchestrator.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val categories: List<Category> = emptyList(),
    val tasksByCategory: Map<String?, List<Task>> = emptyMap(), // null key = uncategorized
    val statusFilter: String? = null,   // null = all, "todo", "in_progress", "done"
    val selectedCategoryId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loadAll()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            loadAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadAll() {
        val statusFilter = _uiState.value.statusFilter
        val catsDeferred = viewModelScope.async { categoryRepository.getCategories() }
        val tasksDeferred = viewModelScope.async { taskRepository.getTasks(status = statusFilter) }

        val catsResult = catsDeferred.await()
        val tasksResult = tasksDeferred.await()

        if (catsResult.isFailure) {
            _uiState.update { it.copy(error = catsResult.exceptionOrNull()?.message) }
            return
        }
        if (tasksResult.isFailure) {
            _uiState.update { it.copy(error = tasksResult.exceptionOrNull()?.message) }
            return
        }

        val cats = catsResult.getOrDefault(emptyList())
        val tasks = tasksResult.getOrDefault(emptyList())

        val currentSelected = _uiState.value.selectedCategoryId
        val newSelected = if (currentSelected == null || cats.none { it.id == currentSelected }) {
            cats.firstOrNull()?.id
        } else currentSelected

        _uiState.update {
            it.copy(
                categories = cats,
                tasksByCategory = groupTasks(tasks),
                selectedCategoryId = newSelected
            )
        }
    }

    private fun groupTasks(tasks: List<Task>): Map<String?, List<Task>> {
        // Only top-level tasks (parentId == null); subtasks are nested inside Task.subtasks
        return tasks
            .filter { it.parentId == null }
            .sortedBy { it.position }
            .groupBy { it.categoryId }
    }

    fun selectCategory(id: String?) {
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(statusFilter = status) }
        viewModelScope.launch {
            val result = taskRepository.getTasks(status = status)
            result.onSuccess { tasks ->
                _uiState.update { it.copy(tasksByCategory = groupTasks(tasks)) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun cycleStatus(task: Task) {
        val next = when (task.status) {
            "todo" -> "in_progress"
            "in_progress" -> "done"
            else -> "todo"
        }
        viewModelScope.launch {
            taskRepository.updateTask(task.id, TaskUpdate(status = next)).fold(
                onSuccess = { reloadTasks() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id).fold(
                onSuccess = { reloadTasks() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.createCategory(name).fold(
                onSuccess = { cat ->
                    _uiState.update { state ->
                        state.copy(
                            categories = state.categories + cat,
                            tasksByCategory = state.tasksByCategory + (cat.id to emptyList())
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun renameCategory(id: String, name: String) {
        viewModelScope.launch {
            categoryRepository.updateCategory(id, CategoryUpdate(name = name)).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(categories = state.categories.map { if (it.id == id) updated else it })
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val newCategories = state.categories.filter { it.id != id }
                        val newSelected = if (state.selectedCategoryId == id) {
                            newCategories.firstOrNull()?.id
                        } else state.selectedCategoryId
                        state.copy(
                            categories = newCategories,
                            tasksByCategory = state.tasksByCategory.filterKeys { it != id },
                            selectedCategoryId = newSelected
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun addSubtask(parentId: String, title: String) {
        viewModelScope.launch {
            // Find parent to inherit its categoryId
            val parent = _uiState.value.tasksByCategory.values.flatten().find { it.id == parentId }
            taskRepository.createTask(
                title = title,
                description = null,
                dueDate = null,
                categoryId = parent?.categoryId,
                parentId = parentId
            ).fold(
                onSuccess = { reloadTasks() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun cycleSubtaskStatus(subtask: Task) = cycleStatus(subtask)

    fun deleteSubtask(subtask: Task) = deleteTask(subtask)

    fun reorderTasksLocally(categoryId: String?, newOrder: List<Task>) {
        _uiState.update { state ->
            state.copy(
                tasksByCategory = state.tasksByCategory.toMutableMap().apply {
                    this[categoryId] = newOrder
                }
            )
        }
        viewModelScope.launch {
            val items = newOrder.mapIndexed { index, task ->
                TaskReorderItem(id = task.id, position = index, categoryId = categoryId)
            }
            taskRepository.reorderTasks(items).onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createQuickTask(title: String, categoryId: String?) {
        viewModelScope.launch {
            taskRepository.createTask(title = title, description = null, dueDate = null, categoryId = categoryId, parentId = null)
                .fold(
                    onSuccess = { reloadTasks() },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
                )
        }
    }

    fun markComplete(task: Task) {
        val newStatus = if (task.status == "done") "todo" else "done"
        viewModelScope.launch {
            taskRepository.updateTask(task.id, TaskUpdate(status = newStatus)).fold(
                onSuccess = { reloadTasks() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun logout(onLogout: () -> Unit) {
        authRepository.logout()
        onLogout()
    }

    private suspend fun reloadTasks() {
        val result = taskRepository.getTasks(status = _uiState.value.statusFilter)
        result.onSuccess { tasks ->
            _uiState.update { it.copy(tasksByCategory = groupTasks(tasks)) }
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
        }
    }
}
