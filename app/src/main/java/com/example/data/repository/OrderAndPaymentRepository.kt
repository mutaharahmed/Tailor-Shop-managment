package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.OrderWithSummary
import com.example.data.local.dao.PaymentWithOrderInfo
import com.example.data.local.entity.CustomOptionPair
import com.example.data.local.entity.CustomPreferenceOptionEntity
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionEntity
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.local.entity.PreferenceGroup
import com.example.data.local.entity.TailoringOrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PaymentPostResult {
    data class Success(val payment: PaymentTransactionEntity, val wasIdempotentReplay: Boolean = false) : PaymentPostResult()
    data class RequiresDuplicateConfirmation(val existingCount: Int) : PaymentPostResult()
    data class Error(val message: String) : PaymentPostResult()
}

data class OrderDraftInput(
    val customerId: Long,
    val measurementProfileId: Long?,
    val garmentType: GarmentType,
    val customGarmentName: String,
    val quantity: Int,
    val collarStyle: String,
    val frontDesign: String,
    val cuffStyle: String,
    val pocketStyle: String,
    val sleeveStyle: String,
    val bottomStyle: String,
    val fitStyle: String,
    val embroideryStyle: String,
    val customOptions: List<CustomOptionPair>,
    val additionalInstructions: String,
    val stitchingPricePkr: Long,
    val initialAdvancePkr: Long,
    val advancePaymentMethod: PaymentMethod,
    val orderDate: Long,
    val deliveryDate: Long,
    val status: OrderStatus,
    val advanceIdempotencyKey: String
)

class OrderAndPaymentRepository(
    private val dbProvider: () -> AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun db() = dbProvider()

    fun observeAllOrders(): Flow<List<OrderWithSummary>> =
        db().orderDao().observeAllOrdersWithSummary()

    fun observeCustomerOrders(customerId: Long): Flow<List<OrderWithSummary>> =
        db().orderDao().observeCustomerOrdersWithSummary(customerId)

    fun observeOrder(orderId: Long): Flow<OrderWithSummary?> =
        db().orderDao().observeOrderWithSummary(orderId)

    fun observePaymentsForOrder(orderId: Long): Flow<List<PaymentWithOrderInfo>> =
        db().paymentDao().observePaymentsForOrder(orderId)

    fun observePaymentsForCustomer(customerId: Long): Flow<List<PaymentWithOrderInfo>> =
        db().paymentDao().observePaymentsForCustomer(customerId)

    fun observeAllPayments(): Flow<List<PaymentWithOrderInfo>> =
        db().paymentDao().observeAllPayments()

    fun observePaymentById(paymentId: Long): Flow<PaymentWithOrderInfo?> =
        db().paymentDao().observePaymentById(paymentId)

    fun observeCustomOptions(): Flow<List<CustomPreferenceOptionEntity>> =
        db().preferenceOptionDao().observeAll()

    suspend fun saveCustomPreferenceOption(group: PreferenceGroup, value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) return
        db().preferenceOptionDao().insert(
            CustomPreferenceOptionEntity(
                preferenceGroup = group,
                optionValue = clean,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun createOrder(input: OrderDraftInput): Result<TailoringOrderEntity> {
        if (input.customerId <= 0L) return Result.failure(IllegalArgumentException("Customer is required"))
        if (input.quantity <= 0) return Result.failure(IllegalArgumentException("Quantity must be at least 1"))
        if (input.stitchingPricePkr < 0L) return Result.failure(IllegalArgumentException("Price cannot be negative"))
        if (input.initialAdvancePkr < 0L) return Result.failure(IllegalArgumentException("Advance cannot be negative"))
        if (input.initialAdvancePkr > input.stitchingPricePkr) {
            return Result.failure(IllegalArgumentException("Advance cannot exceed total stitching price"))
        }

        return runCatching {
            db().withTransaction {
                val now = System.currentTimeMillis()
                val orderDao = db().orderDao()
                val paymentDao = db().paymentDao()
                val nextSeq = orderDao.getMaxId() + 1L
                val datePrefix = SimpleDateFormat("yyyyMM", Locale.US).format(Date(input.orderDate))
                val orderNo = "AT-$datePrefix-${nextSeq.toString().padStart(4, '0')}"

                val order = TailoringOrderEntity(
                    orderNumber = orderNo,
                    customerId = input.customerId,
                    measurementProfileId = input.measurementProfileId,
                    garmentType = input.garmentType,
                    customGarmentName = if (input.garmentType == GarmentType.CUSTOM) input.customGarmentName.trim() else "",
                    quantity = input.quantity,
                    collarStyle = input.collarStyle.trim(),
                    frontDesign = input.frontDesign.trim(),
                    cuffStyle = input.cuffStyle.trim(),
                    pocketStyle = input.pocketStyle.trim(),
                    sleeveStyle = input.sleeveStyle.trim(),
                    bottomStyle = input.bottomStyle.trim(),
                    fitStyle = input.fitStyle.trim(),
                    embroideryStyle = input.embroideryStyle.trim(),
                    customDesignOptionsJson = json.encodeToString(
                        input.customOptions.filter { it.name.isNotBlank() && it.value.isNotBlank() }
                    ),
                    additionalInstructions = input.additionalInstructions.trim(),
                    stitchingPricePkr = input.stitchingPricePkr,
                    orderDate = input.orderDate,
                    deliveryDate = input.deliveryDate,
                    status = input.status,
                    createdAt = now,
                    updatedAt = now
                )
                val orderId = orderDao.insert(order)

                if (input.initialAdvancePkr > 0L) {
                    val existing = paymentDao.getByIdempotencyKey(input.advanceIdempotencyKey)
                    if (existing == null) {
                        val nextPaySeq = paymentDao.getMaxId() + 1L
                        val rcpNo = "RCP-$datePrefix-${nextPaySeq.toString().padStart(4, '0')}"
                        paymentDao.insert(
                            PaymentTransactionEntity(
                                receiptNumber = rcpNo,
                                orderId = orderId,
                                customerId = input.customerId,
                                transactionType = PaymentTransactionType.ADVANCE,
                                method = input.advancePaymentMethod,
                                amountPkr = input.initialAdvancePkr,
                                isDeduction = false,
                                idempotencyKey = input.advanceIdempotencyKey,
                                note = "Initial advance for $orderNo",
                                paidAt = now,
                                createdAt = now
                            )
                        )
                    }
                }
                order.copy(id = orderId)
            }
        }
    }

    suspend fun updateOrderDetails(
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
        status: OrderStatus
    ): Result<TailoringOrderEntity> {
        if (quantity <= 0) return Result.failure(IllegalArgumentException("Quantity must be at least 1"))
        if (stitchingPricePkr < 0L) return Result.failure(IllegalArgumentException("Price cannot be negative"))

        return runCatching {
            db().withTransaction {
                val dao = db().orderDao()
                val existing = dao.getById(orderId) ?: throw IllegalStateException("Order not found")
                val updated = existing.copy(
                    measurementProfileId = measurementProfileId,
                    quantity = quantity,
                    collarStyle = collarStyle.trim(),
                    frontDesign = frontDesign.trim(),
                    cuffStyle = cuffStyle.trim(),
                    pocketStyle = pocketStyle.trim(),
                    sleeveStyle = sleeveStyle.trim(),
                    bottomStyle = bottomStyle.trim(),
                    fitStyle = fitStyle.trim(),
                    embroideryStyle = embroideryStyle.trim(),
                    customDesignOptionsJson = json.encodeToString(
                        customOptions.filter { it.name.isNotBlank() && it.value.isNotBlank() }
                    ),
                    additionalInstructions = additionalInstructions.trim(),
                    stitchingPricePkr = stitchingPricePkr,
                    deliveryDate = deliveryDate,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated)
                updated
            }
        }
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): Result<Unit> {
        return runCatching {
            db().orderDao().updateStatus(orderId, newStatus, System.currentTimeMillis())
        }
    }

    /**
     * Posts a financial transaction atomically with idempotency protection and
     * optional recent-similar-payment confirmation guard so legitimate installments
     * are allowed while accidental double-submissions are prevented.
     */
    suspend fun postPaymentTransaction(
        orderId: Long,
        transactionType: PaymentTransactionType,
        method: PaymentMethod,
        otherMethodLabel: String,
        amountPkr: Long,
        note: String,
        idempotencyKey: String,
        referenceTransactionId: Long? = null,
        isDeductionOverride: Boolean? = null,
        bypassRecentDuplicateCheck: Boolean = false
    ): PaymentPostResult {
        if (amountPkr <= 0L) {
            return PaymentPostResult.Error("Amount must be greater than zero")
        }
        if (method == PaymentMethod.OTHER && otherMethodLabel.trim().isEmpty()) {
            return PaymentPostResult.Error("Please specify the payment method")
        }

        return try {
            db().withTransaction {
                val paymentDao = db().paymentDao()
                val orderDao = db().orderDao()

                // 1. Exact idempotency key replay check (prevents double-tap / retry duplicates)
                val existingIdempotent = paymentDao.getByIdempotencyKey(idempotencyKey)
                if (existingIdempotent != null) {
                    return@withTransaction PaymentPostResult.Success(
                        payment = existingIdempotent,
                        wasIdempotentReplay = true
                    )
                }

                val order = orderDao.getById(orderId)
                    ?: return@withTransaction PaymentPostResult.Error("Order not found")

                val isDeduction = isDeductionOverride ?: (transactionType == PaymentTransactionType.REFUND)

                if (isDeduction) {
                    val netPaid = paymentDao.getNetPaidForOrder(orderId)
                    if (amountPkr > netPaid) {
                        return@withTransaction PaymentPostResult.Error(
                            "Refund/deduction cannot exceed net collected amount (PKR $netPaid)"
                        )
                    }
                } else if (!bypassRecentDuplicateCheck) {
                    // 2. Soft guard: check if an identical payment was posted within last 60 seconds
                    val sixtySecondsAgo = System.currentTimeMillis() - 60_000L
                    val recentSimilar = paymentDao.countRecentSimilarPayments(
                        orderId = orderId,
                        amountPkr = amountPkr,
                        method = method,
                        sinceMillis = sixtySecondsAgo
                    )
                    if (recentSimilar > 0) {
                        return@withTransaction PaymentPostResult.RequiresDuplicateConfirmation(recentSimilar)
                    }
                }

                val now = System.currentTimeMillis()
                val nextPaySeq = paymentDao.getMaxId() + 1L
                val datePrefix = SimpleDateFormat("yyyyMM", Locale.US).format(Date(now))
                val rcpNo = "RCP-$datePrefix-${nextPaySeq.toString().padStart(4, '0')}"

                val entity = PaymentTransactionEntity(
                    receiptNumber = rcpNo,
                    orderId = orderId,
                    customerId = order.customerId,
                    transactionType = transactionType,
                    method = method,
                    otherMethodLabel = if (method == PaymentMethod.OTHER) otherMethodLabel.trim() else "",
                    amountPkr = amountPkr,
                    isDeduction = isDeduction,
                    referenceTransactionId = referenceTransactionId,
                    idempotencyKey = idempotencyKey,
                    note = note.trim(),
                    paidAt = now,
                    createdAt = now
                )
                val id = paymentDao.insert(entity)
                PaymentPostResult.Success(entity.copy(id = id), wasIdempotentReplay = false)
            }
        } catch (e: Exception) {
            PaymentPostResult.Error(e.message ?: "Failed to record payment")
        }
    }

    fun decodeCustomOptions(jsonStr: String): List<CustomOptionPair> {
        return try {
            json.decodeFromString<List<CustomOptionPair>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
