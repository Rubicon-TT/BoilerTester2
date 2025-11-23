package com.example.boilertester

import androidx.room.*

@Dao
interface AppDatabaseDao {

    // Модели котлов
    @Query("SELECT * FROM boiler_models ORDER BY name")
    suspend fun getAllBoilerModels(): List<BoilerModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoilerModel(model: BoilerModel)

    @Delete
    suspend fun deleteBoilerModel(model: BoilerModel)

    @Query("SELECT COUNT(*) FROM boiler_models")
    suspend fun getBoilerModelCount(): Int


    // Модели горелок
    @Query("SELECT * FROM burner_models ORDER BY name")
    suspend fun getAllBurnerModels(): List<BurnerModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBurnerModel(model: BurnerModel)

    @Delete
    suspend fun deleteBurnerModel(model: BurnerModel)

    @Query("SELECT COUNT(*) FROM burner_models")
    suspend fun getBurnerModelCount(): Int


    // Сохранённые объекты
    @Query("SELECT * FROM boiler_setups ORDER BY objectName")
    suspend fun getAllSetups(): List<BoilerSetup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: BoilerSetup)

    @Delete
    suspend fun deleteSetup(setup: BoilerSetup)
}