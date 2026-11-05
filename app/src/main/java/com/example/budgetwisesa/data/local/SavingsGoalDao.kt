package com.example.budgetwisesa.data.local

import androidx.room.*
import com.example.budgetwisesa.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT COUNT(*) FROM savings_goals WHERE isSynced = 0")
    fun pendingCount(): Flow<Int>
    @Query("SELECT * FROM savings_goals WHERE deleted = 0 ORDER BY targetDate")
    fun getAllGoals(): Flow<List<SavingsGoal>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(item: SavingsGoal)
    @Query("SELECT * FROM savings_goals WHERE isSynced = 0")
    suspend fun pending(): List<SavingsGoal>
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun find(id: String): SavingsGoal?
    @Query("UPDATE savings_goals SET isSynced = 1 WHERE id = :id AND revision = :revision")
    suspend fun acknowledge(id: String, revision: String)
}
