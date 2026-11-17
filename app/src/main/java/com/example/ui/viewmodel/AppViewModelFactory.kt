package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.AppContainer

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainSettingsViewModel::class.java) ->
                MainSettingsViewModel(container.preferencesRepository, container.backupManager) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(
                    orderRepo = container.orderAndPaymentRepository,
                    inventoryRepo = container.inventoryRepository,
                    expenseRepo = container.expenseRepository,
                    prefsRepo = container.preferencesRepository,
                    backupManager = container.backupManager
                ) as T
            modelClass.isAssignableFrom(CustomersViewModel::class.java) ->
                CustomersViewModel(
                    customerRepo = container.customerRepository,
                    measurementRepo = container.measurementRepository,
                    orderRepo = container.orderAndPaymentRepository,
                    backupManager = container.backupManager
                ) as T
            modelClass.isAssignableFrom(OrdersViewModel::class.java) ->
                OrdersViewModel(
                    orderRepo = container.orderAndPaymentRepository,
                    customerRepo = container.customerRepository,
                    measurementRepo = container.measurementRepository,
                    backupManager = container.backupManager
                ) as T
            modelClass.isAssignableFrom(InventoryViewModel::class.java) ->
                InventoryViewModel(
                    inventoryRepo = container.inventoryRepository,
                    orderRepo = container.orderAndPaymentRepository,
                    prefsRepo = container.preferencesRepository,
                    backupManager = container.backupManager
                ) as T
            modelClass.isAssignableFrom(FinanceAndBackupViewModel::class.java) ->
                FinanceAndBackupViewModel(
                    expenseRepo = container.expenseRepository,
                    orderRepo = container.orderAndPaymentRepository,
                    inventoryRepo = container.inventoryRepository,
                    prefsRepo = container.preferencesRepository,
                    backupManager = container.backupManager
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
