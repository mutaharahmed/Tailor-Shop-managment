package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.GarmentType
import com.example.data.local.entity.MeasurementProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_profiles WHERE customerId = :customerId ORDER BY garmentType ASC, versionNumber DESC")
    fun observeByCustomer(customerId: Long): Flow<List<MeasurementProfileEntity>>

    @Query("SELECT * FROM measurement_profiles WHERE id = :profileId")
    suspend fun getById(profileId: Long): MeasurementProfileEntity?

    @Query("SELECT * FROM measurement_profiles WHERE id = :profileId")
    fun observeById(profileId: Long): Flow<MeasurementProfileEntity?>

    @Query(
        """
        SELECT * FROM measurement_profiles
        WHERE customerId = :customerId
          AND garmentType = :garmentType
          AND customGarmentName = :customGarmentName
        ORDER BY versionNumber DESC
        LIMIT 1
        """
    )
    suspend fun getLatestForGarment(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String
    ): MeasurementProfileEntity?

    @Query(
        """
        SELECT COALESCE(MAX(versionNumber), 0) FROM measurement_profiles
        WHERE customerId = :customerId
          AND garmentType = :garmentType
          AND customGarmentName = :customGarmentName
        """
    )
    suspend fun getMaxVersionForGarment(
        customerId: Long,
        garmentType: GarmentType,
        customGarmentName: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: MeasurementProfileEntity): Long

    @Query("SELECT COUNT(*) FROM tailoring_orders WHERE measurementProfileId = :profileId")
    suspend fun countOrdersUsingProfile(profileId: Long): Int
}
