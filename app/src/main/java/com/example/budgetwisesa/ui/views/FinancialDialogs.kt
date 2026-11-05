package com.example.budgetwisesa.ui.views

import android.app.DatePickerDialog
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import coil.load
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.data.ReceiptFiles
import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.databinding.*
import com.example.budgetwisesa.domain.FinanceRules
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.util.Calendar

class FinancialDialogs(private val host: MainActivity) {
    private var dialog: AlertDialog? = null
    private var dialogScope: CoroutineScope? = null
    fun close() { dialog?.dismiss(); dialogScope?.cancel(); dialog = null }
    private fun show(title: Int, root: android.view.View, setup: (AlertDialog) -> Unit) {
        close()
        val current = MaterialAlertDialogBuilder(host).setTitle(title).setView(root)
            .setPositiveButton(R.string.save, null).setNegativeButton(R.string.cancel, null).create()
        dialog = current
        dialogScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        current.setOnShowListener { setup(current) }
        current.setOnDismissListener { dialogScope?.cancel() }
        current.show()
    }
    private fun spinner(view: Spinner, labels: List<Int>, selected: Int) {
        view.adapter = ArrayAdapter(host, android.R.layout.simple_spinner_dropdown_item, labels.map(host::getString))
        view.setSelection(selected)
    }
    private fun watch(fields: List<EditText>, refresh: () -> Unit) { fields.forEach { it.doAfterTextChanged { refresh() } } }
    private fun chooseDate(value: Long, chosen: (Long) -> Unit) {
        val date = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(host, { _, year, month, day ->
            date.set(year, month, day); chosen(date.timeInMillis)
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun transaction(existing: Transaction? = null) {
        val b = DialogTransactionBinding.inflate(host.layoutInflater)
        val original = existing ?: Transaction(type = TransactionType.EXPENSE, category = "", amount = 0.0,
            date = System.currentTimeMillis(), description = "")
        var selectedDate = original.date
        var receipt = original.receiptPath
        var importing = false
        var receiptFailed = false
        b.category.setText(original.category)
        b.amount.setText(existing?.amount?.toString().orEmpty())
        b.description.setText(original.description)
        spinner(b.type, listOf(R.string.income, R.string.expense), if (original.type == TransactionType.INCOME) 0 else 1)
        b.date.text = host.getString(R.string.date_label) + ": " + date(selectedDate)
        fun candidate() = original.copy(type = if (b.type.selectedItemPosition == 0) TransactionType.INCOME else TransactionType.EXPENSE,
            category = b.category.text.toString().trim(), amount = FinanceRules.positiveAmount(b.amount.text.toString()) ?: 0.0,
            date = selectedDate, description = b.description.text.toString(), receiptPath = receipt)
        show(if (existing == null) R.string.add_transaction else R.string.edit_transaction, b.root) { alert ->
            fun refresh() {
                val valid = FinanceRules.valid(candidate())
                alert.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = valid && !importing
                b.error.setText(if (receiptFailed) R.string.receipt_error else R.string.invalid_financial_input)
                b.error.isVisible = !valid || receiptFailed
                b.attach.isEnabled = !importing
                b.attach.setText(if (receipt == null) R.string.attach_receipt else R.string.receipt_attached)
                b.preview.isVisible = receipt != null
                if (receipt != null) b.preview.load(receipt)
            }
            watch(listOf(b.category, b.amount, b.description), ::refresh)
            b.date.setOnClickListener { chooseDate(selectedDate) { selectedDate = it; b.date.text = host.getString(R.string.date_label) + ": " + date(it); refresh() } }
            b.attach.setOnClickListener {
                host.pickReceipt { uri ->
                    if (uri != null && alert.isShowing) dialogScope?.launch {
                        importing = true; refresh()
                        try { receipt = ReceiptFiles.import(host, uri); receiptFailed = false }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { receiptFailed = true }
                        finally { importing = false; if (alert.isShowing) refresh() }
                    }
                }
            }
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val item = candidate()
                if (FinanceRules.valid(item) && !importing) {
                    if (existing == null) host.model.addTransaction(item) else host.model.updateTransaction(item)
                    alert.dismiss()
                }
            }
            refresh()
        }
    }
    fun budget(existing: Budget? = null) {
        val b = DialogBudgetBinding.inflate(host.layoutInflater)
        val original = existing ?: Budget(category = "", amount = 0.0, period = "Monthly")
        val periods = listOf("Weekly", "Monthly", "Yearly")
        b.category.setText(original.category)
        b.amount.setText(existing?.amount?.toString().orEmpty())
        spinner(b.period, listOf(R.string.weekly, R.string.monthly, R.string.yearly), periods.indexOf(original.period).coerceAtLeast(0))
        fun candidate() = original.copy(category = b.category.text.toString().trim(),
            amount = FinanceRules.positiveAmount(b.amount.text.toString()) ?: 0.0, period = periods[b.period.selectedItemPosition])
        show(if (existing == null) R.string.add_budget else R.string.edit_budget, b.root) { alert ->
            fun refresh() {
                val valid = FinanceRules.valid(candidate())
                alert.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = valid
                b.error.isVisible = !valid
            }
            watch(listOf(b.category, b.amount), ::refresh)
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val item = candidate()
                if (FinanceRules.valid(item)) {
                    if (existing == null) host.model.addBudget(item) else host.model.updateBudget(item)
                    alert.dismiss()
                }
            }
            refresh()
        }
    }
    fun goal(existing: SavingsGoal? = null) {
        val b = DialogGoalBinding.inflate(host.layoutInflater)
        val original = existing ?: SavingsGoal(name = "", targetAmount = 0.0, targetDate = System.currentTimeMillis() + 30L * 86400000, description = "")
        var selectedDate = original.targetDate
        b.name.setText(original.name)
        b.target.setText(existing?.targetAmount?.toString().orEmpty())
        b.current.setText(original.currentAmount.toString())
        b.description.setText(original.description)
        b.date.text = host.getString(R.string.deadline_value, date(selectedDate))
        fun candidate() = original.copy(name = b.name.text.toString().trim(),
            targetAmount = FinanceRules.positiveAmount(b.target.text.toString()) ?: 0.0,
            currentAmount = b.current.text.toString().replace(',', '.').toDoubleOrNull() ?: -1.0,
            targetDate = selectedDate, description = b.description.text.toString())
        show(if (existing == null) R.string.add_goal else R.string.edit_goal, b.root) { alert ->
            fun refresh() {
                val valid = FinanceRules.valid(candidate())
                alert.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = valid
                b.error.isVisible = !valid
            }
            watch(listOf(b.name, b.target, b.current, b.description), ::refresh)
            b.date.setOnClickListener { chooseDate(selectedDate) { selectedDate = it; b.date.text = host.getString(R.string.deadline_value, date(it)); refresh() } }
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val item = candidate()
                if (FinanceRules.valid(item)) {
                    if (existing == null) host.model.addGoal(item) else host.model.updateGoal(item)
                    alert.dismiss()
                }
            }
            refresh()
        }
    }
}
