package com.orchestrator.app.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orchestrator.app.data.model.Account
import com.orchestrator.app.data.model.Budget
import com.orchestrator.app.data.model.NetWorth
import com.orchestrator.app.data.model.RecurringCharge
import com.orchestrator.app.data.model.RollingAverage
import com.orchestrator.app.data.model.Transaction
import com.orchestrator.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---- Derived view types ----

data class MonthBucket(val month: YearMonth, val label: String, val income: Double, val spending: Double)
data class CategorySpend(val category: String, val amount: Double)
data class MerchantSpend(val merchant: String, val amount: Double)
data class BudgetLine(val category: String, val spent: Double, val budget: Double?, val rollingAvg: Double?)
data class AccountGroup(val title: String, val accounts: List<Account>, val total: Double)

enum class Pace { UNDER, ON, OVER }

data class VelocityInfo(
    val spentSoFar: Double,
    val projected: Double,
    val historicalAvg: Double,
    val pace: Pace
)

data class FinanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val netWorth: NetWorth? = null,
    val totalSpending: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val months: List<MonthBucket> = emptyList(),
    val categorySpending: List<CategorySpend> = emptyList(),
    val topMerchants: List<MerchantSpend> = emptyList(),
    val budgets: List<BudgetLine> = emptyList(),
    val recurring: List<RecurringCharge> = emptyList(),
    val velocity: VelocityInfo? = null,
    val accountGroups: List<AccountGroup> = emptyList(),
    val rangeMonths: Int = 3, // 1 | 3 | 6
    val showAverage: Boolean = false
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    // Raw data cached so range/average toggles recompute without refetching.
    private var rawTransactions: List<Transaction> = emptyList()
    private var rawAccounts: List<Account> = emptyList()
    private var rawBudgets: List<Budget> = emptyList()
    private var rawRollingAverages: List<RollingAverage> = emptyList()
    private var rawRecurring: List<RecurringCharge> = emptyList()
    private var rawNetWorth: NetWorth? = null

    init {
        load(initial = true)
    }

    fun load(initial: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = initial, isRefreshing = !initial, error = null) }
            fetchAll()
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    /** Pull-to-refresh: trigger a backend Plaid sync, then reload. */
    fun pullToRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            repository.sync() // best-effort; ignore sync failures, still reload
            fetchAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setRange(months: Int) {
        _uiState.update { it.copy(rangeMonths = months) }
        recompute()
    }

    fun setShowAverage(show: Boolean) {
        _uiState.update { it.copy(showAverage = show) }
        recompute()
    }

    fun setBudget(category: String, monthlyAmount: Double) {
        viewModelScope.launch {
            repository.upsertBudget(category, monthlyAmount).fold(
                onSuccess = { updated ->
                    rawBudgets = rawBudgets.filter { it.category != category } + updated
                    recompute()
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    private suspend fun fetchAll() {
        // Always fetch a 6-month window so range toggles are instant client-side.
        val startDate = LocalDate.now().minusMonths(5).withDayOfMonth(1)
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val netWorthD = viewModelScope.async { repository.getNetWorth() }
        val accountsD = viewModelScope.async { repository.getAccounts() }
        val txD = viewModelScope.async {
            repository.getTransactions(startDate = startStr, pageSize = 2000, sortBy = "date", sortDir = "desc")
        }
        val recurringD = viewModelScope.async { repository.getRecurring() }
        val budgetsD = viewModelScope.async { repository.getBudgets() }
        val rollingD = viewModelScope.async { repository.getRollingAverages() }

        val txResult = txD.await()
        if (txResult.isFailure) {
            _uiState.update { it.copy(error = txResult.exceptionOrNull()?.message ?: "Failed to load transactions") }
            return
        }

        rawTransactions = txResult.getOrNull()?.items ?: emptyList()
        rawAccounts = accountsD.await().getOrDefault(emptyList())
        rawRecurring = recurringD.await().getOrDefault(emptyList())
        rawBudgets = budgetsD.await().getOrDefault(emptyList())
        rawRollingAverages = rollingD.await().getOrDefault(emptyList())
        rawNetWorth = netWorthD.await().getOrNull()

        recompute()
    }

    private fun recompute() {
        val range = _uiState.value.rangeMonths
        val showAverage = _uiState.value.showAverage

        val today = LocalDate.now()
        val windowStart = YearMonth.from(today).minusMonths((range - 1).toLong())

        // Spending/income excludes transfers; income amounts are negative.
        val windowTx = rawTransactions.mapNotNull { tx ->
            val d = parseDate(tx.date) ?: return@mapNotNull null
            if (YearMonth.from(d) < windowStart) null else tx
        }.filter { !it.pending && it.category != "Transfer" }

        // Monthly buckets across the window (chronological).
        val buckets = (0 until range).map { offset ->
            val ym = windowStart.plusMonths(offset.toLong())
            val inMonth = windowTx.filter { parseDate(it.date)?.let { d -> YearMonth.from(d) == ym } == true }
            val spending = inMonth.filter { it.amount > 0 }.sumOf { it.amount }
            val income = inMonth.filter { it.amount < 0 }.sumOf { -it.amount }
            MonthBucket(ym, ym.format(MONTH_LABEL), income, spending)
        }

        val totalSpending = buckets.sumOf { it.spending }
        val totalIncome = buckets.sumOf { it.income }
        val divisor = if (showAverage && range > 0) range.toDouble() else 1.0

        // Category spending (expenses only) over the window.
        val categorySpending = windowTx
            .filter { it.amount > 0 }
            .groupBy { it.category ?: "Uncategorized" }
            .map { (cat, txs) -> CategorySpend(cat, txs.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
            .take(10)

        // Top merchants (expenses only).
        val topMerchants = windowTx
            .filter { it.amount > 0 }
            .groupBy { it.displayName }
            .map { (m, txs) -> MerchantSpend(m, txs.sumOf { it.amount }) }
            .sortedByDescending { it.amount }
            .take(8)

        // Budget lines: current calendar month spend per budgeted category.
        val currentMonth = YearMonth.from(today)
        val currentMonthTx = rawTransactions.filter {
            !it.pending && it.category != "Transfer" && it.amount > 0 &&
                parseDate(it.date)?.let { d -> YearMonth.from(d) == currentMonth } == true
        }
        val spentByCategory = currentMonthTx.groupBy { it.category ?: "Uncategorized" }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        val rollingByCategory = rawRollingAverages.associate { it.category to it.avgMonthly }
        val budgetedCategories = rawBudgets.map { it.category }.toSet()
        val budgetLines = (budgetedCategories + spentByCategory.keys)
            .map { cat ->
                BudgetLine(
                    category = cat,
                    spent = spentByCategory[cat] ?: 0.0,
                    budget = rawBudgets.find { it.category == cat }?.monthlyAmount,
                    rollingAvg = rollingByCategory[cat]
                )
            }
            .sortedWith(compareByDescending<BudgetLine> { it.budget != null }.thenByDescending { it.spent })
            .take(8)

        // Velocity for the current month.
        val velocity = computeVelocity(buckets, currentMonth, today)

        // Account balances grouped by type.
        val accountGroups = groupAccounts(rawAccounts)

        _uiState.update {
            it.copy(
                netWorth = rawNetWorth,
                totalSpending = totalSpending / divisor,
                totalIncome = totalIncome / divisor,
                netCashFlow = (totalIncome - totalSpending) / divisor,
                months = buckets,
                categorySpending = categorySpending,
                topMerchants = topMerchants,
                budgets = budgetLines,
                recurring = rawRecurring,
                velocity = velocity,
                accountGroups = accountGroups
            )
        }
    }

    private fun computeVelocity(
        buckets: List<MonthBucket>,
        currentMonth: YearMonth,
        today: LocalDate
    ): VelocityInfo? {
        val current = buckets.find { it.month == currentMonth } ?: return null
        val priorMonths = buckets.filter { it.month != currentMonth }
        val historicalAvg = if (priorMonths.isNotEmpty()) priorMonths.sumOf { it.spending } / priorMonths.size else 0.0
        val dayOfMonth = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()
        val projected = if (dayOfMonth > 0) current.spending / dayOfMonth * daysInMonth else current.spending
        val pace = when {
            historicalAvg <= 0.0 -> Pace.ON
            projected > historicalAvg * 1.10 -> Pace.OVER
            projected < historicalAvg * 0.90 -> Pace.UNDER
            else -> Pace.ON
        }
        return VelocityInfo(current.spending, projected, historicalAvg, pace)
    }

    private fun groupAccounts(accounts: List<Account>): List<AccountGroup> {
        if (accounts.isEmpty()) return emptyList()
        val order = listOf(
            "depository" to "Checking & Savings",
            "credit" to "Credit Cards",
            "investment" to "Investments",
            "loan" to "Loans",
            "other" to "Other"
        )
        return order.mapNotNull { (type, title) ->
            val group = accounts.filter { (it.type.ifBlank { "other" }) == type }
            if (group.isEmpty()) null
            else AccountGroup(title, group, group.sumOf { it.balance })
        }
    }

    private fun parseDate(value: String): LocalDate? =
        try { LocalDate.parse(value.take(10)) } catch (e: Exception) { null }

    companion object {
        private val MONTH_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
    }
}
