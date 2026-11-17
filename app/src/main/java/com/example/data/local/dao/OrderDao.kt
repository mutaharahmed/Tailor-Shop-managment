package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.TailoringOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query(
        """
        SELECT o.*,
               c.name AS customerName,
               c.phone AS customerPhone,
               c.customerCode AS customerCode,
               m.versionNumber AS measurementVersion,
               COALESCE((SELECT SUM(CASE WHEN p.isDeduction = 1 THEN -p.amountPkr ELSE p.amountPkr END)
                         FROM payment_transactions p WHERE p.orderId = o.id), 0) AS netPaidPkr
        FROM tailoring_orders o
        INNER JOIN customers c ON c.id = o.customerId
        LEFT JOIN measurement_profiles m ON m.id = o.measurementProfileId
        ORDER BY o.orderDate DESC, o.id DESC
        """
    )
    fun observeAllOrdersWithSummary(): Flow<List<OrderWithSummary>>

    @Query(
        """
        SELECT o.*,
               c.name AS customerName,
               c.phone AS customerPhone,
               c.customerCode AS customerCode,
               m.versionNumber AS measurementVersion,
               COALESCE((SELECT SUM(CASE WHEN p.isDeduction = 1 THEN -p.amountPkr ELSE p.amountPkr END)
                         FROM payment_transactions p WHERE p.orderId = o.id), 0) AS netPaidPkr
        FROM tailoring_orders o
        INNER JOIN customers c ON c.id = o.customerId
        LEFT JOIN measurement_profiles m ON m.id = o.measurementProfileId
        WHERE o.customerId = :customerId
        ORDER BY o.orderDate DESC, o.id DESC
        """
    )
    fun observeCustomerOrdersWithSummary(customerId: Long): Flow<List<OrderWithSummary>>

    @Query(
        """
        SELECT o.*,
               c.name AS customerName,
               c.phone AS customerPhone,
               c.customerCode AS customerCode,
               m.versionNumber AS measurementVersion,
               COALESCE((SELECT SUM(CASE WHEN p.isDeduction = 1 THEN -p.amountPkr ELSE p.amountPkr END)
                         FROM payment_transactions p WHERE p.orderId = o.id), 0) AS netPaidPkr
        FROM tailoring_orders o
        INNER JOIN customers c ON c.id = o.customerId
        LEFT JOIN measurement_profiles m ON m.id = o.measurementProfileId
        WHERE o.id = :orderId
        """
    )
    fun observeOrderWithSummary(orderId: Long): Flow<OrderWithSummary?>

    @Query("SELECT * FROM tailoring_orders WHERE id = :orderId")
    suspend fun getById(orderId: Long): TailoringOrderEntity?

    @Query("SELECT COALESCE(MAX(id), 0) FROM tailoring_orders")
    suspend fun getMaxId(): Long

    @Query("SELECT COUNT(*) FROM tailoring_orders")
    suspend fun countOrders(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: TailoringOrderEntity): Long

    @Update
    suspend fun update(order: TailoringOrderEntity)

    @Query("UPDATE tailoring_orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, status: OrderStatus, updatedAt: Long)
}
