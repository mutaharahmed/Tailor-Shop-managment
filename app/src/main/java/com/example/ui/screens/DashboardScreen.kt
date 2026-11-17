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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LtrMonoText
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.SectionTitleRow
import com.example.ui.components.StatusPill
import com.example.ui.components.WarningBanner
import com.example.ui.components.garmentTypeLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CraftSage
import com.example.ui.theme.CraftSageContainer
import com.example.ui.theme.TailorIndigo
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.ShopFormatters

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNewOrder: () -> Unit,
    onNewCustomer: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrderDetail: (Long) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenBackup: () -> Unit
) {
    val now = System.currentTimeMillis()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = stringResource(R.string.label_offline_badge),
                        containerColor = CraftSageContainer,
                        contentColor = CraftSage
                    )
                }

                if (uiState.showBackupAlert) {
                    WarningBanner(
                        icon = Icons.Default.Backup,
                        message = stringResource(R.string.alert_backup_failed_banner),
                        isError = true,
                        onClick = onOpenBackup
                    )
                }
                if (uiState.showLowStorageAlert) {
                    WarningBanner(
                        icon = Icons.Default.Storage,
                        message = stringResource(R.string.alert_low_storage_banner),
                        isError = false,
                        onClick = onOpenBackup
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_new_order),
                            icon = Icons.Default.ContentCut,
                            tag = "quick_new_order",
                            onClick = onNewOrder
                        )
                    }
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_new_customer),
                            icon = Icons.Default.PersonAdd,
                            tag = "quick_new_customer",
                            onClick = onNewCustomer
                        )
                    }
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_record_payment),
                            icon = Icons.Default.Payments,
                            tag = "quick_record_payment",
                            onClick = onOpenOrders
                        )
                    }
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_inventory_sale),
                            icon = Icons.Default.Sell,
                            tag = "quick_inventory_sale",
                            onClick = onOpenInventory
                        )
                    }
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_add_expense),
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            tag = "quick_add_expense",
                            onClick = onOpenExpenses
                        )
                    }
                    item {
                        QuickActionButton(
                            label = stringResource(R.string.quick_backup_now),
                            icon = Icons.Default.Backup,
                            tag = "quick_backup_now",
                            onClick = onOpenBackup
                        )
                    }
                }
            }
        }

        // Order KPIs grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_todays_orders),
                        value = uiState.todaysOrdersCount.toString(),
                        icon = Icons.Default.Today,
                        accentColor = TailorIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_in_progress),
                        value = uiState.inProgressCount.toString(),
                        icon = Icons.Default.ContentCut,
                        accentColor = BrassGold,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_ready_orders),
                        value = uiState.readyOrdersCount.toString(),
                        icon = Icons.Default.CheckCircle,
                        accentColor = CraftSage,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_overdue_orders),
                        value = uiState.overdueOrdersCount.toString(),
                        icon = Icons.Default.Warning,
                        accentColor = if (uiState.overdueOrdersCount > 0) AlertCrimson else TailorIndigo,
                        containerColor = if (uiState.overdueOrdersCount > 0) AlertCrimsonContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_todays_payments),
                        value = ShopFormatters.formatCurrencyPkr(uiState.todaysPaymentsPkr),
                        icon = Icons.Default.Payments,
                        accentColor = CraftSage,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_outstanding_balances),
                        value = ShopFormatters.formatCurrencyPkr(uiState.outstandingBalancePkr),
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = BrassGold,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenOrders
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_todays_expenses),
                        value = ShopFormatters.formatCurrencyPkr(uiState.todaysExpensesPkr),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        accentColor = TailorIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenExpenses
                    )
                    MetricSummaryCard(
                        title = stringResource(R.string.dashboard_low_stock),
                        value = uiState.lowStockCount.toString(),
                        icon = Icons.Default.Inventory2,
                        accentColor = if (uiState.lowStockCount > 0) WarningAmber else CraftSage,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInventory
                    )
                }
            }
        }

        if (uiState.lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clickable(onClick = onOpenInventory),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = WarningAmber)
                            Text(
                                text = "${stringResource(R.string.dashboard_low_stock)} (${uiState.lowStockCount})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        uiState.lowStockProducts.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${p.name} (${p.size.ifBlank { p.category }})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                LtrMonoText(
                                    text = "${p.quantity} left",
                                    color = if (p.quantity == 0) AlertCrimson else WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                SectionTitleRow(
                    title = stringResource(R.string.dashboard_recent_orders),
                    actionLabel = stringResource(R.string.action_view_all),
                    onAction = onOpenOrders
                )
            }
        }

        if (uiState.recentActiveOrders.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.orders_empty_title),
                    subtitle = stringResource(R.string.dashboard_no_active_orders),
                    actionLabel = stringResource(R.string.quick_new_order),
                    onAction = onNewOrder,
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            }
        } else {
            items(uiState.recentActiveOrders, key = { it.order.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .clickable { onOpenOrderDetail(item.order.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LtrMonoText(
                                text = item.order.orderNumber,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            OrderStatusPill(
                                status = item.order.status,
                                isOverdue = item.isOverdue(now)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                            }
                            Column(horizontalAlignment = Alignment.End) {
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
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .testTag(tag),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
