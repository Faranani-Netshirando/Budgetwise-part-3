package com.example.budgetwisesa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val date: Long, // Using Long for simplicity with Room/JSON
    val description: String,
    val receiptPath: String? = null,
    val isSynced: Boolean = false,
    val deleted: Boolean = false,
    val revision: String = UUID.randomUUID().toString()
)

enum class TransactionType {
    INCOME, EXPENSE
}
