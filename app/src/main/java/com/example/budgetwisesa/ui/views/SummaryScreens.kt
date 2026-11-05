package com.example.budgetwisesa.ui.views

import android.widget.TextView
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.data.model.TransactionType
import com.example.budgetwisesa.databinding.*
import com.google.android.material.chip.Chip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DashboardScreen(private val host: MainActivity,
    private val binding: ScreenDashboardBinding = ScreenDashboardBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    override fun refresh() {
        val transactions = host.model.transactions.value
        val goals = host.model.goals.value
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        binding.balance.text = money(income - expense)
        binding.income.text = host.getString(R.string.income) + ": " + money(income)
        binding.expense.text = host.getString(R.string.expense) + ": " + money(expense)
        val dates = transactions.map { Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        val earned = listOfNotNull(
            R.string.budget_hero.takeIf { (0L..6L).all { day -> LocalDate.now().minusDays(day) in dates } },
            R.string.saver_pro.takeIf { goals.any { it.currentAmount >= it.targetAmount } },
            R.string.consistent_recorder.takeIf { transactions.size >= 10 },
            R.string.wealth_builder.takeIf { goals.sumOf { it.currentAmount } >= 1000 }
        )
        binding.badges.removeAllViews()
        earned.forEach { title -> binding.badges.addView(Chip(host).apply { setText(title); isClickable = false; isCheckable = false }) }
        binding.noBadges.isVisible = earned.isEmpty()
        binding.empty.isVisible = transactions.isEmpty()
        binding.recent.removeAllViews()
        transactions.take(5).forEach { item ->
            val row = ItemRecordBinding.inflate(host.layoutInflater, binding.recent, false)
            row.title.text = item.category
            row.value.text = (if (item.type == TransactionType.INCOME) "+ " else "− ") + money(item.amount)
            row.subtitle.text = date(item.date)
            row.actions.isVisible = false
            binding.recent.addView(row.root)
        }
    }
}

class ReportsScreen(private val host: MainActivity,
    private val binding: ScreenReportsBinding = ScreenReportsBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    override fun refresh() {
        val transactions = host.model.transactions.value
        val budgets = host.model.budgets.value
        val goals = host.model.goals.value
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val spent = expenses.sumOf { it.amount }
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalBudget = budgets.sumOf { it.amount }
        val totalSpent = budgets.sumOf { it.spent }
        val target = goals.sumOf { it.targetAmount }
        val saved = goals.sumOf { it.currentAmount }
        val budgetPerformance = if (totalBudget > 0) "${((totalBudget - (totalSpent - totalBudget).coerceAtLeast(0.0)) / totalBudget * 100).coerceIn(0.0, 100.0).toInt()}%" else "—"
        val savings = if (target > 0) "${(100 * saved / target).toInt()}%" else "—"
        val ratio = if (income > 0) "${(100 * spent / income).toInt()}%" else "—"
        binding.performance.text = listOf(host.getString(R.string.budget_performance) + ": " + budgetPerformance,
            host.getString(R.string.savings_progress) + ": " + savings, host.getString(R.string.expense_ratio) + ": " + ratio).joinToString("\n")
        val totals = expenses.groupBy { it.category }.map { (category, items) -> category to items.sumOf { it.amount } }
        binding.chart.isVisible = totals.isNotEmpty()
        binding.empty.isVisible = totals.isEmpty()
        binding.chart.update(totals)
        binding.chart.contentDescription = host.getString(R.string.spending_category) + ": " + totals.joinToString { it.first + " " + money(it.second) }
        binding.legend.removeAllViews()
        totals.forEachIndexed { index, (category, value) ->
            val text = TextView(host).apply {
                this.text = "●  $category: ${money(value)}"
                setPadding(0, 12, 0, 12)
                setTextColor(SpendingChartView.colors[index % SpendingChartView.colors.size])
                textSize = 16f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            binding.legend.addView(text)
        }
    }
}
