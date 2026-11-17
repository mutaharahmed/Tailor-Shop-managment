package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query(
        """
        SELECT c.*,
               COUNT(DISTINCT o.id) AS orderCount,
               COALESCE((SELECT SUM(o2.stitchingPricePkr) FROM tailoring_orders o2 WHERE o2.customerId = c.id AND o2.status != 'CANCELLED'), 0) AS totalBilledPkr,
               COALESCE((SELECT SUM(CASE WHEN p.isDeduction = 1 THEN -p.amountPkr ELSE p.amountPkr END) FROM payment_transactions p WHERE p.customerId = c.id), 0) AS totalPaidPkr
        FROM customers c
        LEFT JOIN tailoring_orders o ON o.customerId = c.id
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
        """
    )
    fun observeCustomerSummaries(): Flow<List<CustomerSummaryTuple>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun observeCustomer(customerId: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone AND id != :excludeId")
    suspend fun findByPhone(phone: String, excludeId: Long = -1L): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId")
    suspend fun findByExactName(name: String, excludeId: Long = -1L): List<CustomerEntity>

    @Query("SELECT COALESCE(MAX(id), 0) FROM customers")
    suspend fun getMaxId(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countCustomers(): Int
}
