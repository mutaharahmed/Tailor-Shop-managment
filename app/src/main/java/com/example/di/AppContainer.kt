package com.example.di

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.OrderAndPaymentRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val dbProvider: () -> AppDatabase = {
        AppDatabase.getInstance(appContext)
    }

    val preferencesRepository = ShopPreferencesRepository(appContext)
    val backupManager = BackupManager(appContext, preferencesRepository)

    val customerRepository = CustomerRepository(dbProvider)
    val measurementRepository = MeasurementRepository(dbProvider)
    val orderAndPaymentRepository = OrderAndPaymentRepository(dbProvider)
    val inventoryRepository = InventoryRepository(dbProvider)
    val expenseRepository = ExpenseRepository(dbProvider)
}
