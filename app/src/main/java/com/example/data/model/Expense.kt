package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val currency: String,
    val originalText: String,
    val originalApp: String,
    val timestamp: Long,
    val isPending: Boolean = false,
    val parseError: String? = null,
    val engineUsed: String = "Desconocido"
)
