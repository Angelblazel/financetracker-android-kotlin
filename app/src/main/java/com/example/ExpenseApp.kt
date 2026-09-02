package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository

class ExpenseApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ExpenseRepository(database.expenseDao(), this) }

    override fun onCreate() {
        super.onCreate()
    }
}
