package com.orchestrator.app.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.orchestrator.app.R
import com.orchestrator.app.data.model.Category
import com.orchestrator.app.data.model.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Google Tasks brand colors
val GoogleBlue = Color(0xFF1A73E8)
val GoogleRed = Color(0xFFD93025)
val GoogleOrange = Color(0xFFF29900)
val InactiveGray = Color(0xFF757575)

@Composable
fun StatusCircle(status: String, modifier: Modifier = Modifier) {
    val sizeDp = 24.dp
    Canvas(modifier = modifier.size(sizeDp)) {
        val radius = size.minDimension / 2f
        val strokeWidth = with(this) { 2.dp.toPx() }
        when (status) {
            "todo" -> {
                drawCircle(
                    color = InactiveGray,
                    radius = radius - strokeWidth / 2,
                    style = Stroke(width = strokeWidth)
                )
            }
            "in_progress" -> {
                drawCircle(
                    color = GoogleBlue,
                    radius = radius - strokeWidth / 2,
                    style = Stroke(width = strokeWidth)
                )
                val path = Path().apply {
                    moveTo(size.width / 2f, 0f + strokeWidth)
                    lineTo(size.width / 2f, size.height - strokeWidth)
                    lineTo(0f + strokeWidth, size.height - strokeWidth)
                    lineTo(0f + strokeWidth, 0f + strokeWidth)
                    close()
                }
                drawPath(path = path, color = GoogleBlue, style = Fill)
            }
            "done" -> {
                drawCircle(color = GoogleBlue, radius = radius, style = Fill)
                // Checkmark
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.67f)
                    lineTo(size.width * 0.75f, size.height * 0.33f)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round)
                )
            }
            else -> {
                drawCircle(
                    color = InactiveGray,
                    radius = radius - strokeWidth / 2,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
fun DueDateLabel(dueDate: String?) {
    if (dueDate == null) return
    val date = try { LocalDate.parse(dueDate) } catch (e: Exception) { return }
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val (label, color) = when {
        date.isBefore(today) -> {
            val days = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
            val text = if (days == 1) "Yesterday" else "$days days ago"
            text to GoogleRed
        }
        date == today -> "Today" to GoogleOrange
        date == tomorrow -> "Tomorrow" to InactiveGray
        date.isBefore(today.plusWeeks(2)) -> {
            date.format(DateTimeFormatter.ofPattern("EEE, MMM d")) to InactiveGray
        }
        else -> date.format(DateTimeFormatter.ofPattern("MMM d")) to InactiveGray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onEditTask: (Task) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    var addCategoryDialogOpen by remember { mutableStateOf(false) }
    var renameCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<Category?>(null) }
    var showNewTaskSheet by remember { mutableStateOf(false) }
    val newTaskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) viewModel.refresh()
    }
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) pullToRefreshState.endRefresh()
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
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.addCategory(nameInput.trim())
                        addCategoryDialogOpen = false
                    }
                }) { Text(stringResource(R.string.save)) }
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
                TextButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.renameCategory(cat.id, nameInput.trim())
                        renameCategoryTarget = null
                    }
                }) { Text(stringResource(R.string.save)) }
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
                TextButton(onClick = {
                    viewModel.deleteCategory(cat.id)
                    deleteCategoryTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // New task bottom sheet
    if (showNewTaskSheet) {
        NewTaskBottomSheet(
            sheetState = newTaskSheetState,
            onDismiss = { showNewTaskSheet = false },
            onSave = { title ->
                viewModel.createQuickTask(title, uiState.selectedCategoryId)
                showNewTaskSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tasks),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    // Account avatar circle
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF29900))
                            .clickable { viewModel.logout(onLogout) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskSheet = true },
                shape = MaterialTheme.shapes.large,
                containerColor = GoogleBlue,
                contentColor = Color.White
            ) {
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
            // Category tab row
            val categories = uiState.categories
            val selectedCategoryId = uiState.selectedCategoryId
            val selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)

            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GoogleBlue,
                    edgePadding = 0.dp
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { viewModel.selectCategory(category.id) },
                            text = {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index == selectedTabIndex) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = GoogleBlue,
                            unselectedContentColor = InactiveGray
                        )
                    }
                    // "+ New list" button as last tab item
                    Tab(
                        selected = false,
                        onClick = { addCategoryDialogOpen = true },
                        text = {
                            Text(
                                text = "+ New list",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoogleBlue
                            )
                        }
                    )
                }

                // Section header for selected category
                val selectedCategory = categories.find { it.id == selectedCategoryId }
                if (selectedCategory != null) {
                    CategorySectionHeader(
                        category = selectedCategory,
                        onRename = { renameCategoryTarget = selectedCategory },
                        onDelete = { deleteCategoryTarget = selectedCategory }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        val selectedTasks = uiState.tasksByCategory[selectedCategoryId] ?: emptyList()
                        val localTasks = remember(selectedTasks) { selectedTasks.toMutableStateList() }
                        var draggingTaskId by remember { mutableStateOf<String?>(null) }
                        // Accumulated horizontal drag of the row being dragged. A sustained
                        // rightward drag past the threshold means "nest under the task above".
                        var dragAccumX by remember { mutableStateOf(0f) }
                        val isDragging = draggingTaskId != null
                        val haptic = LocalHapticFeedback.current
                        val density = LocalDensity.current
                        val nestThresholdPx = with(density) { 48.dp.toPx() }

                        LaunchedEffect(selectedTasks) {
                            if (!isDragging) {
                                localTasks.clear()
                                localTasks.addAll(selectedTasks)
                            }
                        }

                        if (localTasks.isEmpty() && categories.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_tasks),
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(localTasks, key = { "task_${it.id}" }) { task ->
                                    val isDraggingThis = task.id == draggingTaskId
                                    // Live preview: only top-level tasks without their own
                                    // subtasks can be nested (backend allows 1 level only).
                                    val nestPreview = isDraggingThis &&
                                        dragAccumX > nestThresholdPx &&
                                        task.subtasks.isEmpty() &&
                                        localTasks.indexOfFirst { it.id == task.id } > 0
                                    SwipeToDismissTaskRow(
                                        task = task,
                                        snackbarHostState = snackbarHostState,
                                        onDelete = { viewModel.deleteTask(task) },
                                        onEditTask = onEditTask,
                                        onMarkComplete = { viewModel.markComplete(task) },
                                        onCycleSubtaskStatus = { viewModel.cycleSubtaskStatus(it) },
                                        onPromoteSubtask = { viewModel.promoteToTopLevel(it.id) },
                                        scope = scope,
                                        isDragging = isDraggingThis,
                                        nestPreview = nestPreview,
                                        dragModifier = Modifier.pointerInput(task.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggingTaskId = task.id
                                                    dragAccumX = 0f
                                                },
                                                onDrag = { _, dragAmount ->
                                                    dragAccumX += dragAmount.x
                                                    val currentIdx = localTasks.indexOfFirst { it.id == draggingTaskId }
                                                    if (currentIdx < 0) return@detectDragGesturesAfterLongPress
                                                    val itemHeightPx = with(density) { 72.dp.toPx() }
                                                    val shift = (dragAmount.y / itemHeightPx).let {
                                                        if (it > 0.5f) 1 else if (it < -0.5f) -1 else 0
                                                    }
                                                    val targetIdx = (currentIdx + shift).coerceIn(0, localTasks.lastIndex)
                                                    if (targetIdx != currentIdx) {
                                                        val item = localTasks.removeAt(currentIdx)
                                                        localTasks.add(targetIdx, item)
                                                    }
                                                },
                                                onDragEnd = {
                                                    val draggedId = draggingTaskId
                                                    val nest = dragAccumX > nestThresholdPx
                                                    draggingTaskId = null
                                                    dragAccumX = 0f
                                                    val idx = localTasks.indexOfFirst { it.id == draggedId }
                                                    val above = if (idx > 0) localTasks[idx - 1] else null
                                                    val dragged = localTasks.getOrNull(idx)
                                                    if (nest && draggedId != null && above != null &&
                                                        dragged?.subtasks.isNullOrEmpty()
                                                    ) {
                                                        // Nest under the task immediately above.
                                                        viewModel.nestUnder(draggedId, above.id)
                                                    } else {
                                                        viewModel.reorderTasksLocally(selectedCategoryId, localTasks.toList())
                                                    }
                                                },
                                                onDragCancel = {
                                                    draggingTaskId = null
                                                    dragAccumX = 0f
                                                }
                                            )
                                        }
                                    )
                                }

                                item(key = "bottom_spacer") {
                                    Spacer(modifier = Modifier.height(96.dp))
                                }
                            }
                        }

                        // Only draw the indicator while actively pulling or refreshing —
                        // otherwise Material3 leaves its dark container circle parked at rest.
                        if (pullToRefreshState.progress > 0f || pullToRefreshState.isRefreshing) {
                            PullToRefreshContainer(
                                state = pullToRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { /* sort — cosmetic */ }) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                tint = InactiveGray
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = InactiveGray)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rename_category)) },
                    onClick = { menuExpanded = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_category)) },
                    onClick = { menuExpanded = false; onDelete() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTaskBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("New task", color = InactiveGray) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                TextButton(
                    onClick = {
                        if (titleInput.isNotBlank()) onSave(titleInput.trim())
                    },
                    enabled = titleInput.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        color = if (titleInput.isNotBlank()) GoogleBlue else InactiveGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = "Notes",
                    tint = InactiveGray,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Schedule",
                    tint = InactiveGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTaskRow(
    task: Task,
    snackbarHostState: SnackbarHostState,
    onDelete: () -> Unit,
    onEditTask: (Task) -> Unit,
    onMarkComplete: () -> Unit,
    onCycleSubtaskStatus: (Task) -> Unit,
    onPromoteSubtask: (Task) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    isDragging: Boolean,
    nestPreview: Boolean,
    dragModifier: Modifier
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
            } else false
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
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                }
            },
            enableDismissFromStartToEnd = false,
            modifier = dragModifier
                .zIndex(if (isDragging) 1f else 0f)
                .background(
                    if (isDragging) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    else Color.Transparent
                )
        ) {
            TaskFlatRow(
                task = task,
                onEditTask = onEditTask,
                onMarkComplete = onMarkComplete,
                onCycleSubtaskStatus = onCycleSubtaskStatus,
                onPromoteSubtask = onPromoteSubtask,
                nestPreview = nestPreview
            )
        }
    }
}

@Composable
private fun TaskFlatRow(
    task: Task,
    onEditTask: (Task) -> Unit,
    onMarkComplete: () -> Unit,
    onCycleSubtaskStatus: (Task) -> Unit,
    onPromoteSubtask: (Task) -> Unit,
    nestPreview: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Main task row. When a nest is previewed, indent it to hint it will become
        // a subtask of the task above.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable { onEditTask(task) }
                .padding(start = if (nestPreview) 48.dp else 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status circle — tapping cycles status
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onMarkComplete() },
                contentAlignment = Alignment.Center
            ) {
                StatusCircle(status = task.status)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.status == "done") TextDecoration.LineThrough else null,
                    color = if (task.status == "done") InactiveGray else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                DueDateLabel(dueDate = task.dueDate)
            }
        }

        // Subtasks indented below parent
        if (task.subtasks.isNotEmpty()) {
            task.subtasks.forEach { subtask ->
                SubtaskFlatRow(
                    subtask = subtask,
                    onCycleStatus = { onCycleSubtaskStatus(subtask) },
                    onPromote = { onPromoteSubtask(subtask) }
                )
            }
        }
    }
}

@Composable
private fun SubtaskFlatRow(
    subtask: Task,
    onCycleStatus: () -> Unit,
    onPromote: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val promoteThresholdPx = with(density) { 48.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var accumX by remember { mutableStateOf(0f) }
    // Long-press a subtask and drag it left past the threshold to promote it to a
    // top-level task. The leftward outdent is previewed live.
    val willPromote = dragging && accumX < -promoteThresholdPx

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                if (willPromote) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else Color.Transparent
            )
            .padding(start = if (willPromote) 16.dp else 68.dp, end = 16.dp)
            .pointerInput(subtask.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dragging = true
                        accumX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumX += dragAmount.x
                    },
                    onDragEnd = {
                        val promote = accumX < -promoteThresholdPx
                        dragging = false
                        accumX = 0f
                        if (promote) onPromote()
                    },
                    onDragCancel = {
                        dragging = false
                        accumX = 0f
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { onCycleStatus() },
            contentAlignment = Alignment.Center
        ) {
            StatusCircle(status = subtask.status, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.status == "done") TextDecoration.LineThrough else null,
            color = if (subtask.status == "done") InactiveGray else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
