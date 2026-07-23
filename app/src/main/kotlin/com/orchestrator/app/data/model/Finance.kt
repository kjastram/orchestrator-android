package com.orchestrator.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the backend finance slice (accounts / transactions / budgets / rules).
 * Mirrors the shapes the web frontend consumes — see ../orchestrator/backend/app/routers/.
 *
 * Amount sign convention (matches backend): positive = expense/debit, negative = income/credit.
 */

data class Account(
    @SerializedName("id")
    val id: String,
    @SerializedName("item_id")
    val itemId: String?,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String, // depository | credit | investment | loan | other
    @SerializedName("subtype")
    val subtype: String?,
    @SerializedName("current_balance")
    val currentBalance: Double?,
    @SerializedName("available_balance")
    val availableBalance: Double?,
    @SerializedName("currency")
    val currency: String = "USD",
    @SerializedName("balance")
    val balance: Double = 0.0,
    @SerializedName("last_updated")
    val lastUpdated: String? = null // ISO datetime; null when never refreshed
)

data class NetWorth(
    @SerializedName("total_assets")
    val totalAssets: Double,
    @SerializedName("total_liabilities")
    val totalLiabilities: Double,
    @SerializedName("net_worth")
    val netWorth: Double
)

data class Transaction(
    @SerializedName("id")
    val id: String,
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("date")
    val date: String, // YYYY-MM-DD
    @SerializedName("amount")
    val amount: Double, // positive = expense, negative = income
    @SerializedName("merchant_name")
    val merchantName: String?,
    @SerializedName("description")
    val description: String,
    @SerializedName("category")
    val category: String?,
    @SerializedName("category_source")
    val categorySource: String?, // rule | plaid | ai | user | transfer
    @SerializedName("plaid_category")
    val plaidCategory: String?,
    @SerializedName("pending")
    val pending: Boolean = false,
    @SerializedName("currency")
    val currency: String = "USD"
) {
    /** Human-friendly label for the row. */
    val displayName: String
        get() = merchantName?.takeIf { it.isNotBlank() } ?: description
}

data class TransactionPage(
    @SerializedName("items")
    val items: List<Transaction>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int
)

data class RecurringCharge(
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("last_date")
    val lastDate: String,
    @SerializedName("estimated_next_date")
    val estimatedNextDate: String,
    @SerializedName("occurrence_count")
    val occurrenceCount: Int
)

data class Budget(
    @SerializedName("id")
    val id: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("monthly_amount")
    val monthlyAmount: Double,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class RollingAverage(
    @SerializedName("category")
    val category: String,
    @SerializedName("avg_monthly")
    val avgMonthly: Double
)

data class CategoryRule(
    @SerializedName("id")
    val id: String,
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("created_at")
    val createdAt: String?
)

// ---- Request bodies ----

data class CategoryUpdateRequest(
    @SerializedName("category")
    val category: String
)

data class BudgetUpdateRequest(
    @SerializedName("monthly_amount")
    val monthlyAmount: Double
)

data class RuleRequest(
    @SerializedName("merchant_name")
    val merchantName: String,
    @SerializedName("category")
    val category: String
)

data class SyncResult(
    @SerializedName("synced_items")
    val syncedItems: Int = 0,
    @SerializedName("added")
    val added: Int = 0,
    @SerializedName("modified")
    val modified: Int = 0,
    @SerializedName("removed")
    val removed: Int = 0
)
