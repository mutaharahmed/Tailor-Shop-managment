package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.backup.BackupKind
import com.example.data.backup.LocalBackupEntry
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.BackupSchedule
import com.example.data.preferences.MeasurementUnitPref
import com.example.data.preferences.ShopSettings
import com.example.ui.components.LtrMonoText
import com.example.ui.components.ShopTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.WarningBanner
import com.example.ui.components.backupScheduleLabel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BrassGoldContainer
import com.example.ui.theme.CraftSage
import com.example.ui.theme.CraftSageContainer
import com.example.ui.theme.TailorIndigoContainer
import com.example.ui.viewmodel.FinanceAndBackupViewModel
import com.example.ui.viewmodel.MainSettingsViewModel
import com.example.util.ShopFormatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupAndRecoveryScreen(
    viewModel: FinanceAndBackupViewModel,
    settingsViewModel: MainSettingsViewModel,
    settings: ShopSettings,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val backups by viewModel.localBackups.collectAsStateWithLifecycle()
    val freeBytes by viewModel.availableStorageBytes.collectAsStateWithLifecycle()
    val isLowStorage by viewModel.isLowStorage.collectAsStateWithLifecycle()
    val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val busy by viewModel.backupBusy.collectAsStateWithLifecycle()

    var corruptionError by remember { mutableStateOf<String?>(null) }
    val savedMsg = stringResource(R.string.label_saved_successfully)
    val restoreSuccessMsg = stringResource(R.string.backup_restore_success)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri) { res ->
                res.onSuccess { onShowMessage(savedMsg) }
                    .onFailure { onShowMessage(it.message ?: "Export failed") }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectUriForRestore(uri, uri.lastPathSegment ?: "External Backup") { err ->
                corruptionError = err
            }
        }
    }

    Scaffold(
        topBar = {
            ShopTopBar(title = stringResource(R.string.backup_title), onBack = onBack)
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
            if (busy) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp))
                }
            }

            if (isLowStorage) {
                item {
                    Column(Modifier.widthIn(max = 760.dp)) {
                        WarningBanner(
                            icon = Icons.Default.Storage,
                            message = stringResource(R.string.alert_low_storage_banner),
                            isError = true
                        )
                    }
                }
            }

            if (!settings.lastBackupStatusOk && settings.lastBackupError.isNotBlank()) {
                item {
                    Column(Modifier.widthIn(max = 760.dp)) {
                        WarningBanner(
                            icon = Icons.Default.ErrorOutline,
                            message = "${stringResource(R.string.alert_backup_failed_banner)} (${settings.lastBackupError})",
                            isError = true
                        )
                    }
                }
            }

            // Honest risk disclosure card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = BrassGoldContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.backup_warning_card_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.backup_warning_card_body),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Status & Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.backup_free_space, ShopFormatters.formatBytes(freeBytes)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.backup_last_status,
                                if (settings.lastBackupTimestamp > 0L) ShopFormatters.formatDateTime(settings.lastBackupTimestamp)
                                else stringResource(R.string.backup_never)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                viewModel.createManualBackup { res ->
                                    res.onSuccess { onShowMessage(savedMsg) }
                                        .onFailure { onShowMessage(it.message ?: "Backup failed") }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_backup_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.backup_create_now))
                        }

                        OutlinedButton(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportLauncher.launch("azad_taillors_$stamp.atbackup")
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.backup_export_external))
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.backup_import_external))
                        }

                        HorizontalDivider()
                        Text(stringResource(R.string.backup_auto_schedule_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BackupSchedule.entries) { sched ->
                                FilterChip(
                                    selected = settings.backupSchedule == sched,
                                    onClick = { settingsViewModel.setBackupSchedule(context, sched) },
                                    label = { Text(backupScheduleLabel(sched)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${stringResource(R.string.backup_generations_title)} (${backups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)
                )
            }

            items(backups, key = { it.file.absolutePath }) { entry ->
                BackupGenerationCard(
                    entry = entry,
                    onRestoreClick = {
                        viewModel.inspectLocalForRestore(entry) { err ->
                            corruptionError = err
                        }
                    },
                    onDeleteClick = {
                        viewModel.deleteBackup(entry) { res ->
                            res.onFailure { err ->
                                onShowMessage(err.message ?: context.getString(R.string.backup_only_valid_guard))
                            }
                        }
                    }
                )
            }
        }
    }

    restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePreview() },
            title = { Text(stringResource(R.string.backup_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(preview.sourceLabel, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = "SHA-256 OK",
                            containerColor = CraftSageContainer,
                            contentColor = CraftSage
                        )
                        StatusPill(
                            text = "SQLite OK",
                            containerColor = CraftSageContainer,
                            contentColor = CraftSage
                        )
                    }
                    HorizontalDivider()
                    Text("${stringResource(R.string.label_date)}: ${ShopFormatters.formatDateTime(preview.manifest.createdAtMillis)}")
                    Text("${stringResource(R.string.customers_title)}: ${preview.customerCount}")
                    Text("${stringResource(R.string.measurements_title)}: ${preview.measurementCount}")
                    Text("${stringResource(R.string.nav_orders)}: ${preview.orderCount}")
                    Text("${stringResource(R.string.payments_title)}: ${preview.paymentCount}")
                    Text("${stringResource(R.string.nav_inventory)}: ${preview.productCount}")
                    Text("${stringResource(R.string.expenses_title)}: ${preview.expenseCount}")
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.backup_preview_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertCrimson
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmRestore { res ->
                            res.onSuccess { onShowMessage(restoreSuccessMsg) }
                                .onFailure { err -> corruptionError = err.message ?: "Restore failed" }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text(stringResource(R.string.backup_confirm_restore))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRestorePreview() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    corruptionError?.let { reason ->
        AlertDialog(
            onDismissRequest = { corruptionError = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AlertCrimson) },
            title = { Text(stringResource(R.string.backup_corrupted_title)) },
            text = { Text("${stringResource(R.string.backup_corrupted_body)}\n\n$reason") },
            confirmButton = {
                Button(onClick = { corruptionError = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun BackupGenerationCard(
    entry: LocalBackupEntry,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val badgeText = when (entry.kind) {
        BackupKind.AUTO -> stringResource(R.string.backup_auto_badge)
        BackupKind.MANUAL -> stringResource(R.string.backup_manual_badge)
        BackupKind.SAFETY -> stringResource(R.string.backup_safety_badge)
    }
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = badgeText,
                    containerColor = TailorIndigoContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
                LtrMonoText(ShopFormatters.formatBytes(entry.sizeBytes))
            }
            LtrMonoText(ShopFormatters.formatDateTime(entry.createdAtMillis), fontWeight = FontWeight.Bold)
            entry.manifest?.let { m ->
                Text(
                    text = "${m.customerCount} customers • ${m.orderCount} orders • ${m.paymentCount} payments • ${m.productCount} products",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.backup_restore_btn))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: ShopSettings,
    viewModel: MainSettingsViewModel,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var shopName by rememberSaveable(settings.shopName) { mutableStateOf(settings.shopName) }
    var shopPhone by rememberSaveable(settings.shopPhone) { mutableStateOf(settings.shopPhone) }
    var shopAddress by rememberSaveable(settings.shopAddress) { mutableStateOf(settings.shopAddress) }
    var lowStockText by rememberSaveable(settings.defaultLowStockThreshold) {
        mutableStateOf(settings.defaultLowStockThreshold.toString())
    }
    val savedMsg = stringResource(R.string.label_saved_successfully)

    Scaffold(
        topBar = {
            ShopTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
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
                modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = settings.language == AppLanguage.ENGLISH,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                                label = { Text(stringResource(R.string.language_english)) },
                                modifier = Modifier.testTag("settings_lang_en")
                            )
                            FilterChip(
                                selected = settings.language == AppLanguage.SINDHI,
                                onClick = { viewModel.setLanguage(AppLanguage.SINDHI) },
                                label = { Text(stringResource(R.string.language_sindhi)) },
                                modifier = Modifier.testTag("settings_lang_sd")
                            )
                        }

                        HorizontalDivider()

                        Text(stringResource(R.string.settings_default_unit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = settings.defaultUnit == MeasurementUnitPref.INCHES,
                                onClick = { viewModel.setDefaultUnit(MeasurementUnitPref.INCHES) },
                                label = { Text(stringResource(R.string.unit_inches)) }
                            )
                            FilterChip(
                                selected = settings.defaultUnit == MeasurementUnitPref.CENTIMETERS,
                                onClick = { viewModel.setDefaultUnit(MeasurementUnitPref.CENTIMETERS) },
                                label = { Text(stringResource(R.string.unit_centimeters)) }
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_shop_details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text(stringResource(R.string.settings_shop_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = shopPhone,
                            onValueChange = { shopPhone = it },
                            label = { Text(stringResource(R.string.settings_shop_phone)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = shopAddress,
                            onValueChange = { shopAddress = it },
                            label = { Text(stringResource(R.string.settings_shop_address)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = lowStockText,
                            onValueChange = { lowStockText = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.settings_low_stock_default)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val thresh = lowStockText.toIntOrNull() ?: 5
                                viewModel.updateShopDetails(shopName, shopPhone, shopAddress, thresh)
                                onShowMessage(savedMsg)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}
