package com.example.boilertester

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoilerSetupDao {
    @Query("SELECT * FROM boiler_setups ORDER BY objectName")
    suspend fun getAllSetups(): List<BoilerSetup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setup: BoilerSetup)

    @Delete
    suspend fun delete(setup: BoilerSetup)
}