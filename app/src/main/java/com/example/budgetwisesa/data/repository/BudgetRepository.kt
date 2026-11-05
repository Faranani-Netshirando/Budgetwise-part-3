package com.example.budgetwisesa.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.budgetwisesa.data.local.AppDatabase
import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.data.remote.CloudStore
import com.example.budgetwisesa.domain.FinanceRules
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BudgetRepository(context: Context, val account: String) {
    private val database = AppDatabase.getDatabase(context, account)
    private val transactions = database.transactionDao()
    private val budgets = database.budgetDao()
    private val goals = database.savingsGoalDao()
    val allTransactions = transactions.getAllTransactions()
    val allBudgets = budgets.getAllBudgets()
    val allGoals = goals.getAllGoals()
    val pendingCount = combine(transactions.pendingCount(), budgets.pendingCount(), goals.pendingCount()) { t, b, g -> t + b + g }

    suspend fun save(item: Transaction) {
        require(FinanceRules.valid(item))
        transactions.insertTransaction(item.copy(category = item.category.trim(), isSynced = false, revision = UUID.randomUUID().toString()))
    }
    suspend fun save(item: Budget) {
        require(FinanceRules.valid(item))
        budgets.insertBudget(item.copy(category = item.category.trim(), spent = 0.0, isSynced = false, revision = UUID.randomUUID().toString()))
    }
    suspend fun save(item: SavingsGoal) {
        require(FinanceRules.valid(item))
        goals.insertGoal(item.copy(name = item.name.trim(), isSynced = false, revision = UUID.randomUUID().toString()))
    }

    // Tombstones stay in Room and Firestore, so an offline deletion can reach every device.
    suspend fun delete(item: Transaction) = save(item.copy(deleted = true))
    suspend fun delete(item: Budget) = save(item.copy(deleted = true))
    suspend fun delete(item: SavingsGoal) = save(item.copy(deleted = true))

    suspend fun sync() = locks.getOrPut(account) { Mutex() }.withLock {
        require(account != GUEST)
        val cloud = CloudStore(account)
        for (item in transactions.pending()) {
            val uploaded = cloud.uploadReceipt(item)
            cloud.put("transactions", item.id, uploaded)
            // Only acknowledge the revision sent: editing while a request runs must remain pending.
            transactions.acknowledge(item.id, item.revision, item.receiptPath)
        }
        for (item in budgets.pending()) {
            cloud.put("budgets", item.id, item)
            budgets.acknowledge(item.id, item.revision)
        }
        for (item in goals.pending()) {
            cloud.put("goals", item.id, item)
            goals.acknowledge(item.id, item.revision)
        }
        val remoteTransactions = cloud.list("transactions", Transaction::class.java)
        val remoteBudgets = cloud.list("budgets", Budget::class.java)
        val remoteGoals = cloud.list("goals", SavingsGoal::class.java)
        database.withTransaction {
            for (item in remoteTransactions) {
                require(FinanceRules.valid(item))
                val local = transactions.find(item.id)
                if (local?.isSynced != false) {
                    val receipt = local?.receiptPath?.takeIf { local.revision == item.revision && it.startsWith("file://") }
                    transactions.insertTransaction(if (receipt != null) item.copy(receiptPath = receipt) else item)
                }
            }
            for (item in remoteBudgets) {
                require(FinanceRules.valid(item))
                if (budgets.find(item.id)?.isSynced != false) budgets.insertBudget(item)
            }
            for (item in remoteGoals) {
                require(FinanceRules.valid(item))
                if (goals.find(item.id)?.isSynced != false) goals.insertGoal(item)
            }
        }
    }

    companion object {
        const val GUEST = "local-guest"
        private val locks = ConcurrentHashMap<String, Mutex>()
    }
}
