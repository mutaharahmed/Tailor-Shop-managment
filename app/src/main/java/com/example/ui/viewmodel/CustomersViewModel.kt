package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.dao.CustomerSummaryTuple
import com.example.data.local.dao.OrderWithSummary
import com.example.data.local.dao.PaymentWithOrderInfo
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementFieldValue
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.MeasurementUnit
import com.example.data.repository.CustomerRepository
import com.example.data.repository.DuplicateCheckResult
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.OrderAndPaymentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val orders: List<OrderWithSummary> = emptyList(),
    val measurements: List<MeasurementProfileEntity> = emptyList(),
    val payments: List<PaymentWithOrderInfo> = emptyList(),
    val totalBilledPkr: Long = 0L,
    val totalPaidPkr: Long = 0L
) {
    val outstandingPkr: Long
        get() = (totalBilledPkr - totalPaidPkr).coerceAtLeast(0L)
}

@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModel(
    private val customerRepo: CustomerRepository,
    val measurementRepo: MeasurementRepository,
    private val orderRepo: OrderAndPaymentRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val currentSearchQuery: StateFlow<String> = searchQuery

    val customers: StateFlow<List<CustomerSummaryTuple>> = combine(
        backupManager.databaseEpoch.flatMapLatest { customerRepo.observeCustomerSummaries() },
        searchQuery
    ) { list, q ->
        val trimmed = q.trim().lowercase()
        if (trimmed.isEmpty()) {
            list
        } else {
            list.filter {
                it.customer.name.lowercase().contains(trimmed) ||
                    it.customer.phone.lowercase().contains(trimmed) ||
                    it.customer.customerCode.lowercase().contains(trimmed)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun observeCustomerDetail(customerId: Long): Flow<CustomerDetailUiState> {
        return backupManager.databaseEpoch.flatMapLatest {
            combine(
                customerRepo.observeCustomer(customerId),
                orderRepo.observeCustomerOrders(customerId),
                measurementRepo.observeCustomerMeasurements(customerId),
                orderRepo.observePaymentsForCustomer(customerId)
            ) { customer, orders, measurements, payments ->
                val billed = orders
                    .filter { it.order.status != com.example.data.local.entity.OrderStatus.CANCELLED }
                    .sumOf { it.order.stitchingPricePkr }
                val paid = payments.sumOf { it.netEffectPkr }
                CustomerDetailUiState(
                    customer = customer,
                    orders = orders,
                    measurements = measurements,
                    payments = payments,
                    totalBilledPkr = billed,
                    totalPaidPkr = paid
                )
            }
        }
    }

    suspend fun checkDuplicates(name: String, phone: String, excludeId: Long = -1L): DuplicateCheckResult {
        return customerRepo.checkPossibleDuplicates(name, phone, excludeId)
    }

    fun saveCustomer(
        editingId: Long?,
        name: String,
        phone: String,
        address: String,
        notes: String,
        onResult: (Result<CustomerEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (editingId == null) {
                customerRepo.createCustomer(name, phone, address, notes)
            } else {
                customerRepo.updateCustomer(editingId, name, phone, address, notes)
            }
            onResult(result)
        }
    }

    fun saveNewMeasurementVersion(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String,
        unit: MeasurementUnit,
        fields: List<MeasurementFieldValue>,
        notes: String,
        onResult: (Result<MeasurementProfileEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val result = measurementRepo.saveNewVersion(
                customerId = customerId,
                garmentType = garmentType,
                customGarmentName = customGarmentName,
                unit = unit,
                fields = fields,
                notes = notes
            )
            onResult(result)
        }
    }
}
