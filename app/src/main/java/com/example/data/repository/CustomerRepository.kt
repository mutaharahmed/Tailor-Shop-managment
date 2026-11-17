package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CustomerSummaryTuple
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

data class DuplicateCheckResult(
    val hasPossibleDuplicate: Boolean,
    val matchingNames: List<String> = emptyList()
)

class CustomerRepository(
    private val dbProvider: () -> AppDatabase
) {
    private fun db() = dbProvider()

    fun observeCustomerSummaries(): Flow<List<CustomerSummaryTuple>> =
        db().customerDao().observeCustomerSummaries()

    fun observeCustomer(customerId: Long): Flow<CustomerEntity?> =
        db().customerDao().observeCustomer(customerId)

    suspend fun getCustomerById(customerId: Long): CustomerEntity? =
        db().customerDao().getCustomerById(customerId)

    suspend fun checkPossibleDuplicates(
        name: String,
        phone: String,
        excludeCustomerId: Long = -1L
    ): DuplicateCheckResult {
        val dao = db().customerDao()
        val trimmedPhone = phone.trim()
        val trimmedName = name.trim()
        val phoneMatches = if (trimmedPhone.isNotEmpty()) {
            dao.findByPhone(trimmedPhone, excludeCustomerId)
        } else {
            emptyList()
        }
        val nameMatches = if (trimmedName.isNotEmpty()) {
            dao.findByExactName(trimmedName, excludeCustomerId)
        } else {
            emptyList()
        }
        val combined = (phoneMatches + nameMatches).distinctBy { it.id }
        return DuplicateCheckResult(
            hasPossibleDuplicate = combined.isNotEmpty(),
            matchingNames = combined.map { "${it.name} (${it.phone})" }
        )
    }

    suspend fun createCustomer(
        name: String,
        phone: String,
        address: String,
        notes: String
    ): Result<CustomerEntity> {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Customer name is required"))
        if (cleanPhone.isEmpty()) return Result.failure(IllegalArgumentException("Phone number is required"))

        return runCatching {
            db().withTransaction {
                val dao = db().customerDao()
                val now = System.currentTimeMillis()
                val nextSeq = dao.getMaxId() + 1L
                val code = "AT-C-${nextSeq.toString().padStart(4, '0')}"
                val entity = CustomerEntity(
                    customerCode = code,
                    name = cleanName,
                    phone = cleanPhone,
                    address = address.trim(),
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                val insertedId = dao.insert(entity)
                entity.copy(id = insertedId)
            }
        }
    }

    suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String,
        address: String,
        notes: String
    ): Result<CustomerEntity> {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Customer name is required"))
        if (cleanPhone.isEmpty()) return Result.failure(IllegalArgumentException("Phone number is required"))

        return runCatching {
            db().withTransaction {
                val dao = db().customerDao()
                val existing = dao.getCustomerById(customerId)
                    ?: throw IllegalStateException("Customer not found")
                val updated = existing.copy(
                    name = cleanName,
                    phone = cleanPhone,
                    address = address.trim(),
                    notes = notes.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated)
                updated
            }
        }
    }
}
