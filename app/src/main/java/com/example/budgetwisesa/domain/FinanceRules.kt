package com.example.budgetwisesa.domain

import com.example.budgetwisesa.data.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

object FinanceRules {
    fun positiveAmount(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 1_000_000_000 }

    fun valid(t: Transaction) = t.category.isNotBlank() && t.category.length <= 80 &&
        t.amount.isFinite() && t.amount > 0 && t.amount <= 1_000_000_000 && t.date > 0 && t.description.length <= 1000
    fun valid(b: Budget) = b.category.isNotBlank() && b.category.length <= 80 &&
        b.amount.isFinite() && b.amount > 0 && b.amount <= 1_000_000_000 && b.period in listOf("Weekly", "Monthly", "Yearly")
    fun valid(g: SavingsGoal) = g.name.isNotBlank() && g.name.length <= 80 &&
        g.targetAmount.isFinite() && g.targetAmount > 0 && g.targetAmount <= 1_000_000_000 &&
        g.currentAmount.isFinite() && g.currentAmount >= 0 && g.currentAmount <= 1_000_000_000 &&
        g.targetDate > 0 && g.description.length <= 1000

    // Recomputing spending fixes edit/delete drift and resets each calendar period.
    fun spending(budget: Budget, transactions: List<Transaction>, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Double {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = when (budget.period) {
            "Weekly" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "Yearly" -> today.withDayOfYear(1)
            else -> today.withDayOfMonth(1)
        }
        val end = when (budget.period) {
            "Weekly" -> start.plusWeeks(1)
            "Yearly" -> start.plusYears(1)
            else -> start.plusMonths(1)
        }
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = end.atStartOfDay(zone).toInstant().toEpochMilli()
        return transactions.filter { !it.deleted && it.type == TransactionType.EXPENSE &&
            it.category.trim().equals(budget.category.trim(), true) && it.date >= from && it.date < until }.sumOf { it.amount }
    }
}
