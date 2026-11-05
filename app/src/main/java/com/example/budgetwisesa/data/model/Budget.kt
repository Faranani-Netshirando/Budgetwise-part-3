package com.example.budgetwisesa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val amount: Double,
    val period: String, // e.g., "Monthly", "Weekly"
    val spent: Double = 0.0,
    val isSynced: Boolean = false,
    val deleted: Boolean = false,
    val revision: String = UUID.randomUUID().toString()
)
