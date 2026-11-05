package com.example.budgetwisesa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val description: String,
    val isSynced: Boolean = false,
    val deleted: Boolean = false,
    val revision: String = UUID.randomUUID().toString()
)
