package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.local.entity.CustomOptionPair
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.OrderStatus
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.PreferenceGroup
import com.example.data.repository.OrderDraftInput
import com.example.ui.components.LtrMonoText
import com.example.ui.components.ShopTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.garmentTypeLabel
import com.example.ui.components.measurementUnitLabel
import com.example.ui.components.orderStatusLabel
import com.example.ui.components.paymentMethodLabel
import com.example.ui.theme.BrassGoldContainer
import com.example.ui.theme.CraftSage
import com.example.ui.theme.CraftSageContainer
import com.example.ui.viewmodel.OrdersViewModel
import com.example.util.ShopFormatters
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    editOrderId: Long?,
    preselectedCustomerId: Long?,
    newlyCreatedMeasurementId: Long?,
    viewModel: OrdersViewModel,
    onBack: () -> Unit,
    onAddMeasurementForCustomer: (Long, String, Long) -> Unit,
    onOrderSaved: (Long, String) -> Unit
) {
    val isEditing = editOrderId != null && editOrderId > 0L
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val customSavedOptions by viewModel.customPreferenceOptions.collectAsStateWithLifecycle()

    var selectedCustomerId by rememberSaveable { mutableLongStateOf(preselectedCustomerId?.takeIf { it > 0L } ?: -1L) }
    var customerMenuExpanded by remember { mutableStateOf(false) }

    var garmentType by rememberSaveable { mutableStateOf(GarmentType.SHALWAR_KAMEEZ) }
    var customGarmentName by rememberSaveable { mutableStateOf("") }
    var selectedMeasurementId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reuseConfirmed by rememberSaveable { mutableStateOf(false) }
    var latestMeasurement by remember { mutableStateOf<MeasurementProfileEntity?>(null) }

    var quantityText by rememberSaveable { mutableStateOf("1") }
    var stitchingPriceText by rememberSaveable { mutableStateOf("") }
    var initialAdvanceText by rememberSaveable { mutableStateOf("") }
    var advanceMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var status by rememberSaveable { mutableStateOf(OrderStatus.CONFIRMED) }
    var deliveryDaysFromOrder by rememberSaveable { mutableIntStateOf(7) }
    var orderDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var deliveryDateMillis by rememberSaveable {
        mutableLongStateOf(ShopFormatters.addDaysMillis(System.currentTimeMillis(), 7))
    }

    // Tailoring preferences
    var collarStyle by rememberSaveable { mutableStateOf("") }
    var frontDesign by rememberSaveable { mutableStateOf("") }
    var cuffStyle by rememberSaveable { mutableStateOf("") }
    var pocketStyle by rememberSaveable { mutableStateOf("") }
    var sleeveStyle by rememberSaveable { mutableStateOf("") }
    var bottomStyle by rememberSaveable { mutableStateOf("") }
    var fitStyle by rememberSaveable { mutableStateOf("") }
    var embroideryStyle by rememberSaveable { mutableStateOf("") }
    var additionalInstructions by rememberSaveable { mutableStateOf("") }

    val customDesignPairs = remember { mutableStateListOf<CustomOptionPair>() }
    var newOptionLabel by rememberSaveable { mutableStateOf("") }
    var newOptionValue by rememberSaveable { mutableStateOf("") }

    var loadedEdit by rememberSaveable { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val advanceIdempotencyKey = remember { UUID.randomUUID().toString() }
    val savedMsg = stringResource(R.string.label_saved_successfully)

    // Load existing order if editing
    LaunchedEffect(editOrderId) {
        if (isEditing && !loadedEdit) {
            val detail = viewModel.orderRepo.observeOrder(editOrderId)
            detail.collect { summary ->
                if (summary != null && !loadedEdit) {
                    val o = summary.order
                    selectedCustomerId = o.customerId
                    garmentType = o.garmentType
                    customGarmentName = o.customGarmentName
                    selectedMeasurementId = o.measurementProfileId
                    reuseConfirmed = o.measurementProfileId != null
                    quantityText = o.quantity.toString()
                    stitchingPriceText = o.stitchingPricePkr.toString()
                    status = o.status
                    orderDateMillis = o.orderDate
                    deliveryDateMillis = o.deliveryDate
                    collarStyle = o.collarStyle
                    frontDesign = o.frontDesign
                    cuffStyle = o.cuffStyle
                    pocketStyle = o.pocketStyle
                    sleeveStyle = o.sleeveStyle
                    bottomStyle = o.bottomStyle
                    fitStyle = o.fitStyle
                    embroideryStyle = o.embroideryStyle
                    additionalInstructions = o.additionalInstructions
                    customDesignPairs.clear()
                    customDesignPairs.addAll(viewModel.orderRepo.decodeCustomOptions(o.customDesignOptionsJson))
                    loadedEdit = true
                }
            }
        }
    }

    // Handle returning from MeasurementEditorScreen with newly saved version
    LaunchedEffect(newlyCreatedMeasurementId) {
        if (newlyCreatedMeasurementId != null && newlyCreatedMeasurementId > 0L) {
            selectedMeasurementId = newlyCreatedMeasurementId
            reuseConfirmed = true
        }
    }

    // Look up latest measurement for returning customer + garmentType
    LaunchedEffect(selectedCustomerId, garmentType, customGarmentName) {
        if (selectedCustomerId > 0L) {
            val latest = viewModel.getLatestMeasurementForCustomerGarment(
                customerId = selectedCustomerId,
                garmentType = garmentType,
                customGarmentName = customGarmentName
            )
            latestMeasurement = latest
            if (!isEditing && selectedMeasurementId == null) {
                reuseConfirmed = false
            }
        } else {
            latestMeasurement = null
        }
    }

    Scaffold(
        topBar = {
            ShopTopBar(
                title = stringResource(if (isEditing) R.string.order_edit_title else R.string.order_create_title, ""),
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
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Picker
                val selectedCustomer = customers.firstOrNull { it.customer.id == selectedCustomerId }?.customer
                ExposedDropdownMenuBox(
                    expanded = customerMenuExpanded && !isEditing,
                    onExpandedChange = { if (!isEditing) customerMenuExpanded = !customerMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isEditing,
                        label = { Text(stringResource(R.string.order_select_customer)) },
                        trailingIcon = {
                            if (!isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("order_customer_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = customerMenuExpanded,
                        onDismissRequest = { customerMenuExpanded = false }
                    ) {
                        customers.forEach { tuple ->
                            DropdownMenuItem(
                                text = { Text("${tuple.customer.name} • ${tuple.customer.phone}") },
                                onClick = {
                                    selectedCustomerId = tuple.customer.id
                                    selectedMeasurementId = null
                                    reuseConfirmed = false
                                    customerMenuExpanded = false
                                    errorMsg = null
                                }
                            )
                        }
                    }
                }

                // Garment Type
                if (!isEditing) {
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
                                    selectedMeasurementId = null
                                    reuseConfirmed = false
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
                }

                // Returning Customer Measurement Reuse vs New Version Card
                if (selectedCustomerId > 0L) {
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.measurements_title), fontWeight = FontWeight.Bold)
                                }
                                if (reuseConfirmed && selectedMeasurementId != null) {
                                    StatusPill(
                                        text = "Locked v${latestMeasurement?.versionNumber ?: ""}",
                                        containerColor = CraftSageContainer,
                                        contentColor = CraftSage
                                    )
                                }
                            }

                            val latest = latestMeasurement
                            if (latest != null && !reuseConfirmed) {
                                Text(
                                    text = stringResource(
                                        R.string.measurement_reuse_prompt_body,
                                        garmentTypeLabel(latest.garmentType, latest.customGarmentName),
                                        latest.versionNumber,
                                        measurementUnitLabel(latest.unit)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            selectedMeasurementId = latest.id
                                            reuseConfirmed = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("confirm_reuse_measurement_btn")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.padding(horizontal = 3.dp))
                                        Text(stringResource(R.string.measurement_confirm_reuse, latest.versionNumber))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onAddMeasurementForCustomer(selectedCustomerId, garmentType.name, latest.id)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("create_new_measurement_version_btn")
                                    ) {
                                        Text(stringResource(R.string.measurement_create_new_version, latest.versionNumber + 1))
                                    }
                                }
                            } else if (latest == null) {
                                Text(
                                    text = stringResource(R.string.measurement_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        onAddMeasurementForCustomer(selectedCustomerId, garmentType.name, -1L)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Text(stringResource(R.string.customer_new_measurement_btn))
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "${garmentTypeLabel(latest.garmentType, latest.customGarmentName)} (v${latest.versionNumber})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onAddMeasurementForCustomer(selectedCustomerId, garmentType.name, latest.id)
                                        }
                                    ) {
                                        Text(stringResource(R.string.measurement_create_new_version, latest.versionNumber + 1))
                                    }
                                }
                            }
                        }
                    }
                }

                // Quantity, Pricing, Advance
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it.filter { ch -> ch.isDigit() }
                            errorMsg = null
                        },
                        label = { Text(stringResource(R.string.order_quantity_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_quantity_input")
                    )
                    OutlinedTextField(
                        value = stitchingPriceText,
                        onValueChange = {
                            stitchingPriceText = it.filter { ch -> ch.isDigit() }
                            errorMsg = null
                        },
                        label = { Text(stringResource(R.string.order_stitching_price_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("order_price_input")
                    )
                }

                if (!isEditing) {
                    OutlinedTextField(
                        value = initialAdvanceText,
                        onValueChange = {
                            initialAdvanceText = it.filter { ch -> ch.isDigit() }
                            errorMsg = null
                        },
                        label = { Text(stringResource(R.string.order_advance_payment_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_advance_input")
                    )
                    val adv = initialAdvanceText.toLongOrNull() ?: 0L
                    if (adv > 0L) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PaymentMethod.entries) { m ->
                                FilterChip(
                                    selected = advanceMethod == m,
                                    onClick = { advanceMethod = m },
                                    label = { Text(paymentMethodLabel(m)) }
                                )
                            }
                        }
                    }
                    val totalP = stitchingPriceText.toLongOrNull() ?: 0L
                    if (totalP > 0L) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.order_remaining_balance_label), fontWeight = FontWeight.Medium)
                            LtrMonoText(
                                text = ShopFormatters.formatCurrencyPkr((totalP - adv).coerceAtLeast(0L)),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Delivery Window
                Text(stringResource(R.string.order_delivery_date_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(2, 3, 5, 7, 10, 14)) { days ->
                        FilterChip(
                            selected = deliveryDaysFromOrder == days,
                            onClick = {
                                deliveryDaysFromOrder = days
                                deliveryDateMillis = ShopFormatters.addDaysMillis(orderDateMillis, days)
                            },
                            label = { Text("+${days}d (${ShopFormatters.formatDate(ShopFormatters.addDaysMillis(orderDateMillis, days))})") }
                        )
                    }
                }

                // Status
                Text(stringResource(R.string.label_status), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(OrderStatus.entries) { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(orderStatusLabel(st)) }
                        )
                    }
                }

                // Configurable Tailoring Preferences
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.order_preferences_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_collar_style),
                            value = collarStyle,
                            onValueChange = { collarStyle = it },
                            presets = listOf("Ban", "Collar", "Sherwani", "Round") +
                                customSavedOptions.filter { it.preferenceGroup == PreferenceGroup.COLLAR }.map { it.optionValue }
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_front_design),
                            value = frontDesign,
                            onValueChange = { frontDesign = it },
                            presets = listOf("Simple Patti", "Hidden Buttons", "Piping", "Double Stitch") +
                                customSavedOptions.filter { it.preferenceGroup == PreferenceGroup.FRONT }.map { it.optionValue }
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_cuffs),
                            value = cuffStyle,
                            onValueChange = { cuffStyle = it },
                            presets = listOf("Single Cuff", "Double Cuff", "Cut Cuff", "Simple Open") +
                                customSavedOptions.filter { it.preferenceGroup == PreferenceGroup.CUFFS }.map { it.optionValue }
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_pockets),
                            value = pocketStyle,
                            onValueChange = { pocketStyle = it },
                            presets = listOf("Front + 2 Side", "2 Side Only", "Zip Pocket", "No Front Pocket") +
                                customSavedOptions.filter { it.preferenceGroup == PreferenceGroup.POCKETS }.map { it.optionValue }
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_sleeves),
                            value = sleeveStyle,
                            onValueChange = { sleeveStyle = it },
                            presets = listOf("Full Fitted", "Loose", "Half")
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_bottom_style),
                            value = bottomStyle,
                            onValueChange = { bottomStyle = it },
                            presets = listOf("Round Daman", "Straight Daman", "Slit Cut")
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_fit),
                            value = fitStyle,
                            onValueChange = { fitStyle = it },
                            presets = listOf("Regular", "Slim", "Loose")
                        )
                        PreferenceFieldWithPresets(
                            label = stringResource(R.string.pref_embroidery),
                            value = embroideryStyle,
                            onValueChange = { embroideryStyle = it },
                            presets = listOf("None", "Collar + Patti", "Cuffs + Patti", "Heavy")
                        )

                        // Custom design options
                        customDesignPairs.forEachIndexed { idx, pair ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${pair.name}: ${pair.value}", modifier = Modifier.weight(1f))
                                IconButton(onClick = { customDesignPairs.removeAt(idx) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newOptionLabel,
                                onValueChange = { newOptionLabel = it },
                                label = { Text(stringResource(R.string.pref_custom_option_name)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newOptionValue,
                                onValueChange = { newOptionValue = it },
                                label = { Text(stringResource(R.string.pref_custom_option_value)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newOptionLabel.isNotBlank() && newOptionValue.isNotBlank()) {
                                        customDesignPairs.add(CustomOptionPair(newOptionLabel.trim(), newOptionValue.trim()))
                                        newOptionLabel = ""
                                        newOptionValue = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.pref_add_custom_option))
                            }
                        }

                        OutlinedTextField(
                            value = additionalInstructions,
                            onValueChange = { additionalInstructions = it },
                            label = { Text(stringResource(R.string.pref_additional_instructions)) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                errorMsg?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        val qty = quantityText.toIntOrNull()
                        val price = stitchingPriceText.toLongOrNull()
                        val adv = if (initialAdvanceText.isBlank()) 0L else initialAdvanceText.toLongOrNull()

                        if (selectedCustomerId <= 0L) {
                            errorMsg = "Please select a customer"
                            return@Button
                        }
                        if (qty == null || qty <= 0) {
                            errorMsg = "Enter valid quantity"
                            return@Button
                        }
                        if (price == null || price < 0L) {
                            errorMsg = "Enter valid stitching price"
                            return@Button
                        }
                        if (!isEditing && (adv == null || adv < 0L || adv > price)) {
                            errorMsg = "Advance must be between 0 and total price"
                            return@Button
                        }

                        if (isEditing) {
                            viewModel.updateOrder(
                                orderId = editOrderId!!,
                                measurementProfileId = selectedMeasurementId,
                                quantity = qty,
                                collarStyle = collarStyle,
                                frontDesign = frontDesign,
                                cuffStyle = cuffStyle,
                                pocketStyle = pocketStyle,
                                sleeveStyle = sleeveStyle,
                                bottomStyle = bottomStyle,
                                fitStyle = fitStyle,
                                embroideryStyle = embroideryStyle,
                                customOptions = customDesignPairs.toList(),
                                additionalInstructions = additionalInstructions,
                                stitchingPricePkr = price,
                                deliveryDate = deliveryDateMillis,
                                status = status
                            ) { res ->
                                res.onSuccess { updated ->
                                    onOrderSaved(updated.id, savedMsg)
                                }.onFailure { err ->
                                    errorMsg = err.message
                                }
                            }
                        } else {
                            viewModel.createOrder(
                                OrderDraftInput(
                                    customerId = selectedCustomerId,
                                    measurementProfileId = selectedMeasurementId,
                                    garmentType = garmentType,
                                    customGarmentName = customGarmentName,
                                    quantity = qty,
                                    collarStyle = collarStyle,
                                    frontDesign = frontDesign,
                                    cuffStyle = cuffStyle,
                                    pocketStyle = pocketStyle,
                                    sleeveStyle = sleeveStyle,
                                    bottomStyle = bottomStyle,
                                    fitStyle = fitStyle,
                                    embroideryStyle = embroideryStyle,
                                    customOptions = customDesignPairs.toList(),
                                    additionalInstructions = additionalInstructions,
                                    stitchingPricePkr = price,
                                    initialAdvancePkr = adv ?: 0L,
                                    advancePaymentMethod = advanceMethod,
                                    orderDate = orderDateMillis,
                                    deliveryDate = deliveryDateMillis,
                                    status = status,
                                    advanceIdempotencyKey = advanceIdempotencyKey
                                )
                            ) { res ->
                                res.onSuccess { created ->
                                    onOrderSaved(created.id, savedMsg)
                                }.onFailure { err ->
                                    errorMsg = err.message
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_order_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PreferenceFieldWithPresets(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    presets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presets.distinct()) { preset ->
                FilterChip(
                    selected = value.equals(preset, ignoreCase = true),
                    onClick = { onValueChange(preset) },
                    label = { Text(preset) }
                )
            }
        }
    }
}
