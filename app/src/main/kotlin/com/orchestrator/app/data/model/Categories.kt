package com.orchestrator.app.data.model

/**
 * Canonical transaction categories, mirroring the web frontend's ALL_CATEGORIES
 * (orchestrator/frontend/src/pages/Transactions.tsx). Used to populate filter and
 * inline category-edit pickers.
 */
val ALL_CATEGORIES: List<String> = listOf(
    "Food & Dining",
    "Shopping",
    "Transportation",
    "Bills & Utilities",
    "Healthcare",
    "Entertainment",
    "Travel",
    "Income",
    "Transfer",
    "Home",
    "Investments",
    "Mortgage & Rent",
    "Other"
)
