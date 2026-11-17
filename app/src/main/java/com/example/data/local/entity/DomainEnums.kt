package com.example.data.local.entity

import kotlinx.serialization.Serializable

enum class GarmentType {
    SHALWAR_KAMEEZ,
    SHIRT,
    PANT_TROUSER,
    WAISTCOAT,
    COAT_JACKET,
    KURTA,
    CUSTOM
}

enum class MeasurementUnit {
    INCHES,
    CENTIMETERS
}

enum class OrderStatus {
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    READY,
    DELIVERED,
    CANCELLED
}

enum class PaymentTransactionType {
    ADVANCE,
    INSTALLMENT,
    FINAL_SETTLEMENT,
    REFUND,
    ADJUSTMENT
}

enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    MOBILE_WALLET,
    CARD,
    OTHER
}

enum class StockMovementType {
    INITIAL_STOCK,
    RESTOCK,
    SALE,
    RETURN,
    ADJUSTMENT
}

enum class ExpenseCategory {
    RENT,
    ELECTRICITY,
    SUPPLIES,
    WAGES,
    MAINTENANCE,
    HOSPITALITY,
    OTHER
}

enum class PreferenceGroup {
    COLLAR,
    FRONT,
    CUFFS,
    POCKETS,
    SLEEVES,
    BOTTOM,
    FIT,
    EMBROIDERY,
    INVENTORY_CATEGORY
}

@Serializable
data class MeasurementFieldValue(
    val key: String,
    val label: String,
    val value: String,
    val isCustom: Boolean = false
)

@Serializable
data class CustomOptionPair(
    val name: String,
    val value: String
)
