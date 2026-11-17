package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["phone"]) // Intentionally non-unique: family members may share phone numbers
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerCode: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "measurement_profiles",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(
            value = ["customerId", "garmentType", "customGarmentName", "versionNumber"],
            unique = true
        )
    ]
)
data class MeasurementProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val garmentType: GarmentType,
    val customGarmentName: String = "",
    val versionNumber: Int,
    val unit: MeasurementUnit,
    val fieldsJson: String,
    val notes: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "tailoring_orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MeasurementProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementProfileId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["customerId"]),
        Index(value = ["measurementProfileId"]),
        Index(value = ["status"]),
        Index(value = ["deliveryDate"]),
        Index(value = ["orderDate"])
    ]
)
data class TailoringOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val customerId: Long,
    val measurementProfileId: Long? = null,
    val garmentType: GarmentType,
    val customGarmentName: String = "",
    val quantity: Int = 1,
    val collarStyle: String = "",
    val frontDesign: String = "",
    val cuffStyle: String = "",
    val pocketStyle: String = "",
    val sleeveStyle: String = "",
    val bottomStyle: String = "",
    val fitStyle: String = "",
    val embroideryStyle: String = "",
    val customDesignOptionsJson: String = "[]",
    val additionalInstructions: String = "",
    val stitchingPricePkr: Long,
    val orderDate: Long,
    val deliveryDate: Long,
    val status: OrderStatus,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "payment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = TailoringOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["orderId"]),
        Index(value = ["customerId"]),
        Index(value = ["paidAt"])
    ]
)
data class PaymentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val orderId: Long,
    val customerId: Long,
    val transactionType: PaymentTransactionType,
    val method: PaymentMethod,
    val otherMethodLabel: String = "",
    val amountPkr: Long,
    val isDeduction: Boolean = false,
    val referenceTransactionId: Long? = null,
    val idempotencyKey: String,
    val note: String = "",
    val paidAt: Long,
    val createdAt: Long
) {
    val netEffectPkr: Long
        get() = if (isDeduction) -amountPkr else amountPkr
}

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["category"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String,
    val size: String = "",
    val color: String = "",
    val costPricePkr: Long,
    val sellingPricePkr: Long,
    val quantity: Int,
    val lowStockThreshold: Int = 5,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val movementType: StockMovementType,
    val quantityDelta: Int,
    val quantityAfter: Int,
    val unitPricePkr: Long,
    val totalAmountPkr: Long,
    val paymentMethod: PaymentMethod? = null,
    val idempotencyKey: String,
    val note: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["expenseDate"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: ExpenseCategory,
    val amountPkr: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val expenseDate: Long,
    val note: String = "",
    val createdAt: Long
)

@Entity(
    tableName = "custom_preference_options",
    indices = [
        Index(value = ["preferenceGroup", "optionValue"], unique = true)
    ]
)
data class CustomPreferenceOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val preferenceGroup: PreferenceGroup,
    val optionValue: String,
    val createdAt: Long
)
