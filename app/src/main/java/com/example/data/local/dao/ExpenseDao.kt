package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CustomPreferenceOptionEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PreferenceGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, id DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE expenseDate >= :startMillis AND expenseDate <= :endMillis ORDER BY expenseDate DESC, id DESC")
    fun observeExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun countExpenses(): Int
}

@Dao
interface PreferenceOptionDao {
    @Query("SELECT * FROM custom_preference_options WHERE preferenceGroup = :group ORDER BY optionValue ASC")
    fun observeByGroup(group: PreferenceGroup): Flow<List<CustomPreferenceOptionEntity>>

    @Query("SELECT * FROM custom_preference_options ORDER BY preferenceGroup ASC, optionValue ASC")
    fun observeAll(): Flow<List<CustomPreferenceOptionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(option: CustomPreferenceOptionEntity): Long
}
