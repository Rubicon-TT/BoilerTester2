package com.example.boilertester

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "burner_models")
data class BurnerModel(
    @PrimaryKey val name: String,
    val power: Double // кВт
)