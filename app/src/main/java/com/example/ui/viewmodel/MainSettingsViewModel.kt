package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.AutoBackupWorker
import com.example.data.backup.BackupManager
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.BackupSchedule
import com.example.data.preferences.MeasurementUnitPref
import com.example.data.preferences.ShopPreferencesRepository
import com.example.data.preferences.ShopSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val prefsRepo: ShopPreferencesRepository,
    val backupManager: BackupManager
) : ViewModel() {

    val settings: StateFlow<ShopSettings> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShopSettings()
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguage(language)
        }
    }

    fun setDefaultUnit(unit: MeasurementUnitPref) {
        viewModelScope.launch {
            prefsRepo.setDefaultUnit(unit)
        }
    }

    fun setBackupSchedule(context: Context, schedule: BackupSchedule) {
        viewModelScope.launch {
            prefsRepo.setBackupSchedule(schedule)
            AutoBackupWorker.schedule(context, schedule)
        }
    }

    fun updateShopDetails(name: String, phone: String, address: String, lowStockThreshold: Int) {
        viewModelScope.launch {
            prefsRepo.updateShopDetails(name, phone, address, lowStockThreshold)
        }
    }
}
