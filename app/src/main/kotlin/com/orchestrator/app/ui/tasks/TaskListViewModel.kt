package com.orchestrator.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.data.model.TaskUpdate
import com.orchestrator.app.data.repository.AuthRepository
import com.orchestrator.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
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
            taskRepository.getTasks().fold(
                onSuccess = { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            taskRepository.getTasks().fold(
                onSuccess = { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isRefreshing = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRefreshing = false, error = e.message) }
                }
            )
        }
    }

    fun toggleDone(task: Task) {
        val newStatus = if (task.status == "done") "todo" else "done"
        viewModelScope.launch {
            taskRepository.updateTask(task.id, TaskUpdate(status = newStatus)).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(tasks = state.tasks.map { if (it.id == updated.id) updated else it })
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(tasks = state.tasks.filter { it.id != task.id })
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun logout(onLogout: () -> Unit) {
        authRepository.logout()
        onLogout()
    }
}
