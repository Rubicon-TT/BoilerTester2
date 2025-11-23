package com.example.boilertester

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boiler_setups")
data class BoilerSetup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: String,
    val boilerModelName: String,
    val burnerModelName: String,
    val power: Double, // кВт (копия из модели котла, для совместимости)
    val objectName: String,
    val address: String
)