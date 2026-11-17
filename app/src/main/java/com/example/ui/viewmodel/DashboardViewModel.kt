package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.dao.OrderWithSummary
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.ProductEntity
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.OrderAndPaymentRepository
import com.example.util.ShopFormatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val todaysOrdersCount: Int = 0,
    val inProgressCount: Int = 0,
    val readyOrdersCount: Int = 0,
    val overdueOrdersCount: Int = 0,
    val todaysPaymentsPkr: Long = 0L,
    val outstandingBalancePkr: Long = 0L,
    val todaysExpensesPkr: Long = 0L,
    val monthlyExpensesPkr: Long = 0L,
    val lowStockCount: Int = 0,
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val recentActiveOrders: List<OrderWithSummary> = emptyList(),
    val showBackupAlert: Boolean = false,
    val showLowStorageAlert: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val orderRepo: OrderAndPaymentRepository,
    private val inventoryRepo: InventoryRepository,
    private val expenseRepo: ExpenseRepository,
    private val prefsRepo: ShopPreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = backupManager.databaseEpoch.flatMapLatest {
        combine(
            orderRepo.observeAllOrders(),
            orderRepo.observeAllPayments(),
            expenseRepo.observeAllExpenses(),
            inventoryRepo.observeLowStockProducts(),
            prefsRepo.settingsFlow
        ) { orders, payments, expenses, lowStock, settings ->
            val now = System.currentTimeMillis()
            val todayStart = ShopFormatters.startOfTodayMillis(now)
            val todayEnd = ShopFormatters.endOfTodayMillis(now)
            val monthStart = ShopFormatters.startOfMonthMillis(now)

            val todaysOrders = orders.count { it.order.orderDate in todayStart..todayEnd && it.order.status != OrderStatus.CANCELLED }
            val inProgress = orders.count { it.order.status == OrderStatus.IN_PROGRESS }
            val ready = orders.count { it.order.status == OrderStatus.READY }
            val overdue = orders.count { it.isOverdue(now) }

            val todaysPayments = payments
                .filter { it.paidAt in todayStart..todayEnd }
                .sumOf { it.netEffectPkr }

            val outstanding = orders
                .filter { it.order.status != OrderStatus.CANCELLED }
                .sumOf { it.remainingBalancePkr }

            val todaysExpenses = expenses
                .filter { it.expenseDate in todayStart..todayEnd }
                .sumOf { it.amountPkr }

            val monthExpenses = expenses
                .filter { it.expenseDate >= monthStart }
                .sumOf { it.amountPkr }

            val activeOrders = orders
                .filter { it.order.status != OrderStatus.DELIVERED && it.order.status != OrderStatus.CANCELLED }
                .sortedWith(compareByDescending<OrderWithSummary> { it.isOverdue(now) }.thenBy { it.order.deliveryDate })
                .take(6)

            val backupStale = settings.lastBackupTimestamp > 0L &&
                (now - settings.lastBackupTimestamp) > 3L * 24L * 60L * 60L * 1000L &&
                settings.backupSchedule.hours > 0L

            DashboardUiState(
                todaysOrdersCount = todaysOrders,
                inProgressCount = inProgress,
                readyOrdersCount = ready,
                overdueOrdersCount = overdue,
                todaysPaymentsPkr = todaysPayments,
                outstandingBalancePkr = outstanding,
                todaysExpensesPkr = todaysExpenses,
                monthlyExpensesPkr = monthExpenses,
                lowStockCount = lowStock.size,
                lowStockProducts = lowStock.take(5),
                recentActiveOrders = activeOrders,
                showBackupAlert = !settings.lastBackupStatusOk || backupStale,
                showLowStorageAlert = backupManager.isLowStorageWarning()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
