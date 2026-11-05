package com.example.budgetwisesa.data.local

import androidx.room.*
import com.example.budgetwisesa.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun pendingCount(): Flow<Int>
    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(item: Transaction)
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun pending(): List<Transaction>
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun find(id: String): Transaction?
    @Query("UPDATE transactions SET isSynced = 1, receiptPath = :receipt WHERE id = :id AND revision = :revision")
    suspend fun acknowledge(id: String, revision: String, receipt: String?)
}
