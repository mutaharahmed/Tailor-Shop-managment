package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.InventoryDao
import com.example.data.local.dao.MeasurementDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.PaymentDao
import com.example.data.local.dao.PreferenceOptionDao
import com.example.data.local.entity.CustomPreferenceOptionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.PaymentTransactionEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.TailoringOrderEntity

@Database(
    entities = [
        CustomerEntity::class,
        MeasurementProfileEntity::class,
        TailoringOrderEntity::class,
        PaymentTransactionEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        ExpenseEntity::class,
        CustomPreferenceOptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun preferenceOptionDao(): PreferenceOptionDao

    companion object {
        const val DATABASE_NAME = "azad_taillors.db"
        const val CURRENT_SCHEMA_VERSION = 1

        /**
         * Explicit migration strategy placeholder for future schema upgrades.
         * Never use fallbackToDestructiveMigration() in production business records.
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Reserved for future additive schema changes (v1 -> v2).
                }
            }
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Preserve data integrity across versions: never silently wipe user business records.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        /**
         * Safely checkpoints WAL and closes the singleton instance before restoring an external SQLite file.
         */
        fun closeForRestore() {
            synchronized(this) {
                INSTANCE?.let { db ->
                    try {
                        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                            cursor.moveToFirst()
                        }
                    } catch (_: Exception) {
                    }
                    if (db.isOpen) {
                        db.close()
                    }
                }
                INSTANCE = null
            }
        }

        fun checkpointWal(context: Context) {
            val db = getInstance(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                cursor.moveToFirst()
            }
        }
    }
}
