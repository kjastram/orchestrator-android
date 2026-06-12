package com.orchestrator.app.ui.finance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orchestrator.app.data.model.RecurringCharge
import com.orchestrator.app.ui.finance.components.ChartSeries
import com.orchestrator.app.ui.finance.components.FinanceColors
import com.orchestrator.app.ui.finance.components.GroupedBarChart
import com.orchestrator.app.ui.finance.components.KpiCard
import com.orchestrator.app.ui.finance.components.Pill
import com.orchestrator.app.ui.finance.components.ProportionRow
import com.orchestrator.app.ui.finance.components.SectionCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboardScreen(
    viewModel: FinanceViewModel,
    onOpenTransactions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullState = rememberPullToRefreshState()

    var budgetEditTarget by remember { mutableStateOf<BudgetLine?>(null) }

    LaunchedEffect(pullState.isRefreshing) {
        if (pullState.isRefreshing) viewModel.pullToRefresh()
    }
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) pullState.endRefresh()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    budgetEditTarget?.let { line ->
        BudgetEditDialog(
            line = line,
            onDismiss = { budgetEditTarget = null },
            onSave = { amount ->
                viewModel.setBudget(line.category, amount)
                budgetEditTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Finance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = onOpenTransactions) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = "Transactions"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            if (uiState.isLoading && uiState.months.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item("kpis") { KpiSection(uiState) }
                    item("controls") { RangeControls(uiState, viewModel) }
                    item("cashflow") { CashFlowCard(uiState) }
                    item("categories") { CategoryCard(uiState) }
                    item("merchants") { MerchantCard(uiState) }
                    item("budgets") {
                        BudgetCard(uiState, onEdit = { budgetEditTarget = it })
                    }
                    item("recurring") { RecurringCard(uiState) }
                    item("velocity") { VelocityCard(uiState) }
                    item("accounts") { AccountsCard(uiState) }
                    item("spacer") { Spacer(Modifier.height(24.dp)) }
                }
            }

            PullToRefreshContainer(
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun KpiSection(state: FinanceUiState) {
    val netWorth = state.netWorth?.netWorth ?: 0.0
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KpiCard(
                label = "Net Worth",
                value = MoneyFormat.moneyWhole(netWorth),
                accent = if (netWorth >= 0) FinanceColors.Positive else FinanceColors.Negative,
                modifier = Modifier.weight(1f).padding(6.dp)
            )
            KpiCard(
                label = if (state.showAverage) "Spending / mo" else "Spending",
                value = MoneyFormat.moneyWhole(state.totalSpending),
                accent = FinanceColors.Negative,
                modifier = Modifier.weight(1f).padding(6.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KpiCard(
                label = if (state.showAverage) "Income / mo" else "Income",
                value = MoneyFormat.moneyWhole(state.totalIncome),
                accent = FinanceColors.Positive,
                modifier = Modifier.weight(1f).padding(6.dp)
            )
            KpiCard(
                label = "Net Cash Flow",
                value = MoneyFormat.moneyWhole(state.netCashFlow),
                accent = if (state.netCashFlow >= 0) FinanceColors.Positive else FinanceColors.Negative,
                modifier = Modifier.weight(1f).padding(6.dp)
            )
        }
    }
}

@Composable
private fun RangeControls(state: FinanceUiState, viewModel: FinanceViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(1 to "1M", 3 to "3M", 6 to "6M").forEach { (months, label) ->
            FilterChip(
                selected = state.rangeMonths == months,
                onClick = { viewModel.setRange(months) },
                label = { Text(label) }
            )
        }
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = state.showAverage,
            onClick = { viewModel.setShowAverage(!state.showAverage) },
            label = { Text("Avg / mo") }
        )
    }
}

@Composable
private fun CashFlowCard(state: FinanceUiState) {
    SectionCard(title = "Cash Flow") {
        Column {
            GroupedBarChart(
                labels = state.months.map { it.label },
                series = listOf(
                    ChartSeries("Income", FinanceColors.Positive, state.months.map { it.income.toFloat() }),
                    ChartSeries("Spending", FinanceColors.Negative, state.months.map { it.spending.toFloat() })
                )
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendDot("Income", FinanceColors.Positive)
                LegendDot("Spending", FinanceColors.Negative)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CategoryCard(state: FinanceUiState) {
    if (state.categorySpending.isEmpty()) return
    val top = state.categorySpending
    val maxAmount = top.maxOf { it.amount }.coerceAtLeast(1.0)
    SectionCard(title = "Spending by Category") {
        Column {
            top.forEachIndexed { index, cs ->
                ProportionRow(
                    label = cs.category,
                    valueLabel = MoneyFormat.money(cs.amount),
                    fraction = (cs.amount / maxAmount).toFloat(),
                    color = FinanceColors.swatch(index)
                )
            }
        }
    }
}

@Composable
private fun MerchantCard(state: FinanceUiState) {
    if (state.topMerchants.isEmpty()) return
    val maxAmount = state.topMerchants.maxOf { it.amount }.coerceAtLeast(1.0)
    SectionCard(title = "Top Merchants") {
        Column {
            state.topMerchants.forEach { m ->
                ProportionRow(
                    label = m.merchant,
                    valueLabel = MoneyFormat.money(m.amount),
                    fraction = (m.amount / maxAmount).toFloat(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(state: FinanceUiState, onEdit: (BudgetLine) -> Unit) {
    if (state.budgets.isEmpty()) return
    SectionCard(title = "Budgets") {
        Column {
            state.budgets.forEach { line ->
                BudgetRow(line = line, onClick = { onEdit(line) })
            }
        }
    }
}

@Composable
private fun BudgetRow(line: BudgetLine, onClick: () -> Unit) {
    val budget = line.budget
    val fraction = if (budget != null && budget > 0) (line.spent / budget).toFloat() else 0f
    val over = budget != null && line.spent > budget
    val barColor = if (over) FinanceColors.Negative else FinanceColors.Positive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = line.category,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Text(
                text = if (budget != null)
                    "${MoneyFormat.money(line.spent)} / ${MoneyFormat.money(budget)}"
                else
                    "${MoneyFormat.money(line.spent)} · set budget",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (budget == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
        line.rollingAvg?.let { avg ->
            Text(
                text = "3-mo avg ${MoneyFormat.money(avg)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun RecurringCard(state: FinanceUiState) {
    if (state.recurring.isEmpty()) return
    SectionCard(title = "Recurring Bills") {
        Column {
            state.recurring.take(8).forEach { bill ->
                RecurringRow(bill)
            }
        }
    }
}

@Composable
private fun RecurringRow(bill: RecurringCharge) {
    val days = daysUntil(bill.estimatedNextDate)
    val (badgeText, badgeColor) = when {
        days == null -> "—" to FinanceColors.Neutral
        days <= 7 -> "in ${max(days, 0)}d" to FinanceColors.Negative
        days <= 14 -> "in ${days}d" to FinanceColors.Warning
        else -> "in ${days}d" to FinanceColors.Neutral
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bill.merchantName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Pill(text = badgeText, color = badgeColor)
        }
        Text(
            text = MoneyFormat.money(bill.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VelocityCard(state: FinanceUiState) {
    val v = state.velocity ?: return
    val (label, color) = when (v.pace) {
        Pace.UNDER -> "Under pace" to FinanceColors.Positive
        Pace.ON -> "On pace" to FinanceColors.Warning
        Pace.OVER -> "Over pace" to FinanceColors.Negative
    }
    SectionCard(title = "Spending Velocity") {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = MoneyFormat.money(v.spentSoFar),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "spent this month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Pill(text = label, color = color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Projected ${MoneyFormat.money(v.projected)} · avg ${MoneyFormat.money(v.historicalAvg)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountsCard(state: FinanceUiState) {
    if (state.accountGroups.isEmpty()) return
    SectionCard(title = "Accounts") {
        Column {
            state.accountGroups.forEach { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormat.money(group.total),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                group.accounts.forEach { acct ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = acct.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        Text(
                            text = MoneyFormat.money(acct.balance),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetEditDialog(
    line: BudgetLine,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(line.budget?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget · ${line.category}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() || it == '.' } },
                label = { Text("Monthly amount") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = text.toDoubleOrNull()
                if (amount != null) onSave(amount)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun daysUntil(dateStr: String): Int? = try {
    ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dateStr.take(10))).toInt()
} catch (e: Exception) {
    null
}
