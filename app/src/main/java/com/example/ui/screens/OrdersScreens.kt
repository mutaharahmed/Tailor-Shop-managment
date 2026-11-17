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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
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
import com.example.data.local.dao.OrderWithSummary
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.preferences.ShopSettings
import com.example.data.repository.PaymentPostResult
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LtrMonoText
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.SearchInputField
import com.example.ui.components.ShopTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.WarningBanner
import com.example.ui.components.garmentTypeLabel
import com.example.ui.components.measurementUnitLabel
import com.example.ui.components.orderStatusLabel
import com.example.ui.components.paymentMethodLabel
import com.example.ui.components.paymentTypeLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CraftSage
import com.example.ui.theme.TailorIndigoContainer
import com.example.ui.viewmodel.OrderFilter
import com.example.ui.viewmodel.OrdersViewModel
import com.example.util.ShopFormatters
import java.util.UUID

@Composable
fun OrdersListScreen(
    viewModel: OrdersViewModel,
    onCreateOrder: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val orders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val query by viewModel.currentSearchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateOrder,
                modifier = Modifier.testTag("add_order_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.order_create_title))
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
                    text = stringResource(R.string.orders_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
                SearchInputField(
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = stringResource(R.string.orders_search_hint),
                    testTag = "orders_search_input"
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OrderFilter.entries) { f ->
                        val label = when (f) {
                            OrderFilter.ALL -> stringResource(R.string.label_all)
                            OrderFilter.DRAFT -> stringResource(R.string.status_draft)
                            OrderFilter.CONFIRMED -> stringResource(R.string.status_confirmed)
                            OrderFilter.IN_PROGRESS -> stringResource(R.string.status_in_progress)
                            OrderFilter.READY -> stringResource(R.string.status_ready)
                            OrderFilter.DELIVERED -> stringResource(R.string.status_delivered)
                            OrderFilter.CANCELLED -> stringResource(R.string.status_cancelled)
                            OrderFilter.OVERDUE -> stringResource(R.string.filter_overdue)
                        }
                        FilterChip(
                            selected = filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (orders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ContentCut,
                    title = stringResource(R.string.orders_empty_title),
                    subtitle = stringResource(R.string.orders_empty_subtitle),
                    actionLabel = stringResource(R.string.order_create_title),
                    onAction = onCreateOrder,
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
                    items(orders, key = { it.order.id }) { item ->
                        OrderSummaryCard(item = item, now = now, onClick = { onOrderClick(item.order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    item: OrderWithSummary,
    now: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${item.order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrMonoText(
                    text = item.order.orderNumber,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusPill(status = item.order.status, isOverdue = item.isOverdue(now))
            }
            Text(
                text = item.customerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${garmentTypeLabel(item.order.garmentType, item.order.customGarmentName)} × ${item.order.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.order_delivery_date_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LtrMonoText(
                        text = ShopFormatters.formatDate(item.order.deliveryDate),
                        color = if (item.isOverdue(now)) AlertCrimson else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.order_remaining_balance_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LtrMonoText(
                        text = ShopFormatters.formatCurrencyPkr(item.remainingBalancePkr),
                        color = if (item.remainingBalancePkr > 0) AlertCrimson else CraftSage,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailScreen(
    orderId: Long,
    viewModel: OrdersViewModel,
    onBack: () -> Unit,
    onEditOrder: (Long) -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val bundle by remember(orderId) {
        viewModel.observeOrderDetail(orderId)
    }.collectAsStateWithLifecycle(initialValue = null)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var isRefundDialog by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<OrderStatus?>(null) }

    val now = System.currentTimeMillis()
    val savedMsg = stringResource(R.string.label_saved_successfully)

    Scaffold(
        topBar = {
            ShopTopBar(
                title = bundle?.order?.order?.orderNumber ?: stringResource(R.string.orders_title),
                onBack = onBack,
                actions = {
                    if (bundle != null) {
                        IconButton(
                            onClick = { onEditOrder(orderId) },
                            modifier = Modifier.testTag("edit_order_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val data = bundle ?: return@Scaffold
        val orderSummary = data.order
        val order = orderSummary.order

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header & Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LtrMonoText(order.orderNumber, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            OrderStatusPill(order.status, orderSummary.isOverdue(now))
                        }
                        Text(
                            text = data.customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenCustomer(data.customer.id) }
                        )
                        LtrMonoText(data.customer.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${garmentTypeLabel(order.garmentType, order.customGarmentName)} × ${order.quantity}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.order_date_label), style = MaterialTheme.typography.labelSmall)
                                LtrMonoText(ShopFormatters.formatDate(order.orderDate))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.order_delivery_date_label), style = MaterialTheme.typography.labelSmall)
                                LtrMonoText(
                                    text = ShopFormatters.formatDate(order.deliveryDate),
                                    color = if (orderSummary.isOverdue(now)) AlertCrimson else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider()
                        Text(stringResource(R.string.label_status), style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(OrderStatus.entries) { st ->
                                FilterChip(
                                    selected = order.status == st,
                                    onClick = {
                                        if (st != order.status) {
                                            pendingStatusChange = st
                                        }
                                    },
                                    label = { Text(orderStatusLabel(st)) }
                                )
                            }
                        }
                    }
                }
            }

            // Financial Summary & Payment Actions
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.order_stitching_price_label))
                            LtrMonoText(ShopFormatters.formatCurrencyPkr(order.stitchingPricePkr), fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.payment_net_paid))
                            LtrMonoText(ShopFormatters.formatCurrencyPkr(orderSummary.netPaidPkr), color = CraftSage, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.order_remaining_balance_label), fontWeight = FontWeight.Bold)
                            LtrMonoText(
                                text = ShopFormatters.formatCurrencyPkr(orderSummary.remainingBalancePkr),
                                color = if (orderSummary.remainingBalancePkr > 0) AlertCrimson else CraftSage,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    isRefundDialog = false
                                    showPaymentDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("record_payment_button")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.payment_record_title))
                            }
                            if (orderSummary.netPaidPkr > 0L) {
                                OutlinedButton(
                                    onClick = {
                                        isRefundDialog = true
                                        showPaymentDialog = true
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("issue_refund_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.payment_type_refund))
                                }
                            }
                        }
                    }
                }
            }

            // Locked Measurement Version Snapshot
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.measurements_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val meas = data.measurement
                            if (meas != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(
                                        text = stringResource(R.string.measurement_version_badge, meas.versionNumber),
                                        containerColor = TailorIndigoContainer,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                    StatusPill(
                                        text = measurementUnitLabel(meas.unit),
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        val meas = data.measurement
                        if (meas == null) {
                            Text(
                                text = stringResource(R.string.measurement_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val fields = remember(meas.fieldsJson) {
                                viewModel.measurementRepo.decodeFields(meas.fieldsJson)
                            }
                            fields.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    pair.forEach { f ->
                                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(f.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            LtrMonoText(f.value, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Tailoring Design Preferences
            item {
                val customOpts = remember(order.customDesignOptionsJson) {
                    viewModel.orderRepo.decodeCustomOptions(order.customDesignOptionsJson)
                }
                val specs = listOf(
                    stringResource(R.string.pref_collar_style) to order.collarStyle,
                    stringResource(R.string.pref_front_design) to order.frontDesign,
                    stringResource(R.string.pref_cuffs) to order.cuffStyle,
                    stringResource(R.string.pref_pockets) to order.pocketStyle,
                    stringResource(R.string.pref_sleeves) to order.sleeveStyle,
                    stringResource(R.string.pref_bottom_style) to order.bottomStyle,
                    stringResource(R.string.pref_fit) to order.fitStyle,
                    stringResource(R.string.pref_embroidery) to order.embroideryStyle
                ).filter { it.second.isNotBlank() }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.order_preferences_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        specs.forEach { (label, value) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, fontWeight = FontWeight.Medium)
                            }
                        }
                        customOpts.forEach { opt ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(opt.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(opt.value, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (order.additionalInstructions.isNotBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "${stringResource(R.string.pref_additional_instructions)}: ${order.additionalInstructions}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Immutable Payment Ledger
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.payment_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.payment_history_immutable_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(data.payments, key = { it.id }) { pay ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clickable { onOpenReceipt(pay.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                LtrMonoText(pay.receiptNumber, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "${paymentTypeLabel(pay.transactionType)} • ${paymentMethodLabel(pay.method, pay.otherMethodLabel)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = ShopFormatters.formatDateTime(pay.paidAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (pay.note.isNotBlank()) {
                                Text(pay.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        LtrMonoText(
                            text = ShopFormatters.formatCurrencyPkr(pay.netEffectPkr),
                            color = if (pay.isDeduction) AlertCrimson else CraftSage,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    pendingStatusChange?.let { targetStatus ->
        AlertDialog(
            onDismissRequest = { pendingStatusChange = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.order_status_change_confirm, orderStatusLabel(targetStatus))) },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateStatus(orderId, targetStatus)
                    pendingStatusChange = null
                    onShowMessage(savedMsg)
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingStatusChange = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showPaymentDialog && bundle != null) {
        RecordPaymentDialog(
            orderId = orderId,
            remainingBalancePkr = bundle!!.order.remainingBalancePkr,
            netPaidPkr = bundle!!.order.netPaidPkr,
            isRefundMode = isRefundDialog,
            viewModel = viewModel,
            onDismiss = { showPaymentDialog = false },
            onPosted = { paymentId ->
                showPaymentDialog = false
                onShowMessage(savedMsg)
                onOpenReceipt(paymentId)
            }
        )
    }
}

@Composable
private fun RecordPaymentDialog(
    orderId: Long,
    remainingBalancePkr: Long,
    netPaidPkr: Long,
    isRefundMode: Boolean,
    viewModel: OrdersViewModel,
    onDismiss: () -> Unit,
    onPosted: (Long) -> Unit
) {
    var amountText by rememberSaveable {
        mutableStateOf(
            if (!isRefundMode && remainingBalancePkr > 0L) remainingBalancePkr.toString() else ""
        )
    }
    var txType by rememberSaveable {
        mutableStateOf(
            if (isRefundMode) PaymentTransactionType.REFUND
            else if (remainingBalancePkr > 0L) PaymentTransactionType.INSTALLMENT
            else PaymentTransactionType.FINAL_SETTLEMENT
        )
    }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var otherMethod by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var duplicateConfirmPending by remember { mutableStateOf(false) }

    // Stable idempotency key per dialog session to prevent accidental double-tap duplicates
    val idempotencyKey = remember { UUID.randomUUID().toString() }
    val invalidNumMsg = stringResource(R.string.label_invalid_number)

    fun submit(bypassDuplicate: Boolean) {
        val parsedAmount = amountText.trim().toLongOrNull()
        if (parsedAmount == null || parsedAmount <= 0L) {
            errorMsg = invalidNumMsg
            return
        }
        viewModel.postPayment(
            orderId = orderId,
            transactionType = txType,
            method = method,
            otherMethodLabel = otherMethod,
            amountPkr = parsedAmount,
            note = note,
            idempotencyKey = idempotencyKey,
            bypassDuplicateCheck = bypassDuplicate
        ) { result ->
            when (result) {
                is PaymentPostResult.Success -> onPosted(result.payment.id)
                is PaymentPostResult.RequiresDuplicateConfirmation -> duplicateConfirmPending = true
                is PaymentPostResult.Error -> errorMsg = result.message
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isRefundMode) R.string.payment_refund_title else R.string.payment_record_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRefundMode) {
                    val types = listOf(
                        PaymentTransactionType.ADVANCE,
                        PaymentTransactionType.INSTALLMENT,
                        PaymentTransactionType.FINAL_SETTLEMENT
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { t ->
                            FilterChip(
                                selected = txType == t,
                                onClick = { txType = t },
                                label = { Text(paymentTypeLabel(t)) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() }
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.payment_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                val enteredAmount = amountText.toLongOrNull() ?: 0L
                if (!isRefundMode && enteredAmount > remainingBalancePkr && remainingBalancePkr >= 0L) {
                    WarningBanner(
                        icon = Icons.Default.Warning,
                        message = stringResource(R.string.payment_exceeds_balance_warning, remainingBalancePkr),
                        isError = false
                    )
                }
                if (isRefundMode) {
                    Text(
                        text = "${stringResource(R.string.payment_net_paid)}: ${ShopFormatters.formatCurrencyPkr(netPaidPkr)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(stringResource(R.string.payment_method_label), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PaymentMethod.entries) { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(paymentMethodLabel(m)) }
                        )
                    }
                }

                if (method == PaymentMethod.OTHER) {
                    OutlinedTextField(
                        value = otherMethod,
                        onValueChange = { otherMethod = it },
                        label = { Text(stringResource(R.string.payment_other_method_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.payment_note_hint)) },
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
                onClick = { submit(bypassDuplicate = false) },
                modifier = Modifier.testTag("submit_payment_button")
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

    if (duplicateConfirmPending) {
        val parsed = amountText.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { duplicateConfirmPending = false },
            title = { Text(stringResource(R.string.payment_duplicate_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.payment_duplicate_confirm_body,
                        parsed,
                        paymentMethodLabel(method, otherMethod)
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    duplicateConfirmPending = false
                    submit(bypassDuplicate = true)
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { duplicateConfirmPending = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun ReceiptScreen(
    paymentId: Long,
    shopSettings: ShopSettings,
    viewModel: OrdersViewModel,
    onBack: () -> Unit
) {
    val payment by remember(paymentId) {
        viewModel.observePaymentReceipt(paymentId)
    }.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            ShopTopBar(
                title = stringResource(R.string.payment_receipt_title),
                onBack = onBack
            )
        }
    ) { padding ->
        val p = payment ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .testTag("receipt_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = shopSettings.shopName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (shopSettings.shopPhone.isNotBlank()) {
                        LtrMonoText(shopSettings.shopPhone)
                    }
                    if (shopSettings.shopAddress.isNotBlank()) {
                        Text(shopSettings.shopAddress, style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider()

                    ReceiptRow(stringResource(R.string.payment_receipt_number), p.receiptNumber, mono = true)
                    ReceiptRow(stringResource(R.string.order_number_label), p.orderNumber, mono = true)
                    ReceiptRow(stringResource(R.string.customers_title), p.customerName)
                    ReceiptRow(stringResource(R.string.customer_phone_label), p.customerPhone, mono = true)
                    ReceiptRow(stringResource(R.string.label_date), ShopFormatters.formatDateTime(p.paidAt), mono = true)
                    ReceiptRow(stringResource(R.string.payment_type_label), paymentTypeLabel(p.transactionType))
                    ReceiptRow(
                        stringResource(R.string.payment_method_label),
                        paymentMethodLabel(p.method, p.otherMethodLabel)
                    )
                    if (p.note.isNotBlank()) {
                        ReceiptRow(stringResource(R.string.label_notes), p.note)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.payment_amount_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LtrMonoText(
                            text = ShopFormatters.formatCurrencyPkr(p.netEffectPkr),
                            color = if (p.isDeduction) AlertCrimson else CraftSage,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (mono) {
            LtrMonoText(value)
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
