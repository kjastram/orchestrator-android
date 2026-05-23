package com.orchestrator.app.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orchestrator.app.R
import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Screen-level dialog state
    var addCategoryDialogOpen by remember { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Add Category dialog
    if (addCategoryDialogOpen) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addCategoryDialogOpen = false },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.add_category_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.addCategory(nameInput.trim())
                            addCategoryDialogOpen = false
                        }
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { addCategoryDialogOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rename Category dialog
    renameCategoryTarget?.let { cat ->
        var nameInput by remember(cat.id) { mutableStateOf(cat.name) }
        AlertDialog(
            onDismissRequest = { renameCategoryTarget = null },
            title = { Text(stringResource(R.string.rename_category_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.rename_category)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.renameCategory(cat.id, nameInput.trim())
                            renameCategoryTarget = null
                        }
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameCategoryTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Category confirmation dialog
    deleteCategoryTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = { Text(stringResource(R.string.confirm_delete_category)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(cat.id)
                        deleteCategoryTarget = null
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasks)) },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = stringResource(R.string.logout)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StatusFilterRow(
                statusFilter = uiState.statusFilter,
                onFilterSelected = { viewModel.setStatusFilter(it) }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.tasksByCategory.isEmpty() && uiState.categories.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.no_tasks),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Render named categories in order
                            uiState.categories.forEach { category ->
                                val tasksForCategory = uiState.tasksByCategory[category.id] ?: emptyList()

                                item(key = "header_${category.id}") {
                                    CategoryHeader(
                                        category = category,
                                        taskCount = tasksForCategory.size,
                                        onRename = { renameCategoryTarget = category },
                                        onDelete = { deleteCategoryTarget = category }
                                    )
                                }

                                items(
                                    items = tasksForCategory,
                                    key = { "task_${it.id}" }
                                ) { task ->
                                    SwipeToDismissTaskRow(
                                        task = task,
                                        snackbarHostState = snackbarHostState,
                                        onDelete = { viewModel.deleteTask(task) },
                                        onEditTask = onEditTask,
                                        onCycleStatus = { viewModel.cycleStatus(task) },
                                        onCycleSubtaskStatus = { viewModel.cycleSubtaskStatus(it) },
                                        onDeleteSubtask = { viewModel.deleteSubtask(it) },
                                        onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                                        scope = scope
                                    )
                                }
                            }

                            // Uncategorized section
                            val uncategorized = uiState.tasksByCategory[null] ?: emptyList()
                            if (uncategorized.isNotEmpty()) {
                                item(key = "header_uncategorized") {
                                    UncategorizedHeader(taskCount = uncategorized.size)
                                }

                                items(
                                    items = uncategorized,
                                    key = { "task_${it.id}" }
                                ) { task ->
                                    SwipeToDismissTaskRow(
                                        task = task,
                                        snackbarHostState = snackbarHostState,
                                        onDelete = { viewModel.deleteTask(task) },
                                        onEditTask = onEditTask,
                                        onCycleStatus = { viewModel.cycleStatus(task) },
                                        onCycleSubtaskStatus = { viewModel.cycleSubtaskStatus(it) },
                                        onDeleteSubtask = { viewModel.deleteSubtask(it) },
                                        onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                                        scope = scope
                                    )
                                }
                            }

                            item(key = "add_category_row") {
                                AddCategoryRow(onClick = { addCategoryDialogOpen = true })
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    statusFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    val filters = listOf(
        null to stringResource(R.string.filter_all),
        "todo" to stringResource(R.string.status_todo),
        "in_progress" to stringResource(R.string.status_in_progress),
        "done" to stringResource(R.string.status_done)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (value, label) ->
            FilterChip(
                selected = statusFilter == value,
                onClick = { onFilterSelected(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryHeader(
    category: Category,
    taskCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = taskCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename_category)) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_category)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun UncategorizedHeader(taskCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.uncategorized),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = taskCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddCategoryRow(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(stringResource(R.string.add_category))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTaskRow(
    task: Task,
    snackbarHostState: SnackbarHostState,
    onDelete: () -> Unit,
    onEditTask: (Task) -> Unit,
    onCycleStatus: () -> Unit,
    onCycleSubtaskStatus: (Task) -> Unit,
    onDeleteSubtask: (Task) -> Unit,
    onAddSubtask: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var dismissed by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                dismissed = true
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Task deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        dismissed = false
                    } else {
                        onDelete()
                    }
                }
                true
            } else {
                false
            }
        }
    )

    if (!dismissed) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val bgColor by animateColorAsState(
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                        Color(0xFFB71C1C) else Color.Transparent,
                    label = "swipe_bg"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            TaskRowWithSubtasks(
                task = task,
                onEditTask = onEditTask,
                onCycleStatus = onCycleStatus,
                onCycleSubtaskStatus = onCycleSubtaskStatus,
                onDeleteSubtask = onDeleteSubtask,
                onAddSubtask = onAddSubtask
            )
        }
    }
}

@Composable
private fun TaskRowWithSubtasks(
    task: Task,
    onEditTask: (Task) -> Unit,
    onCycleStatus: () -> Unit,
    onCycleSubtaskStatus: (Task) -> Unit,
    onDeleteSubtask: (Task) -> Unit,
    onAddSubtask: (String) -> Unit
) {
    var subtasksExpanded by remember { mutableStateOf(true) }

    Column {
        TaskCard(
            task = task,
            onEditTask = onEditTask,
            onCycleStatus = onCycleStatus,
            onAddSubtask = onAddSubtask
        )

        if (task.subtasks.isNotEmpty()) {
            val count = task.subtasks.size
            val chevron = if (subtasksExpanded) "▾" else "▸"
            TextButton(
                onClick = { subtasksExpanded = !subtasksExpanded },
                modifier = Modifier.padding(start = 32.dp)
            ) {
                Text("$chevron $count subtask${if (count == 1) "" else "s"}")
            }

            if (subtasksExpanded) {
                task.subtasks.forEach { subtask ->
                    SubtaskRow(
                        subtask = subtask,
                        onCycleStatus = { onCycleSubtaskStatus(subtask) },
                        onDelete = { onDeleteSubtask(subtask) }
                    )
                }
            }
        }

        // "Add subtask" row always visible below subtasks
        var addSubtaskDialogOpen by remember { mutableStateOf(false) }
        var subtaskTitleInput by remember { mutableStateOf("") }

        if (addSubtaskDialogOpen) {
            AlertDialog(
                onDismissRequest = {
                    addSubtaskDialogOpen = false
                    subtaskTitleInput = ""
                },
                title = { Text(stringResource(R.string.add_subtask)) },
                text = {
                    OutlinedTextField(
                        value = subtaskTitleInput,
                        onValueChange = { subtaskTitleInput = it },
                        label = { Text(stringResource(R.string.subtask_hint)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (subtaskTitleInput.isNotBlank()) {
                                onAddSubtask(subtaskTitleInput.trim())
                                addSubtaskDialogOpen = false
                                subtaskTitleInput = ""
                            }
                        }
                    ) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            addSubtaskDialogOpen = false
                            subtaskTitleInput = ""
                        }
                    ) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        TextButton(
            onClick = { addSubtaskDialogOpen = true },
            modifier = Modifier.padding(start = 32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                stringResource(R.string.add_subtask),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: Task,
    onEditTask: (Task) -> Unit,
    onCycleStatus: () -> Unit,
    onAddSubtask: (String) -> Unit
) {
    val today = LocalDate.now()
    val dueDate = task.dueDate?.let {
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    }

    val urgencyColor = when {
        dueDate != null && dueDate.isBefore(today) && task.status != "done" -> Color(0xFFB71C1C)
        dueDate != null && dueDate == today && task.status != "done" -> Color(0xFFF9A825)
        else -> Color.Transparent
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var addSubtaskDialogOpen by remember { mutableStateOf(false) }
    var subtaskTitleInput by remember { mutableStateOf("") }

    if (addSubtaskDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                addSubtaskDialogOpen = false
                subtaskTitleInput = ""
            },
            title = { Text(stringResource(R.string.add_subtask)) },
            text = {
                OutlinedTextField(
                    value = subtaskTitleInput,
                    onValueChange = { subtaskTitleInput = it },
                    label = { Text(stringResource(R.string.subtask_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (subtaskTitleInput.isNotBlank()) {
                            onAddSubtask(subtaskTitleInput.trim())
                            addSubtaskDialogOpen = false
                            subtaskTitleInput = ""
                        }
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addSubtaskDialogOpen = false
                        subtaskTitleInput = ""
                    }
                ) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTask(task) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left urgency color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(urgencyColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            ) {
                // Status chip + title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val (chipContainerColor, chipLabelColor) = when (task.status) {
                        "in_progress" -> Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        "done" -> Pair(
                            Color(0xFF4CAF50).copy(alpha = 0.2f),
                            Color(0xFF388E3C)
                        )
                        else -> Pair(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SuggestionChip(
                        onClick = onCycleStatus,
                        label = {
                            Text(
                                text = when (task.status) {
                                    "todo" -> stringResource(R.string.status_todo)
                                    "in_progress" -> stringResource(R.string.status_in_progress)
                                    "done" -> stringResource(R.string.status_done)
                                    else -> task.status
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = chipContainerColor,
                            labelColor = chipLabelColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.status == "done") TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                dueDate?.let { date ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val formatted = date.format(DateTimeFormatter.ofPattern("MMM d"))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(formatted, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_subtask)) },
                        onClick = {
                            menuExpanded = false
                            addSubtaskDialogOpen = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: Task,
    onCycleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (chipContainerColor, chipLabelColor) = when (subtask.status) {
            "in_progress" -> Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            "done" -> Pair(
                Color(0xFF4CAF50).copy(alpha = 0.2f),
                Color(0xFF388E3C)
            )
            else -> Pair(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SuggestionChip(
            onClick = onCycleStatus,
            label = {
                Text(
                    text = when (subtask.status) {
                        "todo" -> stringResource(R.string.status_todo)
                        "in_progress" -> stringResource(R.string.status_in_progress)
                        "done" -> stringResource(R.string.status_done)
                        else -> subtask.status
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = chipContainerColor,
                labelColor = chipLabelColor
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.status == "done") TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
