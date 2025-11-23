package com.example.boilertester

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    // Для сохранённых объектов
    private lateinit var savedSetupsList: MutableList<BoilerSetup>
    private lateinit var spinnerSavedAdapter: ArrayAdapter<String>

    // Для моделей котлов и горелок
    private lateinit var boilerModelAdapter: ArrayAdapter<String>
    private lateinit var burnerModelAdapter: ArrayAdapter<String>
    private var boilerModels: List<BoilerModel> = emptyList()
    private var burnerModels: List<BurnerModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        // Инициализация списков
        savedSetupsList = mutableListOf()
        spinnerSavedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerSavedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        boilerModelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        boilerModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        burnerModelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        burnerModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Привязка адаптеров
        findViewById<Spinner>(R.id.spinnerSavedSetups).adapter = spinnerSavedAdapter
        findViewById<Spinner>(R.id.spinnerBoilerModel).adapter = boilerModelAdapter
        findViewById<Spinner>(R.id.spinnerBurnerModel).adapter = burnerModelAdapter

        // Загрузка данных
        loadSavedSetups()
        loadModels()

        // Обработчики
        findViewById<Spinner>(R.id.spinnerSavedSetups).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) return
                val setup = savedSetupsList[position - 1]
                fillForm(setup)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnSaveSetup).setOnClickListener {
            saveCurrentSetup()
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            proceedToMeasurements()
        }

        findViewById<Button>(R.id.btnManageModels).setOnClickListener {
            startActivity(Intent(this, ManageModelsActivity::class.java))
        }
    }

    private fun loadSavedSetups() {
        lifecycleScope.launch {
            val setups = db.dao().getAllSetups()
            savedSetupsList.clear()
            savedSetupsList.addAll(setups)

            spinnerSavedAdapter.clear()
            spinnerSavedAdapter.add("← Выберите сохранённый объект")
            for (setup in setups) {
                spinnerSavedAdapter.add("${setup.objectName} — ${setup.serialNumber}")
            }
            spinnerSavedAdapter.notifyDataSetChanged()
        }
    }

    private fun loadModels() {
        lifecycleScope.launch {
            // Загружаем котлы
            boilerModels = db.dao().getAllBoilerModels()
            boilerModelAdapter.clear()
            boilerModelAdapter.add("Выберите модель котла")
            boilerModelAdapter.addAll(boilerModels.map { it.name })
            boilerModelAdapter.notifyDataSetChanged()

            // Загружаем горелки
            burnerModels = db.dao().getAllBurnerModels()
            burnerModelAdapter.clear()
            burnerModelAdapter.add("Выберите модель горелки")
            burnerModelAdapter.addAll(burnerModels.map { it.name })
            burnerModelAdapter.notifyDataSetChanged()
        }
    }

    private fun fillForm(setup: BoilerSetup) {
        findViewById<TextInputEditText>(R.id.editSerialNumber).setText(setup.serialNumber)
        findViewById<TextInputEditText>(R.id.editPower).setText(setup.power.toString())
        findViewById<TextInputEditText>(R.id.editObjectName).setText(setup.objectName)
        findViewById<TextInputEditText>(R.id.editAddress).setText(setup.address)

        // Выбор модели котла
        val boilerIndex = boilerModels.indexOfFirst { it.name == setup.boilerModelName }
        if (boilerIndex >= 0) {
            findViewById<Spinner>(R.id.spinnerBoilerModel).setSelection(boilerIndex + 1)
        } else {
            findViewById<Spinner>(R.id.spinnerBoilerModel).setSelection(0)
        }

        // Выбор модели горелки
        val burnerIndex = burnerModels.indexOfFirst { it.name == setup.burnerModelName }
        if (burnerIndex >= 0) {
            findViewById<Spinner>(R.id.spinnerBurnerModel).setSelection(burnerIndex + 1)
        } else {
            findViewById<Spinner>(R.id.spinnerBurnerModel).setSelection(0)
        }
    }

    private fun saveCurrentSetup() {
        val serial = findViewById<TextInputEditText>(R.id.editSerialNumber).text.toString().trim()
        val objectName = findViewById<TextInputEditText>(R.id.editObjectName).text.toString().trim()
        if (serial.isEmpty() || objectName.isEmpty()) {
            Toast.makeText(this, "Заводской номер и название объекта обязательны", Toast.LENGTH_SHORT).show()
            return
        }

        val boilerSpinner = findViewById<Spinner>(R.id.spinnerBoilerModel)
        val burnerSpinner = findViewById<Spinner>(R.id.spinnerBurnerModel)

        val boilerModelName = if (boilerSpinner.selectedItemPosition > 0) {
            boilerModels[boilerSpinner.selectedItemPosition - 1].name
        } else {
            ""
        }

        val burnerModelName = if (burnerSpinner.selectedItemPosition > 0) {
            burnerModels[burnerSpinner.selectedItemPosition - 1].name
        } else {
            ""
        }

        if (boilerModelName.isEmpty() || burnerModelName.isEmpty()) {
            Toast.makeText(this, "Выберите модели котла и горелки", Toast.LENGTH_SHORT).show()
            return
        }

        val power = findViewById<TextInputEditText>(R.id.editPower).text.toString().toDoubleOrNull() ?: 0.0
        val address = findViewById<TextInputEditText>(R.id.editAddress).text.toString().trim()

        lifecycleScope.launch {
            db.dao().insertSetup(
                BoilerSetup(
                    serialNumber = serial,
                    boilerModelName = boilerModelName,
                    burnerModelName = burnerModelName,
                    power = power,
                    objectName = objectName,
                    address = address
                )
            )
            loadSavedSetups()
            Toast.makeText(this@MainActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToMeasurements() {
        val serial = findViewById<TextInputEditText>(R.id.editSerialNumber).text.toString().trim()
        val objectName = findViewById<TextInputEditText>(R.id.editObjectName).text.toString().trim()
        if (serial.isEmpty() || objectName.isEmpty()) {
            Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val boilerSpinner = findViewById<Spinner>(R.id.spinnerBoilerModel)
        val burnerSpinner = findViewById<Spinner>(R.id.spinnerBurnerModel)

        val boilerModelName = if (boilerSpinner.selectedItemPosition > 0) {
            boilerModels[boilerSpinner.selectedItemPosition - 1].name
        } else {
            ""
        }

        val burnerModelName = if (burnerSpinner.selectedItemPosition > 0) {
            burnerModels[burnerSpinner.selectedItemPosition - 1].name
        } else {
            ""
        }

        if (boilerModelName.isEmpty() || burnerModelName.isEmpty()) {
            Toast.makeText(this, "Выберите модели котла и горелки", Toast.LENGTH_SHORT).show()
            return
        }

        val power = findViewById<TextInputEditText>(R.id.editPower).text.toString().toDoubleOrNull() ?: 0.0
        val address = findViewById<TextInputEditText>(R.id.editAddress).text.toString().trim()
        val isWater = findViewById<RadioButton>(R.id.radioWater).isChecked
        val gasHeatStr = findViewById<TextInputEditText>(R.id.editGasHeat).text.toString().trim()
        val gasHeat = findViewById<TextInputEditText>(R.id.editGasHeat).text.toString().toDoubleOrNull() ?: 8100.0
        val gasPressure = findViewById<TextInputEditText>(R.id.editGasPressure).text.toString().toDoubleOrNull() ?: 1013.0
        val gasTemperature = findViewById<TextInputEditText>(R.id.editGasTemperature).text.toString().toDoubleOrNull() ?: 0.0

        Intent(this, MeasurementsActivity::class.java).apply {
            putExtra("boilerType", if (isWater) "water" else "steam")
            putExtra("serial", serial)
            putExtra("model", boilerModelName)
            putExtra("power", power)
            putExtra("burner", burnerModelName)
            putExtra("object", objectName)
            putExtra("address", address)
            intent.putExtra("gasHeat", gasHeat)
            intent.putExtra("gasPressure", gasPressure)
            intent.putExtra("gasTemperature", gasTemperature)
            startActivity(this)
        }
    }
}