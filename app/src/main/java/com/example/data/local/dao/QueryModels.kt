package com.example.data.local.dao

import androidx.room.Embedded
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TailoringOrderEntity

data class CustomerSummaryTuple(
    @Embedded val customer: CustomerEntity,
    val orderCount: Int,
    val totalBilledPkr: Long,
    val totalPaidPkr: Long
) {
    val outstandingPkr: Long
        get() = (totalBilledPkr - totalPaidPkr).coerceAtLeast(0L)
}

data class OrderWithSummary(
    @Embedded val order: TailoringOrderEntity,
    val customerName: String,
    val customerPhone: String,
    val customerCode: String,
    val measurementVersion: Int?,
    val netPaidPkr: Long
) {
    val remainingBalancePkr: Long
        get() = if (order.status == OrderStatus.CANCELLED) 0L else (order.stitchingPricePkr - netPaidPkr).coerceAtLeast(0L)

    fun isOverdue(nowMillis: Long): Boolean {
        return order.status != OrderStatus.DELIVERED &&
            order.status != OrderStatus.CANCELLED &&
            order.status != OrderStatus.READY &&
            order.deliveryDate < nowMillis
    }
}

data class PaymentWithOrderInfo(
    val id: Long,
    val receiptNumber: String,
    val orderId: Long,
    val orderNumber: String,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val transactionType: com.example.data.local.entity.PaymentTransactionType,
    val method: com.example.data.local.entity.PaymentMethod,
    val otherMethodLabel: String,
    val amountPkr: Long,
    val isDeduction: Boolean,
    val referenceTransactionId: Long?,
    val idempotencyKey: String,
    val note: String,
    val paidAt: Long
) {
    val netEffectPkr: Long
        get() = if (isDeduction) -amountPkr else amountPkr
}

data class StockMovementWithProduct(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productSku: String,
    val movementType: com.example.data.local.entity.StockMovementType,
    val quantityDelta: Int,
    val quantityAfter: Int,
    val unitPricePkr: Long,
    val totalAmountPkr: Long,
    val paymentMethod: com.example.data.local.entity.PaymentMethod?,
    val note: String,
    val createdAt: Long
)

data class OrderDetailBundle(
    val order: OrderWithSummary,
    val customer: CustomerEntity,
    val measurement: MeasurementProfileEntity?,
    val payments: List<PaymentWithOrderInfo>
)

data class InventoryValuationSummary(
    val totalUnits: Int,
    val totalCostValuePkr: Long,
    val totalRetailValuePkr: Long,
    val lowStockCount: Int,
    val outOfStockCount: Int
)

data class ProductSummaryRow(
    @Embedded val product: ProductEntity,
    val isLowStock: Boolean
)
