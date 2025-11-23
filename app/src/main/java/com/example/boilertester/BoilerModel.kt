package com.example.boilertester

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boiler_models")
data class BoilerModel(
    @PrimaryKey val name: String,
    val power: Double // кВт
)