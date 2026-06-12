package com.orchestrator.app.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.orchestrator.app.data.model.Task
import com.orchestrator.app.ui.finance.FinanceDashboardScreen
import com.orchestrator.app.ui.finance.TransactionsScreen
import com.orchestrator.app.ui.tasks.TaskListScreen

private const val TAB_TASKS = 0
private const val TAB_FINANCE = 1

/**
 * Top-level shell hosting a Material3 bottom navigation bar. Both Tasks and Finance live
 * under the same nav destination, so their ViewModels (and scroll/state) survive tab
 * switches. Full-screen flows (Login, TaskEdit) stay outside this scaffold.
 */
@Composable
fun MainScaffold(
    onEditTask: (Task) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(TAB_TASKS) }
    var financeShowTransactions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == TAB_TASKS,
                    onClick = { selectedTab = TAB_TASKS },
                    icon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
                    label = { Text("Tasks") }
                )
                NavigationBarItem(
                    selected = selectedTab == TAB_FINANCE,
                    onClick = { selectedTab = TAB_FINANCE },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Finance") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                TAB_TASKS -> TaskListScreen(
                    viewModel = hiltViewModel(),
                    onEditTask = onEditTask,
                    onLogout = onLogout
                )
                TAB_FINANCE -> {
                    if (financeShowTransactions) {
                        TransactionsScreen(
                            viewModel = hiltViewModel(),
                            onNavigateBack = { financeShowTransactions = false }
                        )
                    } else {
                        FinanceDashboardScreen(
                            viewModel = hiltViewModel(),
                            onOpenTransactions = { financeShowTransactions = true }
                        )
                    }
                }
            }
        }
    }
}
