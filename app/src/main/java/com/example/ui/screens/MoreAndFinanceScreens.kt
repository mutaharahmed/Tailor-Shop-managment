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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PaymentMethod
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.ShopSettings
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LtrMonoText
import com.example.ui.components.ShopTopBar
import com.example.ui.components.expenseCategoryLabel
import com.example.ui.components.paymentMethodLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CraftSage
import com.example.ui.viewmodel.FinanceAndBackupViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.ShopFormatters

@Composable
fun MoreHubScreen(
    settings: ShopSettings,
    onOpenExpenses: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLanguageQuick: (AppLanguage) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Text(
                    text = stringResource(R.string.more_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Language Switch Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = settings.language == AppLanguage.ENGLISH,
                            onClick = { onToggleLanguageQuick(AppLanguage.ENGLISH) },
                            label = { Text(stringResource(R.string.language_english)) },
                            modifier = Modifier.testTag("lang_chip_en")
                        )
                        FilterChip(
                            selected = settings.language == AppLanguage.SINDHI,
                            onClick = { onToggleLanguageQuick(AppLanguage.SINDHI) },
                            label = { Text(stringResource(R.string.language_sindhi)) },
                            modifier = Modifier.testTag("lang_chip_sd")
                        )
                    }
                }
            }
        }

        item {
            MoreMenuRow(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.expenses_title),
                subtitle = "${stringResource(R.string.expense_period_daily)} • ${stringResource(R.string.expense_period_weekly)} • ${stringResource(R.string.expense_period_monthly)}",
                tag = "nav_expenses_card",
                onClick = onOpenExpenses
            )
        }
        item {
            MoreMenuRow(
                icon = Icons.Default.Assessment,
                title = stringResource(R.string.reports_title),
                subtitle = stringResource(R.string.report_sales_summary),
                tag = "nav_reports_card",
                onClick = onOpenReports
            )
        }
        item {
            MoreMenuRow(
                icon = Icons.Default.Backup,
                title = stringResource(R.string.backup_title),
                subtitle = stringResource(
                    R.string.backup_last_status,
                    if (settings.lastBackupTimestamp > 0L) ShopFormatters.formatDateTime(settings.lastBackupTimestamp)
                    else stringResource(R.string.backup_never)
                ),
                tag = "nav_backup_card",
                onClick = onOpenBackup
            )
        }
        item {
            MoreMenuRow(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_title),
                subtitle = settings.shopName,
                tag = "nav_settings_card",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun ExpensesScreen(
    viewModel: FinanceAndBackupViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val expenses by viewModel.expensesForPeriod.collectAsStateWithLifecycle()
    val period by viewModel.currentExpensePeriod.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<ExpenseEntity?>(null) }
    val savedMsg = stringResource(R.string.label_saved_successfully)
    val totalPeriodPkr = expenses.sumOf { it.amountPkr }

    Scaffold(
        topBar = {
            ShopTopBar(title = stringResource(R.string.expenses_title), onBack = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.expense_add_title))
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
                    .widthIn(max = 760.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(ReportPeriod.DAILY, ReportPeriod.WEEKLY, ReportPeriod.MONTHLY)) { p ->
                        val label = when (p) {
                            ReportPeriod.DAILY -> stringResource(R.string.expense_period_daily)
                            ReportPeriod.WEEKLY -> stringResource(R.string.expense_period_weekly)
                            else -> stringResource(R.string.expense_period_monthly)
                        }
                        FilterChip(
                            selected = period == p,
                            onClick = { viewModel.setExpensePeriod(p) },
                            label = { Text(label) }
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.report_total_expenses), fontWeight = FontWeight.Bold)
                        LtrMonoText(
                            text = ShopFormatters.formatCurrencyPkr(totalPeriodPkr),
                            color = AlertCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (expenses.isEmpty()) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = stringResource(R.string.expenses_title),
                    subtitle = stringResource(R.string.expenses_empty),
                    actionLabel = stringResource(R.string.expense_add_title),
                    onAction = { showAddDialog = true },
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(exp.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${expenseCategoryLabel(exp.category)} • ${paymentMethodLabel(exp.paymentMethod)} • ${ShopFormatters.formatDate(exp.expenseDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LtrMonoText(
                                    text = ShopFormatters.formatCurrencyPkr(exp.amountPkr),
                                    color = AlertCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { deleteCandidate = exp }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var amountText by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf(ExpenseCategory.SUPPLIES) }
        var method by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
        var note by rememberSaveable { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.expense_add_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; err = null },
                        label = { Text(stringResource(R.string.expense_title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() }; err = null },
                        label = { Text(stringResource(R.string.expense_amount_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )
                    Text(stringResource(R.string.expense_category_label), style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpenseCategory.entries) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(expenseCategoryLabel(cat)) }
                            )
                        }
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
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.label_notes)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull()
                        if (title.isBlank() || amt == null || amt <= 0L) {
                            err = "Enter description and valid amount"
                            return@Button
                        }
                        viewModel.addExpense(
                            title = title,
                            category = category,
                            amountPkr = amt,
                            paymentMethod = method,
                            expenseDate = System.currentTimeMillis(),
                            note = note
                        ) { res ->
                            res.onSuccess {
                                showAddDialog = false
                                onShowMessage(savedMsg)
                            }.onFailure { err = it.message }
                        }
                    },
                    modifier = Modifier.testTag("save_expense_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    deleteCandidate?.let { exp ->
        DestructiveConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = "${exp.title} (${ShopFormatters.formatCurrencyPkr(exp.amountPkr)})\n${stringResource(R.string.confirm_destructive_message)}",
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.deleteExpense(exp.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
fun ReportsScreen(
    viewModel: FinanceAndBackupViewModel,
    onBack: () -> Unit
) {
    val report by viewModel.reportSummary.collectAsStateWithLifecycle()
    val period by viewModel.currentReportPeriod.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ShopTopBar(title = stringResource(R.string.reports_title), onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ReportPeriod.entries) { p ->
                        val label = when (p) {
                            ReportPeriod.DAILY -> stringResource(R.string.expense_period_daily)
                            ReportPeriod.WEEKLY -> stringResource(R.string.expense_period_weekly)
                            ReportPeriod.MONTHLY -> stringResource(R.string.expense_period_monthly)
                            ReportPeriod.ALL_TIME -> stringResource(R.string.label_all)
                        }
                        FilterChip(
                            selected = period == p,
                            onClick = { viewModel.setReportPeriod(p) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                ReportSectionCard(title = stringResource(R.string.report_sales_summary)) {
                    ReportLine(stringResource(R.string.report_tailoring_revenue), "${ShopFormatters.formatCurrencyPkr(report.tailoringOrdersValuePkr)} (${report.tailoringOrdersCount})")
                    ReportLine(stringResource(R.string.report_payments_collected), ShopFormatters.formatCurrencyPkr(report.tailoringPaymentsCollectedPkr), CraftSage)
                    ReportLine(stringResource(R.string.report_readymade_sales), "${ShopFormatters.formatCurrencyPkr(report.readyMadeSalesPkr)} (${report.readyMadeUnitsSold} units)", CraftSage)
                    ReportLine(stringResource(R.string.report_total_expenses), ShopFormatters.formatCurrencyPkr(report.totalExpensesPkr), AlertCrimson)
                    HorizontalDivider()
                    ReportLine(
                        label = stringResource(R.string.report_estimated_profit),
                        value = ShopFormatters.formatCurrencyPkr(report.netCashSummaryPkr),
                        valueColor = if (report.netCashSummaryPkr >= 0L) CraftSage else AlertCrimson,
                        bold = true
                    )
                }
            }

            item {
                ReportSectionCard(title = stringResource(R.string.report_outstanding_orders)) {
                    ReportLine(stringResource(R.string.report_outstanding_orders), ShopFormatters.formatCurrencyPkr(report.totalOutstandingBalancesPkr), AlertCrimson)
                    ReportLine(stringResource(R.string.report_pending_orders), report.pendingOrdersCount.toString())
                    ReportLine(stringResource(R.string.report_overdue_orders), report.overdueOrdersCount.toString(), if (report.overdueOrdersCount > 0) AlertCrimson else CraftSage)
                }
            }

            item {
                ReportSectionCard(title = stringResource(R.string.report_inventory_summary)) {
                    ReportLine(stringResource(R.string.report_inventory_units), report.inventoryTotalUnits.toString())
                    ReportLine(stringResource(R.string.report_inventory_cost_value), ShopFormatters.formatCurrencyPkr(report.inventoryCostValuePkr))
                    ReportLine(stringResource(R.string.report_inventory_retail_value), ShopFormatters.formatCurrencyPkr(report.inventoryRetailValuePkr), CraftSage)
                    ReportLine(stringResource(R.string.dashboard_low_stock), report.lowStockProductCount.toString())
                }
            }

            if (report.expensesByCategory.isNotEmpty()) {
                item {
                    ReportSectionCard(title = stringResource(R.string.expenses_title)) {
                        report.expensesByCategory.forEach { (cat, total) ->
                            ReportLine(expenseCategoryLabel(cat), ShopFormatters.formatCurrencyPkr(total))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ReportLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        LtrMonoText(value, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
