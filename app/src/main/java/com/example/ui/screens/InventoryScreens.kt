package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LtrMonoText
import com.example.ui.components.SearchInputField
import com.example.ui.components.ShopTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.WarningBanner
import com.example.ui.components.paymentMethodLabel
import com.example.ui.components.stockMovementLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.CraftSage
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.ShopFormatters
import java.util.UUID

@Composable
fun InventoryListScreen(
    viewModel: InventoryViewModel,
    onProductClick: (Long) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val query by viewModel.currentSearch.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.currentCategory.collectAsStateWithLifecycle()
    val lowStockOnly by viewModel.isLowStockOnly.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val defaultThreshold by viewModel.defaultThreshold.collectAsStateWithLifecycle()

    var showProductForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var movementTarget by remember { mutableStateOf<Pair<ProductEntity, StockMovementType>?>(null) }
    val savedMsg = stringResource(R.string.label_saved_successfully)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductForm = true
                },
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.inventory_add_product))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Text(
                    text = stringResource(R.string.inventory_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
                if (lowStockCount > 0) {
                    WarningBanner(
                        icon = Icons.Default.Warning,
                        message = "${stringResource(R.string.dashboard_low_stock)}: $lowStockCount",
                        isError = false,
                        onClick = { viewModel.toggleLowStockOnly() }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                SearchInputField(
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = stringResource(R.string.inventory_search_hint),
                    testTag = "inventory_search_input"
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL" && !lowStockOnly,
                            onClick = {
                                viewModel.setCategory("ALL")
                                if (lowStockOnly) viewModel.toggleLowStockOnly()
                            },
                            label = { Text(stringResource(R.string.label_all)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = lowStockOnly,
                            onClick = { viewModel.toggleLowStockOnly() },
                            label = { Text("${stringResource(R.string.product_low_stock_badge)} ($lowStockCount)") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (products.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = stringResource(R.string.inventory_empty_title),
                    subtitle = stringResource(R.string.inventory_empty_subtitle),
                    actionLabel = stringResource(R.string.inventory_add_product),
                    onAction = {
                        editingProduct = null
                        showProductForm = true
                    },
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRowCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onSell = { movementTarget = product to StockMovementType.SALE },
                            onRestock = { movementTarget = product to StockMovementType.RESTOCK }
                        )
                    }
                }
            }
        }
    }

    if (showProductForm) {
        ProductFormDialog(
            existing = editingProduct,
            categories = categories,
            defaultLowStock = defaultThreshold,
            viewModel = viewModel,
            onDismiss = { showProductForm = false },
            onSaved = {
                showProductForm = false
                onShowMessage(savedMsg)
            }
        )
    }

    movementTarget?.let { (product, type) ->
        StockMovementDialog(
            product = product,
            movementType = type,
            viewModel = viewModel,
            onDismiss = { movementTarget = null },
            onSaved = {
                movementTarget = null
                onShowMessage(savedMsg)
            }
        )
    }
}

@Composable
private fun ProductRowCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onSell: () -> Unit,
    onRestock: () -> Unit
) {
    val isOut = product.quantity <= 0
    val isLow = product.quantity in 1..product.lowStockThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = listOf(product.category, product.size, product.color).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isOut) {
                    StatusPill(
                        text = stringResource(R.string.product_out_of_stock_badge),
                        containerColor = AlertCrimsonContainer,
                        contentColor = AlertCrimson
                    )
                } else if (isLow) {
                    StatusPill(
                        text = stringResource(R.string.product_low_stock_badge),
                        containerColor = WarningAmberContainer,
                        contentColor = WarningAmber
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrMonoText(
                    text = "${product.sku} • Qty: ${product.quantity}",
                    fontWeight = FontWeight.Bold,
                    color = if (isOut) AlertCrimson else MaterialTheme.colorScheme.onSurface
                )
                LtrMonoText(
                    text = ShopFormatters.formatCurrencyPkr(product.sellingPricePkr),
                    color = CraftSage,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSell,
                    enabled = product.quantity > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sell_product_${product.id}")
                ) {
                    Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.inventory_sell_btn))
                }
                OutlinedButton(
                    onClick = onRestock,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("restock_product_${product.id}")
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.inventory_restock_btn))
                }
            }
        }
    }
}

@Composable
fun ProductDetailScreen(
    productId: Long,
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val product by remember(productId) {
        viewModel.observeProduct(productId)
    }.collectAsStateWithLifecycle(initialValue = null)

    val movements by remember(productId) {
        viewModel.observeProductMovements(productId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var movementDialogType by remember { mutableStateOf<StockMovementType?>(null) }
    val savedMsg = stringResource(R.string.label_saved_successfully)

    Scaffold(
        topBar = {
            ShopTopBar(
                title = product?.name ?: stringResource(R.string.inventory_title),
                onBack = onBack,
                actions = {
                    if (product != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val p = product ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            LtrMonoText(p.sku, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            LtrMonoText("Qty: ${p.quantity}", fontWeight = FontWeight.Bold)
                        }
                        Text("${p.category} • ${p.size} • ${p.color}")
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.product_cost_price_label))
                            LtrMonoText(ShopFormatters.formatCurrencyPkr(p.costPricePkr))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.product_selling_price_label))
                            LtrMonoText(ShopFormatters.formatCurrencyPkr(p.sellingPricePkr), color = CraftSage, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { movementDialogType = StockMovementType.SALE },
                                enabled = p.quantity > 0,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(stringResource(R.string.inventory_sell_btn))
                            }
                            OutlinedButton(
                                onClick = { movementDialogType = StockMovementType.RESTOCK },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(stringResource(R.string.inventory_restock_btn))
                            }
                            OutlinedButton(
                                onClick = { movementDialogType = StockMovementType.ADJUSTMENT },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.inventory_adjust_btn))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.inventory_movements_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                )
            }

            items(movements, key = { it.id }) { mv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stockMovementLabel(mv.movementType), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = ShopFormatters.formatDateTime(mv.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (mv.note.isNotBlank()) {
                                Text(mv.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val sign = if (mv.quantityDelta > 0) "+${mv.quantityDelta}" else "${mv.quantityDelta}"
                            LtrMonoText(
                                text = "$sign (After: ${mv.quantityAfter})",
                                color = if (mv.quantityDelta > 0) CraftSage else AlertCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            LtrMonoText(ShopFormatters.formatCurrencyPkr(mv.totalAmountPkr))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && product != null) {
        ProductFormDialog(
            existing = product,
            categories = categories,
            defaultLowStock = product!!.lowStockThreshold,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
            onSaved = {
                showEditDialog = false
                onShowMessage(savedMsg)
            }
        )
    }

    if (movementDialogType != null && product != null) {
        StockMovementDialog(
            product = product!!,
            movementType = movementDialogType!!,
            viewModel = viewModel,
            onDismiss = { movementDialogType = null },
            onSaved = {
                movementDialogType = null
                onShowMessage(savedMsg)
            }
        )
    }
}

@Composable
private fun ProductFormDialog(
    existing: ProductEntity?,
    categories: List<String>,
    defaultLowStock: Int,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "Shirts") }
    var customCategory by rememberSaveable { mutableStateOf("") }
    var useCustomCategory by rememberSaveable { mutableStateOf(false) }
    var size by rememberSaveable { mutableStateOf(existing?.size ?: "") }
    var color by rememberSaveable { mutableStateOf(existing?.color ?: "") }
    var costText by rememberSaveable { mutableStateOf(existing?.costPricePkr?.toString() ?: "") }
    var sellText by rememberSaveable { mutableStateOf(existing?.sellingPricePkr?.toString() ?: "") }
    var qtyText by rememberSaveable { mutableStateOf(existing?.quantity?.toString() ?: "0") }
    var thresholdText by rememberSaveable { mutableStateOf((existing?.lowStockThreshold ?: defaultLowStock).toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val idempotencyKey = remember { UUID.randomUUID().toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (existing == null) R.string.inventory_add_product else R.string.inventory_edit_product
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(stringResource(R.string.product_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                Text(stringResource(R.string.product_category_label), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = !useCustomCategory && category == cat,
                            onClick = { useCustomCategory = false; category = cat },
                            label = { Text(cat) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = useCustomCategory,
                            onClick = { useCustomCategory = true },
                            label = { Text(stringResource(R.string.category_custom)) }
                        )
                    }
                }
                if (useCustomCategory) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text(stringResource(R.string.product_custom_category_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = { Text(stringResource(R.string.product_size_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text(stringResource(R.string.product_color_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text(stringResource(R.string.product_cost_price_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_cost_input")
                    )
                    OutlinedTextField(
                        value = sellText,
                        onValueChange = { sellText = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text(stringResource(R.string.product_selling_price_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sell_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (existing == null) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it.filter { c -> c.isDigit() }; errorMsg = null },
                            label = { Text(stringResource(R.string.product_quantity_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("product_qty_input")
                        )
                    }
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.product_low_stock_threshold)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                errorMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosenCat = if (useCustomCategory) customCategory.trim() else category
                    val cost = costText.toLongOrNull()
                    val sell = sellText.toLongOrNull()
                    val qty = qtyText.toIntOrNull()
                    val thresh = thresholdText.toIntOrNull() ?: defaultLowStock
                    if (name.isBlank() || chosenCat.isBlank() || cost == null || sell == null || (existing == null && qty == null)) {
                        errorMsg = "Please fill all required fields with valid numbers"
                        return@Button
                    }
                    if (existing == null) {
                        viewModel.createProduct(
                            name = name,
                            category = chosenCat,
                            size = size,
                            color = color,
                            costPricePkr = cost,
                            sellingPricePkr = sell,
                            initialQuantity = qty!!,
                            lowStockThreshold = thresh,
                            idempotencyKey = idempotencyKey
                        ) { res ->
                            res.onSuccess { onSaved() }.onFailure { errorMsg = it.message }
                        }
                    } else {
                        viewModel.updateProduct(
                            productId = existing.id,
                            name = name,
                            category = chosenCat,
                            size = size,
                            color = color,
                            costPricePkr = cost,
                            sellingPricePkr = sell,
                            lowStockThreshold = thresh
                        ) { res ->
                            res.onSuccess { onSaved() }.onFailure { errorMsg = it.message }
                        }
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun StockMovementDialog(
    product: ProductEntity,
    movementType: StockMovementType,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var unitsText by rememberSaveable { mutableStateOf("1") }
    var unitPriceText by rememberSaveable {
        mutableStateOf(
            if (movementType == StockMovementType.SALE || movementType == StockMovementType.RETURN) {
                product.sellingPricePkr.toString()
            } else {
                product.costPricePkr.toString()
            }
        )
    }
    var paymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var isNegativeAdjust by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val idempotencyKey = remember { UUID.randomUUID().toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stockMovementLabel(movementType)} • ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Stock: ${product.quantity}")
                if (movementType == StockMovementType.ADJUSTMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isNegativeAdjust,
                            onClick = { isNegativeAdjust = false },
                            label = { Text("+ Add") }
                        )
                        FilterChip(
                            selected = isNegativeAdjust,
                            onClick = { isNegativeAdjust = true },
                            label = { Text("- Deduct") }
                        )
                    }
                }
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text(stringResource(R.string.order_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("movement_units_input")
                )
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Unit Price (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (movementType == StockMovementType.SALE || movementType == StockMovementType.RETURN) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PaymentMethod.entries) { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(paymentMethodLabel(m)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.label_notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val units = unitsText.toIntOrNull()
                    val price = unitPriceText.toLongOrNull()
                    if (units == null || units <= 0 || price == null || price < 0L) {
                        errorMsg = "Enter valid units and price"
                        return@Button
                    }
                    viewModel.recordMovement(
                        productId = product.id,
                        type = movementType,
                        units = units,
                        unitPricePkr = price,
                        paymentMethod = paymentMethod,
                        note = note,
                        idempotencyKey = idempotencyKey,
                        isNegativeAdjustment = isNegativeAdjust
                    ) { res ->
                        res.onSuccess { onSaved() }.onFailure { errorMsg = it.message }
                    }
                },
                modifier = Modifier.testTag("confirm_movement_button")
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
