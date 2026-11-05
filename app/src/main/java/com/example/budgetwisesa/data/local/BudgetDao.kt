package com.example.budgetwisesa.data.local

import androidx.room.*
import com.example.budgetwisesa.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT COUNT(*) FROM budgets WHERE isSynced = 0")
    fun pendingCount(): Flow<Int>
    @Query("SELECT * FROM budgets WHERE deleted = 0 ORDER BY category")
    fun getAllBudgets(): Flow<List<Budget>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(item: Budget)
    @Query("SELECT * FROM budgets WHERE isSynced = 0")
    suspend fun pending(): List<Budget>
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun find(id: String): Budget?
    @Query("UPDATE budgets SET isSynced = 1 WHERE id = :id AND revision = :revision")
    suspend fun acknowledge(id: String, revision: String)
}
