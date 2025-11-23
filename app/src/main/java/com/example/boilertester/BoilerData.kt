package com.example.boilertester

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boiler_setups")
data class BoilerData(
    val boilerType: String, // "water" или "steam"
    val serialNumber: String,
    val boilerModel: String,
    val power: Double,
    val burnerModel: String,
    val objectName: String,
    val address: String,

    // Замеры — водогрейный
    val tIn: Double = 0.0,
    val tOut: Double = 0.0,
    val waterFlow: Double = 0.0,
    val pressureIn: Double = 0.0,
    val pressureOut: Double = 0.0,

    // Замеры — паровой
    val steamPressure: Double = 0.0,
    val steamFlow: Double = 0.0,

    // Общие замеры
    val co: Double = 0.0,              // ppm
    val co2: Double = 0.0,             // %
    val o2: Double = 0.0,              // %
    val no: Double = 0.0,              // %
    val alpha: Double = 0.0,
    val tAir: Double = 0.0,            // °C
    val tFlue: Double = 0.0,           // °C
    val pAir: Double = 0.0,            // мбар
    val gasFlow: Double = 0.0,         // м³/ч
    val pGas: Double = 0.0,            // мбар
    val tGas: Double = 0.0,            // °C
    val efficiency: Double = 0.0,      // %

    // 🔹 Новые параметры
    val draftFurnace: Double = 0.0,        // мм вод.ст.
    val draftAfterBoiler: Double = 0.0,    // мм вод.ст.
    val gasLowerHeat: Double = 8100.0,      // ккал/м³
    val gasPressure: Double = 300.0,           // мбар (давление газа)
    val gasTemperature: Double = 20.0         // °C (температура газа)
)