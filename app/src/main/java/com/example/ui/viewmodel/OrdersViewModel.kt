package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.dao.CustomerSummaryTuple
import com.example.data.local.dao.OrderDetailBundle
import com.example.data.local.dao.OrderWithSummary
import com.example.data.local.dao.PaymentWithOrderInfo
import com.example.data.local.entity.CustomOptionPair
import com.example.data.local.entity.CustomPreferenceOptionEntity
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.local.entity.PreferenceGroup
import com.example.data.local.entity.TailoringOrderEntity
import com.example.data.repository.CustomerRepository
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.OrderAndPaymentRepository
import com.example.data.repository.OrderDraftInput
import com.example.data.repository.PaymentPostResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OrderFilter {
    ALL,
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    READY,
    DELIVERED,
    CANCELLED,
    OVERDUE
}

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModel(
    val orderRepo: OrderAndPaymentRepository,
    private val customerRepo: CustomerRepository,
    val measurementRepo: MeasurementRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(OrderFilter.ALL)

    val currentSearchQuery: StateFlow<String> = searchQuery
    val currentFilter: StateFlow<OrderFilter> = selectedFilter

    val allCustomers: StateFlow<List<CustomerSummaryTuple>> =
        backupManager.databaseEpoch.flatMapLatest { customerRepo.observeCustomerSummaries() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPreferenceOptions: StateFlow<List<CustomPreferenceOptionEntity>> =
        backupManager.databaseEpoch.flatMapLatest { orderRepo.observeCustomOptions() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredOrders: StateFlow<List<OrderWithSummary>> = combine(
        backupManager.databaseEpoch.flatMapLatest { orderRepo.observeAllOrders() },
        searchQuery,
        selectedFilter
    ) { orders, q, filter ->
        val now = System.currentTimeMillis()
        val trimmed = q.trim().lowercase()
        orders.filter { item ->
            val matchesSearch = trimmed.isEmpty() ||
                item.order.orderNumber.lowercase().contains(trimmed) ||
                item.customerName.lowercase().contains(trimmed) ||
                item.customerPhone.lowercase().contains(trimmed)
            val matchesFilter = when (filter) {
                OrderFilter.ALL -> true
                OrderFilter.DRAFT -> item.order.status == OrderStatus.DRAFT
                OrderFilter.CONFIRMED -> item.order.status == OrderStatus.CONFIRMED
                OrderFilter.IN_PROGRESS -> item.order.status == OrderStatus.IN_PROGRESS
                OrderFilter.READY -> item.order.status == OrderStatus.READY
                OrderFilter.DELIVERED -> item.order.status == OrderStatus.DELIVERED
                OrderFilter.CANCELLED -> item.order.status == OrderStatus.CANCELLED
                OrderFilter.OVERDUE -> item.isOverdue(now)
            }
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun setFilter(filter: OrderFilter) {
        selectedFilter.value = filter
    }

    fun observeOrderDetail(orderId: Long): Flow<OrderDetailBundle?> {
        return backupManager.databaseEpoch.flatMapLatest {
            orderRepo.observeOrder(orderId).flatMapLatest { summary ->
                if (summary == null) {
                    flowOf(null)
                } else {
                    val measFlow = if (summary.order.measurementProfileId != null) {
                        measurementRepo.observeMeasurement(summary.order.measurementProfileId)
                    } else {
                        flowOf(null)
                    }
                    combine(
                        customerRepo.observeCustomer(summary.order.customerId),
                        measFlow,
                        orderRepo.observePaymentsForOrder(orderId)
                    ) { customer, meas, payments ->
                        if (customer == null) null else OrderDetailBundle(summary, customer, meas, payments)
                    }
                }
            }
        }
    }

    fun observePaymentReceipt(paymentId: Long): Flow<PaymentWithOrderInfo?> {
        return backupManager.databaseEpoch.flatMapLatest {
            orderRepo.observePaymentById(paymentId)
        }
    }

    suspend fun getLatestMeasurementForCustomerGarment(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String
    ): MeasurementProfileEntity? {
        return measurementRepo.getLatestForGarment(customerId, garmentType, customGarmentName)
    }

    fun observeCustomerMeasurements(customerId: Long): Flow<List<MeasurementProfileEntity>> {
        return backupManager.databaseEpoch.flatMapLatest {
            measurementRepo.observeCustomerMeasurements(customerId)
        }
    }

    fun createOrder(
        input: OrderDraftInput,
        onResult: (Result<TailoringOrderEntity>) -> Unit
    ) {
        viewModelScope.launch {
            saveCustomOptionsUsed(input)
            val result = orderRepo.createOrder(input)
            onResult(result)
        }
    }

    fun updateOrder(
        orderId: Long,
        measurementProfileId: Long?,
        quantity: Int,
        collarStyle: String,
        frontDesign: String,
        cuffStyle: String,
        pocketStyle: String,
        sleeveStyle: String,
        bottomStyle: String,
        fitStyle: String,
        embroideryStyle: String,
        customOptions: List<CustomOptionPair>,
        additionalInstructions: String,
        stitchingPricePkr: Long,
        deliveryDate: Long,
        status: OrderStatus,
        onResult: (Result<TailoringOrderEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val result = orderRepo.updateOrderDetails(
                orderId = orderId,
                measurementProfileId = measurementProfileId,
                quantity = quantity,
                collarStyle = collarStyle,
                frontDesign = frontDesign,
                cuffStyle = cuffStyle,
                pocketStyle = pocketStyle,
                sleeveStyle = sleeveStyle,
                bottomStyle = bottomStyle,
                fitStyle = fitStyle,
                embroideryStyle = embroideryStyle,
                customOptions = customOptions,
                additionalInstructions = additionalInstructions,
                stitchingPricePkr = stitchingPricePkr,
                deliveryDate = deliveryDate,
                status = status
            )
            onResult(result)
        }
    }

    private suspend fun saveCustomOptionsUsed(input: OrderDraftInput) {
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.COLLAR, input.collarStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.FRONT, input.frontDesign)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.CUFFS, input.cuffStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.POCKETS, input.pocketStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.SLEEVES, input.sleeveStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.BOTTOM, input.bottomStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.FIT, input.fitStyle)
        orderRepo.saveCustomPreferenceOption(PreferenceGroup.EMBROIDERY, input.embroideryStyle)
    }

    fun updateStatus(orderId: Long, newStatus: OrderStatus) {
        viewModelScope.launch {
            orderRepo.updateOrderStatus(orderId, newStatus)
        }
    }

    fun postPayment(
        orderId: Long,
        transactionType: PaymentTransactionType,
        method: PaymentMethod,
        otherMethodLabel: String,
        amountPkr: Long,
        note: String,
        idempotencyKey: String,
        referenceTransactionId: Long? = null,
        bypassDuplicateCheck: Boolean = false,
        onResult: (PaymentPostResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = orderRepo.postPaymentTransaction(
                orderId = orderId,
                transactionType = transactionType,
                method = method,
                otherMethodLabel = otherMethodLabel,
                amountPkr = amountPkr,
                note = note,
                idempotencyKey = idempotencyKey,
                referenceTransactionId = referenceTransactionId,
                bypassRecentDuplicateCheck = bypassDuplicateCheck
            )
            onResult(result)
        }
    }
}
