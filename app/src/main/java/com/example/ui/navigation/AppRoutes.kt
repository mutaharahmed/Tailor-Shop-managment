package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object DashboardRoute
@Serializable object OrdersRoute
@Serializable data class OrderDetailRoute(val orderId: Long)
@Serializable data class OrderFormRoute(
    val editOrderId: Long = -1L,
    val preselectedCustomerId: Long = -1L
)
@Serializable data class ReceiptRoute(val paymentId: Long)

@Serializable object CustomersRoute
@Serializable data class CustomerDetailRoute(val customerId: Long)
@Serializable data class CustomerFormRoute(val editCustomerId: Long = -1L)
@Serializable data class MeasurementEditorRoute(
    val customerId: Long,
    val preselectedGarmentName: String = "",
    val baseProfileId: Long = -1L
)

@Serializable object InventoryRoute
@Serializable data class ProductDetailRoute(val productId: Long)

@Serializable object MoreRoute
@Serializable object ExpensesRoute
@Serializable object ReportsRoute
@Serializable object BackupRoute
@Serializable object SettingsRoute
