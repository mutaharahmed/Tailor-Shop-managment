package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shopDataStore: DataStore<Preferences> by preferencesDataStore(name = "azad_taillors_prefs")

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    SINDHI("sd");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class MeasurementUnitPref(val code: String) {
    INCHES("INCHES"),
    CENTIMETERS("CENTIMETERS");

    companion object {
        fun fromCode(code: String?): MeasurementUnitPref =
            entries.firstOrNull { it.code == code } ?: INCHES
    }
}

enum class BackupSchedule(val hours: Long) {
    DAILY(24L),
    EVERY_3_DAYS(72L),
    WEEKLY(168L),
    OFF(0L);

    companion object {
        fun fromName(name: String?): BackupSchedule =
            entries.firstOrNull { it.name == name } ?: DAILY
    }
}

data class ShopSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultUnit: MeasurementUnitPref = MeasurementUnitPref.INCHES,
    val backupSchedule: BackupSchedule = BackupSchedule.DAILY,
    val shopName: String = "Azad Taillors",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val defaultLowStockThreshold: Int = 5,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupStatusOk: Boolean = true,
    val lastBackupError: String = ""
)

class ShopPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val DEFAULT_UNIT = stringPreferencesKey("default_measurement_unit")
        val BACKUP_SCHEDULE = stringPreferencesKey("backup_schedule")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val LOW_STOCK_THRESHOLD = intPreferencesKey("default_low_stock_threshold")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_OK = booleanPreferencesKey("last_backup_ok")
        val LAST_BACKUP_ERROR = stringPreferencesKey("last_backup_error")
    }

    val settingsFlow: Flow<ShopSettings> = context.shopDataStore.data.map { prefs ->
        ShopSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            defaultUnit = MeasurementUnitPref.fromCode(prefs[Keys.DEFAULT_UNIT]),
            backupSchedule = BackupSchedule.fromName(prefs[Keys.BACKUP_SCHEDULE]),
            shopName = prefs[Keys.SHOP_NAME] ?: "Azad Taillors",
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "",
            shopAddress = prefs[Keys.SHOP_ADDRESS] ?: "",
            defaultLowStockThreshold = prefs[Keys.LOW_STOCK_THRESHOLD] ?: 5,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_AT] ?: 0L,
            lastBackupStatusOk = prefs[Keys.LAST_BACKUP_OK] ?: true,
            lastBackupError = prefs[Keys.LAST_BACKUP_ERROR] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.shopDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDefaultUnit(unit: MeasurementUnitPref) {
        context.shopDataStore.edit { it[Keys.DEFAULT_UNIT] = unit.code }
    }

    suspend fun setBackupSchedule(schedule: BackupSchedule) {
        context.shopDataStore.edit { it[Keys.BACKUP_SCHEDULE] = schedule.name }
    }

    suspend fun updateShopDetails(name: String, phone: String, address: String, lowStockThreshold: Int) {
        context.shopDataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = name.ifBlank { "Azad Taillors" }
            prefs[Keys.SHOP_PHONE] = phone.trim()
            prefs[Keys.SHOP_ADDRESS] = address.trim()
            prefs[Keys.LOW_STOCK_THRESHOLD] = lowStockThreshold.coerceAtLeast(1)
        }
    }

    suspend fun recordBackupOutcome(timestamp: Long, success: Boolean, errorMessage: String = "") {
        context.shopDataStore.edit { prefs ->
            if (success) {
                prefs[Keys.LAST_BACKUP_AT] = timestamp
            }
            prefs[Keys.LAST_BACKUP_OK] = success
            prefs[Keys.LAST_BACKUP_ERROR] = errorMessage
        }
    }
}
