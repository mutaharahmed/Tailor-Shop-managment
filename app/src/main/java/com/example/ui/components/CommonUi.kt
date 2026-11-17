package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementUnit
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PaymentTransactionType
import com.example.data.local.entity.StockMovementType
import com.example.data.preferences.BackupSchedule
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.BrassGold
import com.example.ui.theme.BrassGoldContainer
import com.example.ui.theme.CraftSage
import com.example.ui.theme.CraftSageContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TailorIndigo
import com.example.ui.theme.TailorIndigoContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun orderStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.DRAFT -> stringResource(R.string.status_draft)
    OrderStatus.CONFIRMED -> stringResource(R.string.status_confirmed)
    OrderStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
    OrderStatus.READY -> stringResource(R.string.status_ready)
    OrderStatus.DELIVERED -> stringResource(R.string.status_delivered)
    OrderStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}

@Composable
fun garmentTypeLabel(type: GarmentType, customName: String = ""): String = when (type) {
    GarmentType.SHALWAR_KAMEEZ -> stringResource(R.string.garment_shalwar_kameez)
    GarmentType.SHIRT -> stringResource(R.string.garment_shirt)
    GarmentType.PANT_TROUSER -> stringResource(R.string.garment_pant_trouser)
    GarmentType.WAISTCOAT -> stringResource(R.string.garment_waistcoat)
    GarmentType.COAT_JACKET -> stringResource(R.string.garment_coat_jacket)
    GarmentType.KURTA -> stringResource(R.string.garment_kurta)
    GarmentType.CUSTOM -> customName.ifBlank { stringResource(R.string.garment_custom) }
}

@Composable
fun measurementUnitLabel(unit: MeasurementUnit): String = when (unit) {
    MeasurementUnit.INCHES -> stringResource(R.string.unit_inches)
    MeasurementUnit.CENTIMETERS -> stringResource(R.string.unit_centimeters)
}

@Composable
fun paymentMethodLabel(method: PaymentMethod, otherLabel: String = ""): String = when (method) {
    PaymentMethod.CASH -> stringResource(R.string.method_cash)
    PaymentMethod.BANK_TRANSFER -> stringResource(R.string.method_bank_transfer)
    PaymentMethod.MOBILE_WALLET -> stringResource(R.string.method_mobile_wallet)
    PaymentMethod.CARD -> stringResource(R.string.method_card)
    PaymentMethod.OTHER -> otherLabel.ifBlank { stringResource(R.string.method_other) }
}

@Composable
fun paymentTypeLabel(type: PaymentTransactionType): String = when (type) {
    PaymentTransactionType.ADVANCE -> stringResource(R.string.payment_type_advance)
    PaymentTransactionType.INSTALLMENT -> stringResource(R.string.payment_type_installment)
    PaymentTransactionType.FINAL_SETTLEMENT -> stringResource(R.string.payment_type_final)
    PaymentTransactionType.REFUND -> stringResource(R.string.payment_type_refund)
    PaymentTransactionType.ADJUSTMENT -> stringResource(R.string.payment_type_adjustment)
}

@Composable
fun expenseCategoryLabel(cat: ExpenseCategory): String = when (cat) {
    ExpenseCategory.RENT -> stringResource(R.string.expense_cat_rent)
    ExpenseCategory.ELECTRICITY -> stringResource(R.string.expense_cat_electricity)
    ExpenseCategory.SUPPLIES -> stringResource(R.string.expense_cat_supplies)
    ExpenseCategory.WAGES -> stringResource(R.string.expense_cat_wages)
    ExpenseCategory.MAINTENANCE -> stringResource(R.string.expense_cat_maintenance)
    ExpenseCategory.HOSPITALITY -> stringResource(R.string.expense_cat_hospitality)
    ExpenseCategory.OTHER -> stringResource(R.string.expense_cat_other)
}

@Composable
fun stockMovementLabel(type: StockMovementType): String = when (type) {
    StockMovementType.INITIAL_STOCK -> stringResource(R.string.movement_initial)
    StockMovementType.RESTOCK -> stringResource(R.string.movement_restock)
    StockMovementType.SALE -> stringResource(R.string.movement_sale)
    StockMovementType.RETURN -> stringResource(R.string.movement_return)
    StockMovementType.ADJUSTMENT -> stringResource(R.string.movement_adjustment)
}

@Composable
fun backupScheduleLabel(schedule: BackupSchedule): String = when (schedule) {
    BackupSchedule.DAILY -> stringResource(R.string.backup_schedule_daily)
    BackupSchedule.EVERY_3_DAYS -> stringResource(R.string.backup_schedule_every_3_days)
    BackupSchedule.WEEKLY -> stringResource(R.string.backup_schedule_weekly)
    BackupSchedule.OFF -> stringResource(R.string.backup_schedule_off)
}

@Composable
fun LtrMonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = 1
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderStatusPill(status: OrderStatus, isOverdue: Boolean = false) {
    val (bg, fg) = when {
        isOverdue -> AlertCrimsonContainer to AlertCrimson
        status == OrderStatus.READY -> CraftSageContainer to CraftSage
        status == OrderStatus.DELIVERED -> TailorIndigoContainer to TailorIndigo
        status == OrderStatus.IN_PROGRESS -> BrassGoldContainer to BrassGold
        status == OrderStatus.CONFIRMED -> TailorIndigoContainer to TailorIndigo
        status == OrderStatus.CANCELLED -> AlertCrimsonContainer to AlertCrimson
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = if (isOverdue) {
        "${orderStatusLabel(status)} • ${stringResource(R.string.order_overdue_badge)}"
    } else {
        orderStatusLabel(status)
    }
    StatusPill(text = label, containerColor = bg, contentColor = fg)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "search_input"
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.action_search)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SectionTitleRow(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun WarningBanner(
    icon: ImageVector,
    message: String,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bg = if (isError) AlertCrimsonContainer else WarningAmberContainer
    val fg = if (isError) AlertCrimson else WarningAmber
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
