package com.example.boilertester

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var savedSetupsList: MutableList<BoilerSetup>
    private lateinit var spinnerSavedAdapter: ArrayAdapter<String>
    private lateinit var boilerModelAdapter: ArrayAdapter<String>
    private lateinit var burnerModelAdapter: ArrayAdapter<String>
    private var boilerModels: List<BoilerModel> = emptyList()
    private var burnerModels: List<BurnerModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        savedSetupsList = mutableListOf()
        spinnerSavedAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        boilerModelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        burnerModelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())

        spinnerSavedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        boilerModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        burnerModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        findViewById<Spinner>(R.id.spinnerSavedSetups).adapter = spinnerSavedAdapter
        findViewById<Spinner>(R.id.spinnerBoilerModel).adapter = boilerModelAdapter
        findViewById<Spinner>(R.id.spinnerBurnerModel).adapter = burnerModelAdapter

        loadSavedSetups()
        loadModels()
        initStartModels()

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

    private fun initStartModels() {
        lifecycleScope.launch {
            if (db.dao().getBoilerModelCount() == 0) {
                db.dao().insertBoilerModel(BoilerModel("Arcus Ignis", 1400.0))
                db.dao().insertBoilerModel(BoilerModel("Titan Prom 200", 2000.0))
                db.dao().insertBoilerModel(BoilerModel("Titan Prom", 1000.0))
                db.dao().insertBoilerModel(BoilerModel("Термотехник-ТТ", 800.0))
                db.dao().insertBoilerModel(BoilerModel("Rossen-RSA", 1200.0))
            }
            if (db.dao().getBurnerModelCount() == 0) {
                db.dao().insertBurnerModel(BurnerModel("FBR GAS P", 1500.0))
                db.dao().insertBurnerModel(BurnerModel("Terminator", 1000.0))
                db.dao().insertBurnerModel(BurnerModel("Garant", 800.0))
            }
            loadModels()
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
            boilerModels = db.dao().getAllBoilerModels()
            boilerModelAdapter.clear()
            boilerModelAdapter.add("Выберите модель котла")
            boilerModelAdapter.addAll(boilerModels.map { it.name })
            boilerModelAdapter.notifyDataSetChanged()

            burnerModels = db.dao().getAllBurnerModels()
            burnerModelAdapter.clear()
            burnerModelAdapter.add("Выберите модель горелки")
            burnerModelAdapter.addAll(burnerModels.map { it.name })
            burnerModelAdapter.notifyDataSetChanged()
        }
    }

    private fun fillForm(setup: BoilerSetup) {
        findViewById<TextInputEditText>(R.id.editSerialNumber).setText(setup.serialNumber)
        findViewById<TextInputEditText>(R.id.editObjectName).setText(setup.objectName)
        findViewById<TextInputEditText>(R.id.editAddress).setText(setup.address)
        findViewById<TextInputEditText>(R.id.editGasHeat).setText(setup.power.toString()) // временно

        val boilerIndex = boilerModels.indexOfFirst { it.name == setup.boilerModelName }
        val burnerIndex = burnerModels.indexOfFirst { it.name == setup.burnerModelName }

        if (boilerIndex >= 0) findViewById<Spinner>(R.id.spinnerBoilerModel).setSelection(boilerIndex + 1)
        if (burnerIndex >= 0) findViewById<Spinner>(R.id.spinnerBurnerModel).setSelection(burnerIndex + 1)
    }

    private fun saveCurrentSetup() {
        val serial = findViewById<TextInputEditText>(R.id.editSerialNumber).text.toString().trim()
        val objectName = findViewById<TextInputEditText>(R.id.editObjectName).text.toString().trim()
        if (serial.isEmpty() || objectName.isEmpty()) {
            Toast.makeText(this, "Заводской номер и название объекта обязательны", Toast.LENGTH_SHORT).show()
            return
        }

        val boilerPos = findViewById<Spinner>(R.id.spinnerBoilerModel).selectedItemPosition
        val burnerPos = findViewById<Spinner>(R.id.spinnerBurnerModel).selectedItemPosition

        val boilerModel = if (boilerPos > 0) boilerModels[boilerPos - 1].name else ""
        val burnerModel = if (burnerPos > 0) burnerModels[burnerPos - 1].name else ""

        if (boilerModel.isEmpty() || burnerModel.isEmpty()) {
            Toast.makeText(this, "Выберите модели котла и горелки", Toast.LENGTH_SHORT).show()
            return
        }

        val boilerPower = if (boilerPos > 0) boilerModels[boilerPos - 1].power else 0.0
        val address = findViewById<TextInputEditText>(R.id.editAddress).text.toString().trim()

        lifecycleScope.launch {
            db.dao().insertSetup(
                BoilerSetup(
                    serialNumber = serial,
                    boilerModelName = boilerModel,
                    burnerModelName = burnerModel,
                    power = boilerPower,
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

        val boilerPos = findViewById<Spinner>(R.id.spinnerBoilerModel).selectedItemPosition
        val burnerPos = findViewById<Spinner>(R.id.spinnerBurnerModel).selectedItemPosition

        val boilerModel = if (boilerPos > 0) boilerModels[boilerPos - 1].name else ""
        val burnerModel = if (burnerPos > 0) burnerModels[burnerPos - 1].name else ""

        if (boilerModel.isEmpty() || burnerModel.isEmpty()) {
            Toast.makeText(this, "Выберите модели котла и горелки", Toast.LENGTH_SHORT).show()
            return
        }

        val boilerPower = if (boilerPos > 0) boilerModels[boilerPos - 1].power else 0.0
        val address = findViewById<TextInputEditText>(R.id.editAddress).text.toString().trim()
        val gasHeat = findViewById<TextInputEditText>(R.id.editGasHeat).text.toString().toDoubleOrNull() ?: 8100.0
        val gasPressure = findViewById<TextInputEditText>(R.id.editGasPressure).text.toString().toDoubleOrNull() ?: 1013.0
        val gasTemperature = findViewById<TextInputEditText>(R.id.editGasTemperature).text.toString().toDoubleOrNull() ?: 0.0
        val isWater = findViewById<RadioButton>(R.id.radioWater).isChecked

        Intent(this, MeasurementsActivity::class.java).apply {
            putExtra("boilerType", if (isWater) "water" else "steam")
            putExtra("serial", serial)
            putExtra("model", boilerModel)
            putExtra("power", boilerPower)
            putExtra("burner", burnerModel)
            putExtra("object", objectName)
            putExtra("address", address)
            putExtra("gasHeat", gasHeat)
            putExtra("gasPressure", gasPressure)
            putExtra("gasTemperature", gasTemperature)
            startActivity(this)
        }
    }
}