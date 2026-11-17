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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.dao.CustomerSummaryTuple
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementFieldValue
import com.example.data.local.entity.MeasurementUnit
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LtrMonoText
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.SearchInputField
import com.example.ui.components.ShopTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.WarningBanner
import com.example.ui.components.garmentTypeLabel
import com.example.ui.components.measurementUnitLabel
import com.example.ui.components.paymentMethodLabel
import com.example.ui.components.paymentTypeLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BrassGold
import com.example.ui.theme.CraftSage
import com.example.ui.theme.TailorIndigoContainer
import com.example.ui.viewmodel.CustomersViewModel
import com.example.util.ShopFormatters
import kotlinx.coroutines.launch

@Composable
fun CustomersListScreen(
    viewModel: CustomersViewModel,
    onAddCustomer: () -> Unit,
    onCustomerClick: (Long) -> Unit
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val query by viewModel.currentSearchQuery.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.customer_add_title))
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
                    text = stringResource(R.string.customers_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )
                SearchInputField(
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = stringResource(R.string.customers_search_hint),
                    testTag = "customers_search_input"
                )
                Spacer(Modifier.height(12.dp))
            }

            if (customers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.customer_empty_title),
                    subtitle = stringResource(R.string.customer_empty_subtitle),
                    actionLabel = stringResource(R.string.customer_add_title),
                    onAction = onAddCustomer,
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
                    items(customers, key = { it.customer.id }) { item ->
                        CustomerRowCard(item = item, onClick = { onCustomerClick(item.customer.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRowCard(
    item: CustomerSummaryTuple,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${item.customer.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                LtrMonoText(
                    text = item.customer.customerCode,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrMonoText(
                    text = item.customer.phone,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${item.orderCount} ${stringResource(R.string.nav_orders)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.outstandingPkr > 0L) {
                        LtrMonoText(
                            text = ShopFormatters.formatCurrencyPkr(item.outstandingPkr),
                            color = AlertCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerFormScreen(
    editCustomerId: Long?,
    viewModel: CustomersViewModel,
    onBack: () -> Unit,
    onSaved: (Long, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var duplicateMatches by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.label_saved_successfully)
    val requiredMsg = stringResource(R.string.label_required_field)

    LaunchedEffect(editCustomerId) {
        if (editCustomerId != null && editCustomerId > 0L && !loaded) {
            val detail = viewModel.observeCustomerDetail(editCustomerId)
            detail.collect { state ->
                val c = state.customer
                if (c != null && !loaded) {
                    name = c.name
                    phone = c.phone
                    address = c.address
                    notes = c.notes
                    loaded = true
                }
            }
        }
    }

    fun performSave() {
        viewModel.saveCustomer(
            editingId = editCustomerId?.takeIf { it > 0L },
            name = name,
            phone = phone,
            address = address,
            notes = notes
        ) { result ->
            result.onSuccess { entity ->
                onSaved(entity.id, savedMsg)
            }.onFailure { err ->
                nameError = err.message
            }
        }
    }

    Scaffold(
        topBar = {
            ShopTopBar(
                title = stringResource(
                    if (editCustomerId != null && editCustomerId > 0L) R.string.customer_edit_title
                    else R.string.customer_add_title
                ),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.customer_name_label)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = null
                    },
                    label = { Text(stringResource(R.string.customer_phone_label)) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.customer_address_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.customer_notes_label)) },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_notes_input")
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedPhone = phone.trim()
                        if (trimmedName.isEmpty()) nameError = requiredMsg
                        if (trimmedPhone.isEmpty()) phoneError = requiredMsg
                        if (trimmedName.isEmpty() || trimmedPhone.isEmpty()) return@Button

                        scope.launch {
                            val dup = viewModel.checkDuplicates(
                                name = trimmedName,
                                phone = trimmedPhone,
                                excludeId = editCustomerId ?: -1L
                            )
                            if (dup.hasPossibleDuplicate) {
                                duplicateMatches = dup.matchingNames
                                showDuplicateDialog = true
                            } else {
                                performSave()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_customer_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = BrassGold) },
            title = { Text(stringResource(R.string.customer_duplicate_warning_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.customer_duplicate_warning_body,
                        duplicateMatches.joinToString(", ")
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        performSave()
                    },
                    modifier = Modifier.testTag("confirm_save_duplicate_customer")
                ) {
                    Text(stringResource(R.string.customer_save_anyway))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDuplicateDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: CustomersViewModel,
    onBack: () -> Unit,
    onEditCustomer: (Long) -> Unit,
    onNewOrderForCustomer: (Long) -> Unit,
    onOpenOrderDetail: (Long) -> Unit,
    onNewMeasurement: (Long, String, Long) -> Unit,
    onOpenReceipt: (Long) -> Unit
) {
    val detail by remember(customerId) {
        viewModel.observeCustomerDetail(customerId)
    }.collectAsStateWithLifecycle(initialValue = com.example.ui.viewmodel.CustomerDetailUiState())

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.customer_tab_overview),
        stringResource(R.string.customer_tab_orders),
        stringResource(R.string.customer_tab_measurements),
        stringResource(R.string.customer_tab_payments)
    )
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            ShopTopBar(
                title = detail.customer?.name ?: stringResource(R.string.customers_title),
                onBack = onBack,
                actions = {
                    if (detail.customer != null) {
                        IconButton(
                            onClick = { onEditCustomer(customerId) },
                            modifier = Modifier.testTag("edit_customer_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val customer = detail.customer ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.customer_id_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LtrMonoText(customer.customerCode, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.customer_phone_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LtrMonoText(customer.phone)
                                    }
                                    if (customer.address.isNotBlank()) {
                                        Text(
                                            text = "${stringResource(R.string.customer_address_label)}: ${customer.address}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (customer.notes.isNotBlank()) {
                                        Text(
                                            text = "${stringResource(R.string.label_notes)}: ${customer.notes}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            stringResource(R.string.customer_total_orders),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        LtrMonoText(
                                            text = detail.orders.size.toString(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            stringResource(R.string.customer_total_paid),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        LtrMonoText(
                                            text = ShopFormatters.formatCurrencyPkr(detail.totalPaidPkr),
                                            color = CraftSage,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            stringResource(R.string.customer_outstanding),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        LtrMonoText(
                                            text = ShopFormatters.formatCurrencyPkr(detail.outstandingPkr),
                                            color = if (detail.outstandingPkr > 0) AlertCrimson else CraftSage,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNewOrderForCustomer(customerId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("customer_new_order_button")
                                ) {
                                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.customer_new_order_btn))
                                }
                                OutlinedButton(
                                    onClick = { onNewMeasurement(customerId, "", -1L) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("customer_new_measurement_button")
                                ) {
                                    Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.customer_new_measurement_btn))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (detail.orders.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.ContentCut,
                                    title = stringResource(R.string.orders_empty_title),
                                    subtitle = stringResource(R.string.orders_empty_subtitle),
                                    actionLabel = stringResource(R.string.customer_new_order_btn),
                                    onAction = { onNewOrderForCustomer(customerId) }
                                )
                            }
                        } else {
                            items(detail.orders, key = { it.order.id }) { order ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenOrderDetail(order.order.id) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            LtrMonoText(order.order.orderNumber, color = MaterialTheme.colorScheme.primary)
                                            OrderStatusPill(order.order.status, order.isOverdue(now))
                                        }
                                        Text(
                                            text = "${garmentTypeLabel(order.order.garmentType, order.order.customGarmentName)} × ${order.order.quantity}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            LtrMonoText(ShopFormatters.formatDate(order.order.deliveryDate))
                                            LtrMonoText(
                                                text = "Bal: ${ShopFormatters.formatCurrencyPkr(order.remainingBalancePkr)}",
                                                color = if (order.remainingBalancePkr > 0) AlertCrimson else CraftSage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { onNewMeasurement(customerId, "", -1L) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(R.string.measurement_new_version_title))
                            }
                        }
                        if (detail.measurements.isEmpty()) {
                            item {
                                EmptyStateView(
                                    icon = Icons.Default.Straighten,
                                    title = stringResource(R.string.measurements_title),
                                    subtitle = stringResource(R.string.measurement_empty)
                                )
                            }
                        } else {
                            items(detail.measurements, key = { it.id }) { profile ->
                                val decodedFields = remember(profile.fieldsJson) {
                                    viewModel.measurementRepo.decodeFields(profile.fieldsJson)
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = garmentTypeLabel(profile.garmentType, profile.customGarmentName),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                StatusPill(
                                                    text = stringResource(R.string.measurement_version_badge, profile.versionNumber),
                                                    containerColor = TailorIndigoContainer,
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                                StatusPill(
                                                    text = measurementUnitLabel(profile.unit),
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        LtrMonoText(
                                            text = ShopFormatters.formatDateTime(profile.createdAt),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider()
                                        decodedFields.chunked(2).forEach { pair ->
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                pair.forEach { f ->
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = f.label,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        LtrMonoText(f.value, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                                            }
                                        }
                                        if (profile.notes.isNotBlank()) {
                                            Text(
                                                text = "${stringResource(R.string.label_notes)}: ${profile.notes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onNewMeasurement(
                                                    customerId,
                                                    profile.garmentType.name,
                                                    profile.id
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                stringResource(
                                                    R.string.measurement_create_new_version,
                                                    profile.versionNumber + 1
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detail.payments, key = { it.id }) { pay ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        LtrMonoText(pay.receiptNumber, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "${paymentTypeLabel(pay.transactionType)} • ${paymentMethodLabel(pay.method, pay.otherMethodLabel)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${pay.orderNumber} • ${ShopFormatters.formatDate(pay.paidAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LtrMonoText(
                                        text = ShopFormatters.formatCurrencyPkr(pay.netEffectPkr),
                                        color = if (pay.isDeduction) AlertCrimson else CraftSage,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementEditorScreen(
    customerId: Long,
    preselectedGarmentName: String,
    baseProfileId: Long,
    defaultUnit: MeasurementUnit,
    viewModel: CustomersViewModel,
    onBack: () -> Unit,
    onSaved: (Long, String) -> Unit
) {
    val initialGarment = remember(preselectedGarmentName) {
        GarmentType.entries.firstOrNull { it.name == preselectedGarmentName } ?: GarmentType.SHALWAR_KAMEEZ
    }
    var garmentType by rememberSaveable { mutableStateOf(initialGarment) }
    var customGarmentName by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(defaultUnit) }
    var notes by rememberSaveable { mutableStateOf("") }
    var nextVersionNumber by remember { mutableIntStateOf(1) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val fields = remember {
        mutableStateListOf<MeasurementFieldValue>().apply {
            addAll(viewModel.measurementRepo.defaultFieldsForGarment(initialGarment))
        }
    }
    var newCustomFieldName by rememberSaveable { mutableStateOf("") }
    val savedMsg = stringResource(R.string.label_saved_successfully)

    // Load base profile if creating a new version from an existing one, or fetch latest version count
    LaunchedEffect(customerId, garmentType, customGarmentName, baseProfileId) {
        if (baseProfileId > 0L) {
            val base = viewModel.measurementRepo.getById(baseProfileId)
            if (base != null) {
                garmentType = base.garmentType
                customGarmentName = base.customGarmentName
                unit = base.unit
                notes = base.notes
                val decoded = viewModel.measurementRepo.decodeFields(base.fieldsJson)
                fields.clear()
                fields.addAll(decoded)
            }
        }
        val latest = viewModel.measurementRepo.getLatestForGarment(customerId, garmentType, customGarmentName)
        nextVersionNumber = (latest?.versionNumber ?: 0) + 1
    }

    Scaffold(
        topBar = {
            ShopTopBar(
                title = stringResource(R.string.measurement_new_version_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WarningBanner(
                    icon = Icons.Default.Straighten,
                    message = stringResource(R.string.measurement_immutability_note, nextVersionNumber),
                    isError = false
                )

                Text(
                    text = stringResource(R.string.measurement_garment_type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GarmentType.entries) { gType ->
                        FilterChip(
                            selected = garmentType == gType,
                            onClick = {
                                garmentType = gType
                                if (baseProfileId <= 0L) {
                                    fields.clear()
                                    fields.addAll(viewModel.measurementRepo.defaultFieldsForGarment(gType))
                                }
                            },
                            label = { Text(garmentTypeLabel(gType)) }
                        )
                    }
                }

                if (garmentType == GarmentType.CUSTOM) {
                    OutlinedTextField(
                        value = customGarmentName,
                        onValueChange = { customGarmentName = it },
                        label = { Text(stringResource(R.string.measurement_custom_garment_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = stringResource(R.string.measurement_unit_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MeasurementUnit.entries.forEach { targetUnit ->
                        FilterChip(
                            selected = unit == targetUnit,
                            onClick = {
                                if (unit != targetUnit) {
                                    val converted = viewModel.measurementRepo.convertFieldsUnit(fields.toList(), unit, targetUnit)
                                    fields.clear()
                                    fields.addAll(converted)
                                    unit = targetUnit
                                }
                            },
                            label = { Text(measurementUnitLabel(targetUnit)) },
                            modifier = Modifier.testTag("unit_chip_${targetUnit.name}")
                        )
                    }
                }

                fields.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = field.value,
                            onValueChange = { newVal ->
                                fields[index] = field.copy(value = newVal)
                                errorMsg = null
                            },
                            label = { Text(field.label) },
                            suffix = {
                                Text(if (unit == MeasurementUnit.INCHES) "in" else "cm")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("measurement_field_${field.key}")
                        )
                        if (field.isCustom) {
                            IconButton(onClick = { fields.removeAt(index) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }

                // Add Custom Measurement Field
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomFieldName,
                            onValueChange = { newCustomFieldName = it },
                            label = { Text(stringResource(R.string.measurement_field_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newCustomFieldName.trim()
                                if (clean.isNotEmpty()) {
                                    fields.add(
                                        MeasurementFieldValue(
                                            key = "custom_${System.currentTimeMillis()}",
                                            label = clean,
                                            value = "",
                                            isCustom = true
                                        )
                                    )
                                    newCustomFieldName = ""
                                }
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(stringResource(R.string.action_add))
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.label_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        viewModel.saveNewMeasurementVersion(
                            customerId = customerId,
                            garmentType = garmentType,
                            customGarmentName = customGarmentName,
                            unit = unit,
                            fields = fields.toList(),
                            notes = notes
                        ) { res ->
                            res.onSuccess { profile ->
                                onSaved(profile.id, savedMsg)
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_measurement_button")
                ) {
                    Text("${stringResource(R.string.action_save)} (v$nextVersionNumber)")
                }
            }
        }
    }
}
