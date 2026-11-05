package com.example.budgetwisesa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetwisesa.data.model.*
import java.security.MessageDigest

@Database(entities = [Transaction::class, Budget::class, SavingsGoal::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        private val instances = mutableMapOf<String, AppDatabase>()

        // Separate files prevent guest/account data leaking across sessions.
        // The prototype's unowned budget_wise_db is preserved, never assigned to an arbitrary user.
        @Synchronized
        fun getDatabase(context: Context, account: String): AppDatabase = instances.getOrPut(account) {
            val suffix = MessageDigest.getInstance("SHA-256").digest(account.toByteArray())
                .joinToString("") { "%02x".format(it) }
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "budget_wise_v2_$suffix").build()
        }
    }
}
