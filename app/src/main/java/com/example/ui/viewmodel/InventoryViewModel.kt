package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.dao.StockMovementWithProduct
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PreferenceGroup
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.StockMovementType
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.repository.InventoryRepository
import com.example.data.repository.OrderAndPaymentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val inventoryRepo: InventoryRepository,
    private val orderRepo: OrderAndPaymentRepository,
    prefsRepo: ShopPreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("ALL")
    private val lowStockOnly = MutableStateFlow(false)

    val currentSearch: StateFlow<String> = searchQuery
    val currentCategory: StateFlow<String> = selectedCategory
    val isLowStockOnly: StateFlow<Boolean> = lowStockOnly

    val defaultThreshold: StateFlow<Int> = prefsRepo.settingsFlow
        .flatMapLatest { kotlinx.coroutines.flow.flowOf(it.defaultLowStockThreshold) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val categories: StateFlow<List<String>> = backupManager.databaseEpoch.flatMapLatest {
        combine(
            inventoryRepo.observeCategories(),
            orderRepo.observeCustomOptions()
        ) { dbCats, customOpts ->
            val defaults = listOf("Shirts", "Pants & Trousers", "Jackets", "Sweaters")
            val savedCustom = customOpts
                .filter { it.preferenceGroup == PreferenceGroup.INVENTORY_CATEGORY }
                .map { it.optionValue }
            (defaults + dbCats + savedCustom).distinct().sorted()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Shirts", "Pants & Trousers", "Jackets", "Sweaters"))

    val products: StateFlow<List<ProductEntity>> = combine(
        backupManager.databaseEpoch.flatMapLatest { inventoryRepo.observeActiveProducts() },
        searchQuery,
        selectedCategory,
        lowStockOnly
    ) { list, q, cat, lowOnly ->
        val trimmed = q.trim().lowercase()
        list.filter { item ->
            val matchQ = trimmed.isEmpty() ||
                item.name.lowercase().contains(trimmed) ||
                item.sku.lowercase().contains(trimmed) ||
                item.color.lowercase().contains(trimmed) ||
                item.size.lowercase().contains(trimmed)
            val matchCat = cat == "ALL" || item.category.equals(cat, ignoreCase = true)
            val matchLow = !lowOnly || item.quantity <= item.lowStockThreshold
            matchQ && matchCat && matchLow
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = backupManager.databaseEpoch.flatMapLatest {
        inventoryRepo.observeLowStockProducts()
    }.flatMapLatest { kotlinx.coroutines.flow.flowOf(it.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun setCategory(cat: String) {
        selectedCategory.value = cat
    }

    fun toggleLowStockOnly() {
        lowStockOnly.value = !lowStockOnly.value
    }

    fun observeProduct(productId: Long): Flow<ProductEntity?> =
        backupManager.databaseEpoch.flatMapLatest { inventoryRepo.observeProduct(productId) }

    fun observeProductMovements(productId: Long): Flow<List<StockMovementWithProduct>> =
        backupManager.databaseEpoch.flatMapLatest { inventoryRepo.observeMovementsForProduct(productId) }

    fun createProduct(
        name: String,
        category: String,
        size: String,
        color: String,
        costPricePkr: Long,
        sellingPricePkr: Long,
        initialQuantity: Int,
        lowStockThreshold: Int,
        idempotencyKey: String,
        onResult: (Result<ProductEntity>) -> Unit
    ) {
        viewModelScope.launch {
            orderRepo.saveCustomPreferenceOption(PreferenceGroup.INVENTORY_CATEGORY, category)
            val res = inventoryRepo.createProduct(
                name = name,
                category = category,
                size = size,
                color = color,
                costPricePkr = costPricePkr,
                sellingPricePkr = sellingPricePkr,
                initialQuantity = initialQuantity,
                lowStockThreshold = lowStockThreshold,
                idempotencyKey = idempotencyKey
            )
            onResult(res)
        }
    }

    fun updateProduct(
        productId: Long,
        name: String,
        category: String,
        size: String,
        color: String,
        costPricePkr: Long,
        sellingPricePkr: Long,
        lowStockThreshold: Int,
        onResult: (Result<ProductEntity>) -> Unit
    ) {
        viewModelScope.launch {
            orderRepo.saveCustomPreferenceOption(PreferenceGroup.INVENTORY_CATEGORY, category)
            val res = inventoryRepo.updateProductInfo(
                productId = productId,
                name = name,
                category = category,
                size = size,
                color = color,
                costPricePkr = costPricePkr,
                sellingPricePkr = sellingPricePkr,
                lowStockThreshold = lowStockThreshold
            )
            onResult(res)
        }
    }

    fun recordMovement(
        productId: Long,
        type: StockMovementType,
        units: Int,
        unitPricePkr: Long,
        paymentMethod: PaymentMethod?,
        note: String,
        idempotencyKey: String,
        isNegativeAdjustment: Boolean = false,
        onResult: (Result<StockMovementEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val res = inventoryRepo.recordStockMovement(
                productId = productId,
                movementType = type,
                units = units,
                unitPricePkr = unitPricePkr,
                paymentMethod = paymentMethod,
                note = note,
                idempotencyKey = idempotencyKey,
                isNegativeAdjustment = isNegativeAdjustment
            )
            onResult(res)
        }
    }
}
