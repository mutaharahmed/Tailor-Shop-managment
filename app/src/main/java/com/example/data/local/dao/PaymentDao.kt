package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query(
        """
        SELECT p.id, p.receiptNumber, p.orderId, o.orderNumber, p.customerId,
               c.name AS customerName, c.phone AS customerPhone,
               p.transactionType, p.method, p.otherMethodLabel, p.amountPkr,
               p.isDeduction, p.referenceTransactionId, p.idempotencyKey, p.note, p.paidAt
        FROM payment_transactions p
        INNER JOIN tailoring_orders o ON o.id = p.orderId
        INNER JOIN customers c ON c.id = p.customerId
        WHERE p.orderId = :orderId
        ORDER BY p.paidAt DESC, p.id DESC
        """
    )
    fun observePaymentsForOrder(orderId: Long): Flow<List<PaymentWithOrderInfo>>

    @Query(
        """
        SELECT p.id, p.receiptNumber, p.orderId, o.orderNumber, p.customerId,
               c.name AS customerName, c.phone AS customerPhone,
               p.transactionType, p.method, p.otherMethodLabel, p.amountPkr,
               p.isDeduction, p.referenceTransactionId, p.idempotencyKey, p.note, p.paidAt
        FROM payment_transactions p
        INNER JOIN tailoring_orders o ON o.id = p.orderId
        INNER JOIN customers c ON c.id = p.customerId
        WHERE p.customerId = :customerId
        ORDER BY p.paidAt DESC, p.id DESC
        """
    )
    fun observePaymentsForCustomer(customerId: Long): Flow<List<PaymentWithOrderInfo>>

    @Query(
        """
        SELECT p.id, p.receiptNumber, p.orderId, o.orderNumber, p.customerId,
               c.name AS customerName, c.phone AS customerPhone,
               p.transactionType, p.method, p.otherMethodLabel, p.amountPkr,
               p.isDeduction, p.referenceTransactionId, p.idempotencyKey, p.note, p.paidAt
        FROM payment_transactions p
        INNER JOIN tailoring_orders o ON o.id = p.orderId
        INNER JOIN customers c ON c.id = p.customerId
        ORDER BY p.paidAt DESC, p.id DESC
        """
    )
    fun observeAllPayments(): Flow<List<PaymentWithOrderInfo>>

    @Query(
        """
        SELECT p.id, p.receiptNumber, p.orderId, o.orderNumber, p.customerId,
               c.name AS customerName, c.phone AS customerPhone,
               p.transactionType, p.method, p.otherMethodLabel, p.amountPkr,
               p.isDeduction, p.referenceTransactionId, p.idempotencyKey, p.note, p.paidAt
        FROM payment_transactions p
        INNER JOIN tailoring_orders o ON o.id = p.orderId
        INNER JOIN customers c ON c.id = p.customerId
        WHERE p.id = :paymentId
        """
    )
    fun observePaymentById(paymentId: Long): Flow<PaymentWithOrderInfo?>

    @Query("SELECT * FROM payment_transactions WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getByIdempotencyKey(idempotencyKey: String): PaymentTransactionEntity?

    @Query(
        """
        SELECT COUNT(*) FROM payment_transactions
        WHERE orderId = :orderId
          AND amountPkr = :amountPkr
          AND method = :method
          AND isDeduction = 0
          AND paidAt >= :sinceMillis
        """
    )
    suspend fun countRecentSimilarPayments(
        orderId: Long,
        amountPkr: Long,
        method: PaymentMethod,
        sinceMillis: Long
    ): Int

    @Query("SELECT COALESCE(SUM(CASE WHEN isDeduction = 1 THEN -amountPkr ELSE amountPkr END), 0) FROM payment_transactions WHERE orderId = :orderId")
    suspend fun getNetPaidForOrder(orderId: Long): Long

    @Query("SELECT COALESCE(MAX(id), 0) FROM payment_transactions")
    suspend fun getMaxId(): Long

    @Query("SELECT COUNT(*) FROM payment_transactions")
    suspend fun countPayments(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentTransactionEntity): Long
}
