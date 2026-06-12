package com.orchestrator.app.ui.finance

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Currency formatting helpers for the finance screens.
 *
 * Backend sign convention: a transaction [amount] is POSITIVE for an expense/debit and
 * NEGATIVE for income/credit. UI helpers below convert to the +/- the user expects.
 */
object MoneyFormat {

    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    private val currencyWhole: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    /** Absolute, two-decimal currency, e.g. "$1,234.56" — for balances/KPIs. */
    fun money(value: Double): String = currency.format(value)

    /** Absolute, no-cents currency, e.g. "$1,235" — for compact KPI tiles. */
    fun moneyWhole(value: Double): String = currencyWhole.format(value.roundToLong())

    /** Compact form for axis labels / chips: "$1.2k", "$3.4M". */
    fun compact(value: Double): String {
        val v = abs(value)
        return when {
            v >= 1_000_000 -> "$" + trim(v / 1_000_000) + "M"
            v >= 1_000 -> "$" + trim(v / 1_000) + "k"
            else -> "$" + v.roundToLong()
        }
    }

    private fun trim(d: Double): String {
        val s = String.format(Locale.US, "%.1f", d)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    /**
     * Signed flow label for a raw transaction amount (positive = expense).
     * Expense -> "-$50.00", income -> "+$100.00".
     */
    fun signedFlow(amount: Double): String {
        val abs = money(abs(amount))
        return if (amount > 0) "-$abs" else "+$abs"
    }
}
