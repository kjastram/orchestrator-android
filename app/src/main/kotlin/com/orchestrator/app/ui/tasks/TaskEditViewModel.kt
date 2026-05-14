package com.orchestrator.app.ui.tasks

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orchestrator.app.data.model.TaskUpdate
import com.orchestrator.app.data.repository.TaskRepository
import com.orchestrator.app.nav.Screen
import com.orchestrator.app.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskEditUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: String? = null,
    val notifyEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val titleError: Boolean = false
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle[Screen.TaskEdit.ARG_TASK_ID] ?: Screen.TaskEdit.NEW
    val isNewTask: Boolean get() = taskId == Screen.TaskEdit.NEW

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    init {
        if (!isNewTask) {
            loadTask()
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getTasks().fold(
                onSuccess = { tasks ->
                    val task = tasks.find { it.id == taskId }
                    if (task != null) {
                        _uiState.update {
                            it.copy(
                                title = task.title,
                                description = task.description ?: "",
                                dueDate = task.dueDate,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Task not found") }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun onTitleChange(v: String) {
        _uiState.update { it.copy(title = v, titleError = false, error = null) }
    }

    fun onDescriptionChange(v: String) {
        _uiState.update { it.copy(description = v) }
    }

    fun onDueDateChange(date: String?) {
        _uiState.update { it.copy(dueDate = date, notifyEnabled = if (date == null) false else it.notifyEnabled) }
    }

    fun onNotifyToggle() {
        _uiState.update { it.copy(notifyEnabled = !it.notifyEnabled) }
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val result = if (isNewTask) {
                taskRepository.createTask(
                    title = state.title.trim(),
                    description = state.description.takeIf { it.isNotBlank() },
                    dueDate = state.dueDate
                )
            } else {
                taskRepository.updateTask(
                    id = taskId,
                    update = TaskUpdate(
                        title = state.title.trim(),
                        description = state.description.takeIf { it.isNotBlank() },
                        due_date = state.dueDate
                    )
                )
            }

            result.fold(
                onSuccess = { task ->
                    if (state.notifyEnabled && state.dueDate != null) {
                        NotificationScheduler.scheduleReminder(
                            context = application,
                            taskId = task.id,
                            taskTitle = task.title,
                            dueDate = state.dueDate
                        )
                    }
                    _uiState.update { it.copy(isSaving = false) }
                    onDone()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun deleteAndGoBack(onDone: () -> Unit) {
        if (isNewTask) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            taskRepository.deleteTask(taskId).fold(
                onSuccess = {
                    NotificationScheduler.cancelReminder(application, taskId)
                    _uiState.update { it.copy(isSaving = false) }
                    onDone()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }
}
