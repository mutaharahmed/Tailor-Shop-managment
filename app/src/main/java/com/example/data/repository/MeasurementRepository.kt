package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementFieldValue
import com.example.data.local.entity.MeasurementProfileEntity
import com.example.data.local.entity.MeasurementUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

class MeasurementRepository(
    private val dbProvider: () -> AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun db() = dbProvider()

    fun observeCustomerMeasurements(customerId: Long): Flow<List<MeasurementProfileEntity>> =
        db().measurementDao().observeByCustomer(customerId)

    fun observeMeasurement(profileId: Long): Flow<MeasurementProfileEntity?> =
        db().measurementDao().observeById(profileId)

    suspend fun getById(profileId: Long): MeasurementProfileEntity? =
        db().measurementDao().getById(profileId)

    suspend fun getLatestForGarment(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String = ""
    ): MeasurementProfileEntity? =
        db().measurementDao().getLatestForGarment(
            customerId = customerId,
            garmentType = garmentType,
            customGarmentName = if (garmentType == GarmentType.CUSTOM) customGarmentName.trim() else ""
        )

    /**
     * Always creates a brand-new immutable version (v1, v2, v3...) so historical orders
     * continue referencing the exact measurement snapshot used when they were created.
     */
    suspend fun saveNewVersion(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String,
        unit: MeasurementUnit,
        fields: List<MeasurementFieldValue>,
        notes: String
    ): Result<MeasurementProfileEntity> {
        val nonEmptyFields = fields.filter { it.label.isNotBlank() && it.value.isNotBlank() }
        if (nonEmptyFields.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter at least one measurement value"))
        }
        val normalizedCustomName = if (garmentType == GarmentType.CUSTOM) customGarmentName.trim() else ""
        if (garmentType == GarmentType.CUSTOM && normalizedCustomName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter custom garment name"))
        }

        return runCatching {
            db().withTransaction {
                val dao = db().measurementDao()
                val currentMax = dao.getMaxVersionForGarment(
                    customerId = customerId,
                    garmentType = garmentType,
                    customGarmentName = normalizedCustomName
                )
                val nextVersion = currentMax + 1
                val entity = MeasurementProfileEntity(
                    customerId = customerId,
                    garmentType = garmentType,
                    customGarmentName = normalizedCustomName,
                    versionNumber = nextVersion,
                    unit = unit,
                    fieldsJson = json.encodeToString(nonEmptyFields),
                    notes = notes.trim(),
                    createdAt = System.currentTimeMillis()
                )
                val newId = dao.insert(entity)
                entity.copy(id = newId)
            }
        }
    }

    fun decodeFields(fieldsJson: String): List<MeasurementFieldValue> {
        return try {
            json.decodeFromString<List<MeasurementFieldValue>>(fieldsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun defaultFieldsForGarment(garmentType: GarmentType): List<MeasurementFieldValue> {
        return when (garmentType) {
            GarmentType.SHALWAR_KAMEEZ, GarmentType.KURTA -> listOf(
                MeasurementFieldValue("length", "Length", ""),
                MeasurementFieldValue("shoulder", "Shoulder", ""),
                MeasurementFieldValue("sleeve", "Sleeve", ""),
                MeasurementFieldValue("chest", "Chest", ""),
                MeasurementFieldValue("waist", "Waist", ""),
                MeasurementFieldValue("hip", "Hip", ""),
                MeasurementFieldValue("collar", "Collar / Neck", ""),
                MeasurementFieldValue("cuff", "Cuff", ""),
                MeasurementFieldValue("daman", "Daman / Hem", ""),
                MeasurementFieldValue("shalwar_length", "Shalwar Length", ""),
                MeasurementFieldValue("pancha", "Pancha", "")
            )
            GarmentType.SHIRT -> listOf(
                MeasurementFieldValue("length", "Length", ""),
                MeasurementFieldValue("shoulder", "Shoulder", ""),
                MeasurementFieldValue("sleeve", "Sleeve", ""),
                MeasurementFieldValue("chest", "Chest", ""),
                MeasurementFieldValue("waist", "Waist", ""),
                MeasurementFieldValue("collar", "Collar", ""),
                MeasurementFieldValue("cuff", "Cuff", ""),
                MeasurementFieldValue("armhole", "Armhole", "")
            )
            GarmentType.PANT_TROUSER -> listOf(
                MeasurementFieldValue("shalwar_length", "Trouser Length", ""),
                MeasurementFieldValue("waist", "Waist", ""),
                MeasurementFieldValue("hip", "Hip", ""),
                MeasurementFieldValue("thigh", "Thigh", ""),
                MeasurementFieldValue("inseam", "Inseam", ""),
                MeasurementFieldValue("pancha", "Bottom Opening", "")
            )
            GarmentType.WAISTCOAT -> listOf(
                MeasurementFieldValue("length", "Length", ""),
                MeasurementFieldValue("shoulder", "Shoulder", ""),
                MeasurementFieldValue("chest", "Chest", ""),
                MeasurementFieldValue("waist", "Waist", ""),
                MeasurementFieldValue("hip", "Hip", ""),
                MeasurementFieldValue("collar", "Neck", ""),
                MeasurementFieldValue("armhole", "Armhole", "")
            )
            GarmentType.COAT_JACKET -> listOf(
                MeasurementFieldValue("length", "Length", ""),
                MeasurementFieldValue("shoulder", "Shoulder", ""),
                MeasurementFieldValue("sleeve", "Sleeve", ""),
                MeasurementFieldValue("chest", "Chest", ""),
                MeasurementFieldValue("waist", "Waist", ""),
                MeasurementFieldValue("hip", "Hip", ""),
                MeasurementFieldValue("collar", "Neck", ""),
                MeasurementFieldValue("armhole", "Armhole", "")
            )
            GarmentType.CUSTOM -> listOf(
                MeasurementFieldValue("length", "Length", ""),
                MeasurementFieldValue("chest", "Chest", ""),
                MeasurementFieldValue("waist", "Waist", "")
            )
        }
    }

    fun convertFieldsUnit(
        fields: List<MeasurementFieldValue>,
        from: MeasurementUnit,
        to: MeasurementUnit
    ): List<MeasurementFieldValue> {
        if (from == to) return fields
        val factor = if (from == MeasurementUnit.INCHES && to == MeasurementUnit.CENTIMETERS) 2.54 else (1.0 / 2.54)
        return fields.map { field ->
            val numeric = field.value.trim().toDoubleOrNull()
            if (numeric != null) {
                val converted = ((numeric * factor) * 10.0).roundToInt() / 10.0
                val formatted = if (converted % 1.0 == 0.0) converted.toInt().toString() else converted.toString()
                field.copy(value = formatted)
            } else {
                field
            }
        }
    }
}
