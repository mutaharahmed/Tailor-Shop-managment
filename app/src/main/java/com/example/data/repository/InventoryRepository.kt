package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.StockMovementWithProduct
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StockMovementType
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val dbProvider: () -> AppDatabase
) {
    private fun db() = dbProvider()

    fun observeActiveProducts(): Flow<List<ProductEntity>> =
        db().inventoryDao().observeActiveProducts()

    fun observeProduct(productId: Long): Flow<ProductEntity?> =
        db().inventoryDao().observeProduct(productId)

    fun observeLowStockProducts(): Flow<List<ProductEntity>> =
        db().inventoryDao().observeLowStockProducts()

    fun observeCategories(): Flow<List<String>> =
        db().inventoryDao().observeDistinctCategories()

    fun observeMovementsForProduct(productId: Long): Flow<List<StockMovementWithProduct>> =
        db().inventoryDao().observeMovementsForProduct(productId)

    fun observeAllMovements(): Flow<List<StockMovementWithProduct>> =
        db().inventoryDao().observeAllMovements()

    suspend fun createProduct(
        name: String,
        category: String,
        size: String,
        color: String,
        costPricePkr: Long,
        sellingPricePkr: Long,
        initialQuantity: Int,
        lowStockThreshold: Int,
        idempotencyKey: String
    ): Result<ProductEntity> {
        val cleanName = name.trim()
        val cleanCategory = category.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Product name is required"))
        if (cleanCategory.isEmpty()) return Result.failure(IllegalArgumentException("Category is required"))
        if (costPricePkr < 0L || sellingPricePkr < 0L) return Result.failure(IllegalArgumentException("Price cannot be negative"))
        if (initialQuantity < 0) return Result.failure(IllegalArgumentException("Quantity cannot be negative"))

        return runCatching {
            db().withTransaction {
                val dao = db().inventoryDao()
                val now = System.currentTimeMillis()
                val nextSeq = dao.getMaxProductId() + 1L
                val sku = "AT-P-${nextSeq.toString().padStart(4, '0')}"

                val product = ProductEntity(
                    sku = sku,
                    name = cleanName,
                    category = cleanCategory,
                    size = size.trim(),
                    color = color.trim(),
                    costPricePkr = costPricePkr,
                    sellingPricePkr = sellingPricePkr,
                    quantity = initialQuantity,
                    lowStockThreshold = lowStockThreshold.coerceAtLeast(0),
                    createdAt = now,
                    updatedAt = now
                )
                val id = dao.insertProduct(product)

                if (initialQuantity > 0) {
                    dao.insertMovement(
                        StockMovementEntity(
                            productId = id,
                            movementType = StockMovementType.INITIAL_STOCK,
                            quantityDelta = initialQuantity,
                            quantityAfter = initialQuantity,
                            unitPricePkr = costPricePkr,
                            totalAmountPkr = costPricePkr * initialQuantity,
                            paymentMethod = null,
                            idempotencyKey = idempotencyKey,
                            note = "Initial stock added",
                            createdAt = now
                        )
                    )
                }
                product.copy(id = id)
            }
        }
    }

    suspend fun updateProductInfo(
        productId: Long,
        name: String,
        category: String,
        size: String,
        color: String,
        costPricePkr: Long,
        sellingPricePkr: Long,
        lowStockThreshold: Int
    ): Result<ProductEntity> {
        val cleanName = name.trim()
        val cleanCategory = category.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Product name is required"))
        if (cleanCategory.isEmpty()) return Result.failure(IllegalArgumentException("Category is required"))
        if (costPricePkr < 0L || sellingPricePkr < 0L) return Result.failure(IllegalArgumentException("Price cannot be negative"))

        return runCatching {
            db().withTransaction {
                val dao = db().inventoryDao()
                val existing = dao.getProductById(productId) ?: throw IllegalStateException("Product not found")
                val updated = existing.copy(
                    name = cleanName,
                    category = cleanCategory,
                    size = size.trim(),
                    color = color.trim(),
                    costPricePkr = costPricePkr,
                    sellingPricePkr = sellingPricePkr,
                    lowStockThreshold = lowStockThreshold.coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateProduct(updated)
                updated
            }
        }
    }

    suspend fun recordStockMovement(
        productId: Long,
        movementType: StockMovementType,
        units: Int,
        unitPricePkr: Long,
        paymentMethod: PaymentMethod?,
        note: String,
        idempotencyKey: String,
        isNegativeAdjustment: Boolean = false
    ): Result<StockMovementEntity> {
        if (units <= 0) return Result.failure(IllegalArgumentException("Quantity must be greater than zero"))
        if (unitPricePkr < 0L) return Result.failure(IllegalArgumentException("Price cannot be negative"))

        return runCatching {
            db().withTransaction {
                val dao = db().inventoryDao()
                val existingReplay = dao.getMovementByIdempotencyKey(idempotencyKey)
                if (existingReplay != null) {
                    return@withTransaction existingReplay
                }

                val product = dao.getProductById(productId) ?: throw IllegalStateException("Product not found")
                val delta = when (movementType) {
                    StockMovementType.SALE -> -units
                    StockMovementType.RESTOCK, StockMovementType.RETURN, StockMovementType.INITIAL_STOCK -> units
                    StockMovementType.ADJUSTMENT -> if (isNegativeAdjustment) -units else units
                }
                val newQty = product.quantity + delta
                if (newQty < 0) {
                    throw IllegalStateException("Insufficient stock (${product.quantity} remaining)")
                }

                val now = System.currentTimeMillis()
                dao.updateProduct(product.copy(quantity = newQty, updatedAt = now))

                val movement = StockMovementEntity(
                    productId = productId,
                    movementType = movementType,
                    quantityDelta = delta,
                    quantityAfter = newQty,
                    unitPricePkr = unitPricePkr,
                    totalAmountPkr = unitPricePkr * units,
                    paymentMethod = if (movementType == StockMovementType.SALE || movementType == StockMovementType.RETURN) {
                        paymentMethod ?: PaymentMethod.CASH
                    } else {
                        null
                    },
                    idempotencyKey = idempotencyKey,
                    note = note.trim(),
                    createdAt = now
                )
                val id = dao.insertMovement(movement)
                movement.copy(id = id)
            }
        }
    }
}
