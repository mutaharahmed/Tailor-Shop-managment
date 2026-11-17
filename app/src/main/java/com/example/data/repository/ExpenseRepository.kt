package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PaymentMethod
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val dbProvider: () -> AppDatabase
) {
    private fun db() = dbProvider()

    fun observeAllExpenses(): Flow<List<ExpenseEntity>> =
        db().expenseDao().observeAllExpenses()

    fun observeExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>> =
        db().expenseDao().observeExpensesInRange(startMillis, endMillis)

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        expenseDate: Long,
        note: String
    ): Result<ExpenseEntity> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Expense description is required"))
        if (amountPkr <= 0L) return Result.failure(IllegalArgumentException("Amount must be greater than zero"))

        return runCatching {
            val entity = ExpenseEntity(
                title = cleanTitle,
                category = category,
                amountPkr = amountPkr,
                paymentMethod = paymentMethod,
                expenseDate = expenseDate,
                note = note.trim(),
                createdAt = System.currentTimeMillis()
            )
            val id = db().expenseDao().insert(entity)
            entity.copy(id = id)
        }
    }

    suspend fun deleteExpense(expenseId: Long): Result<Unit> = runCatching {
        db().expenseDao().deleteById(expenseId)
    }
}
