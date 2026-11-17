package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupInspectResult
import com.example.data.backup.BackupKind
import com.example.data.backup.BackupManager
import com.example.data.backup.LocalBackupEntry
import com.example.data.backup.RestorePreviewData
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.StockMovementType
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.OrderAndPaymentRepository
import com.example.util.ShopFormatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME
}

data class BusinessReportSummary(
    val period: ReportPeriod = ReportPeriod.MONTHLY,
    val tailoringOrdersCount: Int = 0,
    val tailoringOrdersValuePkr: Long = 0L,
    val tailoringPaymentsCollectedPkr: Long = 0L,
    val readyMadeSalesPkr: Long = 0L,
    val readyMadeUnitsSold: Int = 0,
    val totalExpensesPkr: Long = 0L,
    val netCashSummaryPkr: Long = 0L,
    val totalOutstandingBalancesPkr: Long = 0L,
    val pendingOrdersCount: Int = 0,
    val overdueOrdersCount: Int = 0,
    val inventoryTotalUnits: Int = 0,
    val inventoryCostValuePkr: Long = 0L,
    val inventoryRetailValuePkr: Long = 0L,
    val lowStockProductCount: Int = 0,
    val expensesByCategory: Map<ExpenseCategory, Long> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceAndBackupViewModel(
    private val expenseRepo: ExpenseRepository,
    private val orderRepo: OrderAndPaymentRepository,
    private val inventoryRepo: InventoryRepository,
    private val prefsRepo: ShopPreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val expensePeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    val currentExpensePeriod: StateFlow<ReportPeriod> = expensePeriod.asStateFlow()

    private val reportPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    val currentReportPeriod: StateFlow<ReportPeriod> = reportPeriod.asStateFlow()

    val expensesForPeriod: StateFlow<List<ExpenseEntity>> = combine(
        backupManager.databaseEpoch.flatMapLatest { expenseRepo.observeAllExpenses() },
        expensePeriod
    ) { list, period ->
        val start = periodStartMillis(period)
        list.filter { it.expenseDate >= start }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSummary: StateFlow<BusinessReportSummary> = combine(
        backupManager.databaseEpoch.flatMapLatest { orderRepo.observeAllOrders() },
        backupManager.databaseEpoch.flatMapLatest { orderRepo.observeAllPayments() },
        backupManager.databaseEpoch.flatMapLatest { inventoryRepo.observeAllMovements() },
        backupManager.databaseEpoch.flatMapLatest { inventoryRepo.observeActiveProducts() },
        backupManager.databaseEpoch.flatMapLatest { expenseRepo.observeAllExpenses() },
        reportPeriod
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val orders = values[0] as List<com.example.data.local.dao.OrderWithSummary>
        @Suppress("UNCHECKED_CAST")
        val payments = values[1] as List<com.example.data.local.dao.PaymentWithOrderInfo>
        @Suppress("UNCHECKED_CAST")
        val movements = values[2] as List<com.example.data.local.dao.StockMovementWithProduct>
        @Suppress("UNCHECKED_CAST")
        val products = values[3] as List<com.example.data.local.entity.ProductEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[4] as List<ExpenseEntity>
        val period = values[5] as ReportPeriod

        val now = System.currentTimeMillis()
        val start = periodStartMillis(period, now)

        val periodOrders = orders.filter {
            it.order.orderDate >= start && it.order.status != OrderStatus.CANCELLED
        }
        val periodPayments = payments.filter { it.paidAt >= start }.sumOf { it.netEffectPkr }

        val periodSalesMovements = movements.filter {
            it.movementType == StockMovementType.SALE && it.createdAt >= start
        }
        val periodReturnsMovements = movements.filter {
            it.movementType == StockMovementType.RETURN && it.createdAt >= start
        }
        val netReadyMadeSales = periodSalesMovements.sumOf { it.totalAmountPkr } -
            periodReturnsMovements.sumOf { it.totalAmountPkr }
        val unitsSold = periodSalesMovements.sumOf { -it.quantityDelta }

        val periodExpenses = expenses.filter { it.expenseDate >= start }
        val totalExpenses = periodExpenses.sumOf { it.amountPkr }
        val expenseBreakdown = periodExpenses
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amountPkr } }

        val allActiveNonCancelled = orders.filter { it.order.status != OrderStatus.CANCELLED }
        val totalOutstanding = allActiveNonCancelled.sumOf { it.remainingBalancePkr }
        val pendingOrders = allActiveNonCancelled.count {
            it.order.status != OrderStatus.DELIVERED
        }
        val overdueOrders = allActiveNonCancelled.count { it.isOverdue(now) }

        val totalUnits = products.sumOf { it.quantity }
        val costValue = products.sumOf { it.costPricePkr * it.quantity }
        val retailValue = products.sumOf { it.sellingPricePkr * it.quantity }
        val lowStockCount = products.count { it.quantity <= it.lowStockThreshold }

        BusinessReportSummary(
            period = period,
            tailoringOrdersCount = periodOrders.size,
            tailoringOrdersValuePkr = periodOrders.sumOf { it.order.stitchingPricePkr },
            tailoringPaymentsCollectedPkr = periodPayments,
            readyMadeSalesPkr = netReadyMadeSales,
            readyMadeUnitsSold = unitsSold,
            totalExpensesPkr = totalExpenses,
            netCashSummaryPkr = periodPayments + netReadyMadeSales - totalExpenses,
            totalOutstandingBalancesPkr = totalOutstanding,
            pendingOrdersCount = pendingOrders,
            overdueOrdersCount = overdueOrders,
            inventoryTotalUnits = totalUnits,
            inventoryCostValuePkr = costValue,
            inventoryRetailValuePkr = retailValue,
            lowStockProductCount = lowStockCount,
            expensesByCategory = expenseBreakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessReportSummary())

    private val _localBackups = MutableStateFlow<List<LocalBackupEntry>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupEntry>> = _localBackups.asStateFlow()

    private val _availableStorageBytes = MutableStateFlow(backupManager.getAvailableStorageBytes())
    val availableStorageBytes: StateFlow<Long> = _availableStorageBytes.asStateFlow()

    private val _isLowStorage = MutableStateFlow(backupManager.isLowStorageWarning())
    val isLowStorage: StateFlow<Boolean> = _isLowStorage.asStateFlow()

    private val _restorePreview = MutableStateFlow<RestorePreviewData?>(null)
    val restorePreview: StateFlow<RestorePreviewData?> = _restorePreview.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    init {
        refreshBackupState()
    }

    fun setExpensePeriod(period: ReportPeriod) {
        expensePeriod.value = period
    }

    fun setReportPeriod(period: ReportPeriod) {
        reportPeriod.value = period
    }

    fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        expenseDate: Long,
        note: String,
        onResult: (Result<ExpenseEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val res = expenseRepo.addExpense(title, category, amountPkr, paymentMethod, expenseDate, note)
            onResult(res)
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepo.deleteExpense(expenseId)
        }
    }

    fun refreshBackupState() {
        viewModelScope.launch {
            _localBackups.value = backupManager.listLocalBackups()
            _availableStorageBytes.value = backupManager.getAvailableStorageBytes()
            _isLowStorage.value = backupManager.isLowStorageWarning()
        }
    }

    fun createManualBackup(onComplete: (Result<LocalBackupEntry>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val res = backupManager.createLocalBackup(BackupKind.MANUAL)
            refreshBackupState()
            _backupBusy.value = false
            onComplete(res)
        }
    }

    fun exportBackupToUri(uri: Uri, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val res = backupManager.exportBackupToUri(uri)
            refreshBackupState()
            _backupBusy.value = false
            onComplete(res)
        }
    }

    fun inspectUriForRestore(uri: Uri, displayName: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            when (val res = backupManager.inspectBackupFromUri(uri, displayName)) {
                is BackupInspectResult.Valid -> _restorePreview.value = res.preview
                is BackupInspectResult.Corrupted -> onError(res.reason)
            }
            _backupBusy.value = false
        }
    }

    fun inspectLocalForRestore(entry: LocalBackupEntry, onError: (String) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            when (val res = backupManager.inspectLocalBackup(entry)) {
                is BackupInspectResult.Valid -> _restorePreview.value = res.preview
                is BackupInspectResult.Corrupted -> onError(res.reason)
            }
            _backupBusy.value = false
        }
    }

    fun dismissRestorePreview() {
        backupManager.cleanupStagedPreview(_restorePreview.value)
        _restorePreview.value = null
    }

    fun confirmRestore(onComplete: (Result<Unit>) -> Unit) {
        val preview = _restorePreview.value ?: return
        viewModelScope.launch {
            _backupBusy.value = true
            val res = backupManager.performRestore(preview)
            _restorePreview.value = null
            refreshBackupState()
            _backupBusy.value = false
            onComplete(res)
        }
    }

    fun deleteBackup(entry: LocalBackupEntry, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.deleteLocalBackup(entry)
            refreshBackupState()
            onComplete(res)
        }
    }

    private fun periodStartMillis(period: ReportPeriod, now: Long = System.currentTimeMillis()): Long {
        return when (period) {
            ReportPeriod.DAILY -> ShopFormatters.startOfTodayMillis(now)
            ReportPeriod.WEEKLY -> ShopFormatters.startOfDaysAgoMillis(6, now)
            ReportPeriod.MONTHLY -> ShopFormatters.startOfMonthMillis(now)
            ReportPeriod.ALL_TIME -> 0L
        }
    }
}
