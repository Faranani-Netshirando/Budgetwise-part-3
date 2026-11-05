package com.example.budgetwisesa.ui.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetwisesa.AppRoute
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.data.model.TransactionType
import com.example.budgetwisesa.databinding.ItemRecordBinding
import com.example.budgetwisesa.databinding.ScreenRecordsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class FinancialRow(val id: String, val title: String, val value: String, val subtitle: String,
    val progress: Int? = null, val warning: String? = null, val receipt: String? = null)

class FinancialAdapter(private val edit: (String) -> Unit, private val delete: (String) -> Unit,
    private val receipt: (String) -> Unit) : ListAdapter<FinancialRow, FinancialAdapter.Holder>(object : DiffUtil.ItemCallback<FinancialRow>() {
        override fun areItemsTheSame(old: FinancialRow, new: FinancialRow) = old.id == new.id
        override fun areContentsTheSame(old: FinancialRow, new: FinancialRow) = old == new
    }) {
    class Holder(val b: ItemRecordBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        with(holder.b) {
            title.text = row.title; value.text = row.value; subtitle.text = row.subtitle
            progress.isVisible = row.progress != null; progress.progress = row.progress ?: 0
            warning.text = row.warning; warning.isVisible = !row.warning.isNullOrBlank()
            receipt.isVisible = row.receipt != null
            receipt.setOnClickListener { row.receipt?.let(this@FinancialAdapter.receipt) }
            edit.contentDescription = root.context.getString(R.string.edit) + ": " + row.title
            delete.contentDescription = root.context.getString(R.string.delete) + ": " + row.title
            edit.setOnClickListener { this@FinancialAdapter.edit(row.id) }
            delete.setOnClickListener { this@FinancialAdapter.delete(row.id) }
        }
    }
}

class RecordsScreen(private val host: MainActivity, private val route: AppRoute,
    private val binding: ScreenRecordsBinding = ScreenRecordsBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    private val dialogs = FinancialDialogs(host)
    private val adapter = FinancialAdapter(::edit, ::delete, host::showReceipt)
    init {
        binding.records.layoutManager = LinearLayoutManager(host)
        binding.records.adapter = adapter
        val transactions = route == AppRoute.TRANSACTIONS
        binding.searchLayout.isVisible = transactions
        binding.filter.isVisible = transactions
        binding.filter.adapter = ArrayAdapter(host, android.R.layout.simple_spinner_dropdown_item,
            listOf(R.string.all, R.string.income, R.string.expense).map(host::getString))
        binding.filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = refresh()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.search.doAfterTextChanged { refresh() }
        binding.addRecord.setText(when (route) { AppRoute.BUDGETS -> R.string.add_budget; AppRoute.GOALS -> R.string.add_goal; else -> R.string.add_transaction })
        binding.empty.setText(when (route) { AppRoute.BUDGETS -> R.string.no_budgets; AppRoute.GOALS -> R.string.no_goals; else -> R.string.no_transactions })
        binding.addRecord.setOnClickListener { when (route) {
            AppRoute.BUDGETS -> dialogs.budget(); AppRoute.GOALS -> dialogs.goal(); else -> dialogs.transaction()
        } }
    }
    override fun refresh() {
        if (!active) return
        val rows = when (route) {
            AppRoute.BUDGETS -> host.model.budgets.value.map { item ->
                val progress = (100 * item.spent / item.amount).toInt()
                val warning = when {
                    item.spent > item.amount -> host.getString(R.string.budget_exceeded, number(item.spent - item.amount))
                    progress >= host.model.alertThreshold.value -> host.getString(R.string.budget_reached, progress)
                    else -> null
                }
                val period = host.getString(when (item.period) { "Weekly" -> R.string.weekly; "Yearly" -> R.string.yearly; else -> R.string.monthly })
                FinancialRow(item.id, item.category, money(item.amount),
                    host.getString(R.string.period_value, period) + "\n" + host.getString(R.string.spent_value, number(item.spent)) +
                        "\n" + host.getString(R.string.remaining_value, number(item.amount - item.spent)), progress.coerceIn(0, 100), warning)
            }
            AppRoute.GOALS -> host.model.goals.value.map { item ->
                val percent = (100 * item.currentAmount / item.targetAmount).toInt()
                FinancialRow(item.id, item.name, "$percent%", host.getString(R.string.saved_value, number(item.currentAmount), number(item.targetAmount)) +
                    "\n" + host.getString(R.string.deadline_value, date(item.targetDate)) + "\n" + item.description, percent.coerceIn(0, 100))
            }
            else -> {
                val query = binding.search.text.toString().trim()
                val filter = binding.filter.selectedItemPosition
                host.model.transactions.value.filter {
                    (it.category.contains(query, true) || it.description.contains(query, true)) &&
                        (filter == 0 || (filter == 1 && it.type == TransactionType.INCOME) || (filter == 2 && it.type == TransactionType.EXPENSE))
                }.map { FinancialRow(it.id, it.category, (if (it.type == TransactionType.INCOME) "+ " else "− ") + money(it.amount),
                    date(it.date) + "\n" + it.description, receipt = it.receiptPath) }
            }
        }
        binding.empty.isVisible = rows.isEmpty()
        adapter.submitList(rows)
    }
    private fun edit(id: String) { when (route) {
        AppRoute.BUDGETS -> host.model.budgets.value.find { it.id == id }?.let(dialogs::budget)
        AppRoute.GOALS -> host.model.goals.value.find { it.id == id }?.let(dialogs::goal)
        else -> host.model.transactions.value.find { it.id == id }?.let(dialogs::transaction)
    } }
    private fun delete(id: String) {
        MaterialAlertDialogBuilder(host).setTitle(R.string.delete)
            .setPositiveButton(R.string.delete) { _, _ -> when (route) {
                AppRoute.BUDGETS -> host.model.budgets.value.find { it.id == id }?.let(host.model::deleteBudget)
                AppRoute.GOALS -> host.model.goals.value.find { it.id == id }?.let(host.model::deleteGoal)
                else -> host.model.transactions.value.find { it.id == id }?.let(host.model::deleteTransaction)
            } }.setNegativeButton(R.string.cancel, null).show()
    }
    override fun close() { dialogs.close(); binding.records.adapter = null; super.close() }
}
