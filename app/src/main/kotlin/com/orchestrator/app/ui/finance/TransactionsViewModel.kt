package com.orchestrator.app.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orchestrator.app.data.model.Account
import com.orchestrator.app.data.model.Transaction
import com.orchestrator.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A pending offer to turn a one-off category edit into a permanent merchant rule. */
data class RuleSuggestion(val merchant: String, val category: String)

data class TransactionsUiState(
    val items: List<Transaction> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val accounts: List<Account> = emptyList(),
    // filters
    val search: String = "",
    val category: String? = null,
    val accountId: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val sortBy: String = "date",
    val sortDir: String = "desc",
    val ruleSuggestion: RuleSuggestion? = null
) {
    val endReached: Boolean get() = items.size >= total
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 30
    }

    init {
        loadAccounts()
        reload()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            repository.getAccounts().onSuccess { accts ->
                _uiState.update { it.copy(accounts = accts) }
            }
        }
    }

    /** Fetch page 1, replacing the list. Call after any filter/sort change. */
    fun reload() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getTransactions(
                accountId = s.accountId,
                category = s.category,
                search = s.search,
                startDate = s.startDate,
                endDate = s.endDate,
                page = 1,
                pageSize = PAGE_SIZE,
                sortBy = s.sortBy,
                sortDir = s.sortDir
            ).fold(
                onSuccess = { pageData ->
                    _uiState.update {
                        it.copy(
                            items = pageData.items,
                            total = pageData.total,
                            page = 1,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingMore || s.endReached) return
        val nextPage = s.page + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.getTransactions(
                accountId = s.accountId,
                category = s.category,
                search = s.search,
                startDate = s.startDate,
                endDate = s.endDate,
                page = nextPage,
                pageSize = PAGE_SIZE,
                sortBy = s.sortBy,
                sortDir = s.sortDir
            ).fold(
                onSuccess = { pageData ->
                    _uiState.update {
                        it.copy(
                            items = it.items + pageData.items,
                            total = pageData.total,
                            page = nextPage,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoadingMore = false, error = e.message) } }
            )
        }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    fun setCategory(category: String?) {
        _uiState.update { it.copy(category = category) }
        reload()
    }

    fun setAccount(accountId: String?) {
        _uiState.update { it.copy(accountId = accountId) }
        reload()
    }

    fun setDateRange(start: String?, end: String?) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        reload()
    }

    fun setSort(sortBy: String) {
        _uiState.update {
            val newDir = if (it.sortBy == sortBy && it.sortDir == "desc") "asc" else "desc"
            it.copy(sortBy = sortBy, sortDir = if (it.sortBy == sortBy) newDir else "desc")
        }
        reload()
    }

    fun editCategory(transaction: Transaction, category: String) {
        if (category == transaction.category) return
        viewModelScope.launch {
            repository.updateCategory(transaction.id, category).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.map { if (it.id == updated.id) updated else it },
                            ruleSuggestion = updated.merchantName
                                ?.takeIf { it.isNotBlank() }
                                ?.let { RuleSuggestion(it, category) }
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun confirmRule() {
        val suggestion = _uiState.value.ruleSuggestion ?: return
        viewModelScope.launch {
            repository.createRule(suggestion.merchant, suggestion.category)
            _uiState.update { it.copy(ruleSuggestion = null) }
        }
    }

    fun dismissRule() {
        _uiState.update { it.copy(ruleSuggestion = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
