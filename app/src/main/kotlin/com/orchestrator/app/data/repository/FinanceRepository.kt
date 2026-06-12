package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.Account
import com.orchestrator.app.data.model.Budget
import com.orchestrator.app.data.model.BudgetUpdateRequest
import com.orchestrator.app.data.model.CategoryRule
import com.orchestrator.app.data.model.CategoryUpdateRequest
import com.orchestrator.app.data.model.NetWorth
import com.orchestrator.app.data.model.RecurringCharge
import com.orchestrator.app.data.model.RollingAverage
import com.orchestrator.app.data.model.RuleRequest
import com.orchestrator.app.data.model.SyncResult
import com.orchestrator.app.data.model.SyncStatus
import com.orchestrator.app.data.model.Transaction
import com.orchestrator.app.data.model.TransactionPage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/write access to the backend finance slice. Mirrors [TaskRepository]'s
 * Result-wrapped suspend style. No backend changes — these hit the same endpoints
 * the web frontend uses.
 */
@Singleton
class FinanceRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getAccounts(): Result<List<Account>> = runCatching { api.getAccounts() }

    suspend fun getNetWorth(): Result<NetWorth> = runCatching { api.getNetWorth() }

    suspend fun getTransactions(
        accountId: String? = null,
        category: String? = null,
        search: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        pageSize: Int = 50,
        sortBy: String = "date",
        sortDir: String = "desc"
    ): Result<TransactionPage> = runCatching {
        api.getTransactions(
            accountId = accountId,
            category = category,
            search = search?.takeIf { it.isNotBlank() },
            startDate = startDate,
            endDate = endDate,
            page = page,
            pageSize = pageSize,
            sortBy = sortBy,
            sortDir = sortDir
        )
    }

    suspend fun getRecurring(): Result<List<RecurringCharge>> = runCatching { api.getRecurring() }

    suspend fun updateCategory(transactionId: String, category: String): Result<Transaction> =
        runCatching { api.updateTransactionCategory(transactionId, CategoryUpdateRequest(category)) }

    suspend fun getBudgets(): Result<List<Budget>> = runCatching { api.getBudgets() }

    suspend fun getRollingAverages(): Result<List<RollingAverage>> =
        runCatching { api.getRollingAverages() }

    suspend fun upsertBudget(category: String, monthlyAmount: Double): Result<Budget> =
        runCatching { api.upsertBudget(category, BudgetUpdateRequest(monthlyAmount)) }

    suspend fun getRules(): Result<List<CategoryRule>> = runCatching { api.getRules() }

    suspend fun createRule(merchantName: String, category: String): Result<CategoryRule> =
        runCatching { api.createRule(RuleRequest(merchantName, category)) }

    suspend fun sync(): Result<SyncResult> = runCatching { api.syncTransactions() }

    suspend fun getSyncStatus(): Result<SyncStatus> = runCatching { api.getSyncStatus() }
}
