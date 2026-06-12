package com.orchestrator.app.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orchestrator.app.data.model.ALL_CATEGORIES
import com.orchestrator.app.data.model.Transaction
import com.orchestrator.app.ui.finance.components.FinanceColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showFilters by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var categoryEditTarget by remember { mutableStateOf<Transaction?>(null) }
    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchInput by remember { mutableStateOf(uiState.search) }

    // Debounced search -> reload (skip the initial emission; init already loaded).
    LaunchedEffect(Unit) {
        snapshotFlow { searchInput }
            .drop(1)
            .debounce(350)
            .collect { q ->
                viewModel.setSearch(q)
                viewModel.reload()
            }
    }

    // Infinite scroll: load more as we approach the end.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                val loaded = viewModel.uiState.value.items.size
                if (loaded > 0 && lastVisible >= loaded - 5) viewModel.loadMore()
            }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Rule suggestion snackbar after a category edit.
    LaunchedEffect(uiState.ruleSuggestion) {
        val s = uiState.ruleSuggestion ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Always categorize ${s.merchant} as ${s.category}?",
            actionLabel = "Always"
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.confirmRule() else viewModel.dismissRule()
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState
        ) {
            FilterSheetContent(uiState, viewModel)
        }
    }

    categoryEditTarget?.let { tx ->
        ModalBottomSheet(
            onDismissRequest = { categoryEditTarget = null },
            sheetState = categorySheetState
        ) {
            CategoryPickerContent(
                current = tx.category,
                onPick = { cat ->
                    viewModel.editCategory(tx, cat)
                    categoryEditTarget = null
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search transactions") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            SortRow(uiState, viewModel)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.items.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.items.isEmpty() -> {
                        Text(
                            text = "No transactions match your filters.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(uiState.items, key = { it.id }) { tx ->
                                TransactionRow(
                                    transaction = tx,
                                    accountName = uiState.accounts.find { it.id == tx.accountId }?.name,
                                    onEditCategory = { categoryEditTarget = tx }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                            if (uiState.isLoadingMore) {
                                item("loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                            item("bottom_spacer") { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortRow(state: TransactionsUiState, viewModel: TransactionsViewModel) {
    val sorts = listOf("date" to "Date", "amount" to "Amount", "merchant_name" to "Merchant", "category" to "Category")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sorts.forEach { (key, label) ->
            val selected = state.sortBy == key
            val arrow = if (selected) (if (state.sortDir == "asc") " ↑" else " ↓") else ""
            FilterChip(
                selected = selected,
                onClick = { viewModel.setSort(key) },
                label = { Text(label + arrow) }
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    accountName: String?,
    onEditCategory: () -> Unit
) {
    val isIncome = transaction.amount < 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(formatDate(transaction.date))
                    accountName?.let { append(" · "); append(it) }
                    if (transaction.pending) append(" · pending")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(
                onClick = onEditCategory,
                label = { Text(transaction.category ?: "Uncategorized", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(Modifier.height(0.dp))
        Text(
            text = MoneyFormat.signedFlow(transaction.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isIncome) FinanceColors.Positive else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryPickerContent(current: String?, onPick: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Set category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ALL_CATEGORIES.forEach { cat ->
            Surface(
                onClick = { onPick(cat) },
                color = if (cat == current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetContent(state: TransactionsUiState, viewModel: TransactionsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(16.dp))
        Text("Date range", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = LocalDate.now()
            DateRangeChip("All", selected = state.startDate == null) {
                viewModel.setDateRange(null, null)
            }
            DateRangeChip("This month", selected = state.startDate == today.withDayOfMonth(1).toString()) {
                viewModel.setDateRange(today.withDayOfMonth(1).toString(), today.toString())
            }
            DateRangeChip("30 days", selected = state.startDate == today.minusDays(30).toString()) {
                viewModel.setDateRange(today.minusDays(30).toString(), today.toString())
            }
            DateRangeChip("90 days", selected = state.startDate == today.minusDays(90).toString()) {
                viewModel.setDateRange(today.minusDays(90).toString(), today.toString())
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowChips(
            options = listOf("All") + ALL_CATEGORIES,
            selected = state.category ?: "All"
        ) { picked ->
            viewModel.setCategory(if (picked == "All") null else picked)
        }

        if (state.accounts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Account", style = MaterialTheme.typography.labelLarge)
            FlowChips(
                options = listOf("All") + state.accounts.map { it.name },
                selected = state.accounts.find { it.id == state.accountId }?.name ?: "All"
            ) { picked ->
                val id = state.accounts.find { it.name == picked }?.id
                viewModel.setAccount(id)
            }
        }
    }
}

@Composable
private fun DateRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** Simple wrapping chip group (avoids extra layout deps). */
@Composable
private fun FlowChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        options.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { opt ->
                    FilterChip(
                        selected = opt == selected,
                        onClick = { onPick(opt) },
                        label = {
                            Text(
                                opt,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatDate(dateStr: String): String = try {
    LocalDate.parse(dateStr.take(10)).format(DISPLAY_DATE)
} catch (e: Exception) {
    dateStr
}

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
