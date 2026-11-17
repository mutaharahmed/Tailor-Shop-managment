package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    fun observeProduct(productId: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE isArchived = 0 AND quantity <= lowStockThreshold ORDER BY quantity ASC, name ASC")
    fun observeLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT category FROM products WHERE isArchived = 0 ORDER BY category ASC")
    fun observeDistinctCategories(): Flow<List<String>>

    @Query("SELECT COALESCE(MAX(id), 0) FROM products")
    suspend fun getMaxProductId(): Long

    @Query("SELECT COUNT(*) FROM products WHERE isArchived = 0")
    suspend fun countActiveProducts(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query(
        """
        SELECT m.id, m.productId, p.name AS productName, p.sku AS productSku,
               m.movementType, m.quantityDelta, m.quantityAfter,
               m.unitPricePkr, m.totalAmountPkr, m.paymentMethod, m.note, m.createdAt
        FROM stock_movements m
        INNER JOIN products p ON p.id = m.productId
        WHERE m.productId = :productId
        ORDER BY m.createdAt DESC, m.id DESC
        """
    )
    fun observeMovementsForProduct(productId: Long): Flow<List<StockMovementWithProduct>>

    @Query(
        """
        SELECT m.id, m.productId, p.name AS productName, p.sku AS productSku,
               m.movementType, m.quantityDelta, m.quantityAfter,
               m.unitPricePkr, m.totalAmountPkr, m.paymentMethod, m.note, m.createdAt
        FROM stock_movements m
        INNER JOIN products p ON p.id = m.productId
        ORDER BY m.createdAt DESC, m.id DESC
        """
    )
    fun observeAllMovements(): Flow<List<StockMovementWithProduct>>

    @Query("SELECT * FROM stock_movements WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getMovementByIdempotencyKey(idempotencyKey: String): StockMovementEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovementEntity): Long
}
