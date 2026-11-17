package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementUnit
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.local.entity.PreferenceGroup
import com.example.data.local.entity.StockMovementType

class Converters {
    @TypeConverter fun fromGarmentType(v: GarmentType): String = v.name
    @TypeConverter fun toGarmentType(v: String): GarmentType = GarmentType.valueOf(v)

    @TypeConverter fun fromMeasurementUnit(v: MeasurementUnit): String = v.name
    @TypeConverter fun toMeasurementUnit(v: String): MeasurementUnit = MeasurementUnit.valueOf(v)

    @TypeConverter fun fromOrderStatus(v: OrderStatus): String = v.name
    @TypeConverter fun toOrderStatus(v: String): OrderStatus = OrderStatus.valueOf(v)

    @TypeConverter fun fromPaymentType(v: PaymentTransactionType): String = v.name
    @TypeConverter fun toPaymentType(v: String): PaymentTransactionType = PaymentTransactionType.valueOf(v)

    @TypeConverter fun fromPaymentMethod(v: PaymentMethod?): String? = v?.name
    @TypeConverter fun toPaymentMethod(v: String?): PaymentMethod? = v?.let { PaymentMethod.valueOf(it) }

    @TypeConverter fun fromStockMovementType(v: StockMovementType): String = v.name
    @TypeConverter fun toStockMovementType(v: String): StockMovementType = StockMovementType.valueOf(v)

    @TypeConverter fun fromExpenseCategory(v: ExpenseCategory): String = v.name
    @TypeConverter fun toExpenseCategory(v: String): ExpenseCategory = ExpenseCategory.valueOf(v)

    @TypeConverter fun fromPreferenceGroup(v: PreferenceGroup): String = v.name
    @TypeConverter fun toPreferenceGroup(v: String): PreferenceGroup = PreferenceGroup.valueOf(v)
}
