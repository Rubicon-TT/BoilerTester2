package com.example.boilertester

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MeasurementsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var btnReport: Button

    private var editTIn: TextInputEditText? = null
    private var editTOut: TextInputEditText? = null
    private var editWaterFlow: TextInputEditText? = null
    private var editSteamP: TextInputEditText? = null
    private var editSteamFlow: TextInputEditText? = null

    private var editCO: TextInputEditText? = null
    private var editO2: TextInputEditText? = null
    private var editCO2: TextInputEditText? = null
    private var editNO: TextInputEditText? = null
    private var editAlpha: TextInputEditText? = null
    private var editTAir: TextInputEditText? = null
    private var editTFlue: TextInputEditText? = null
    private var editPAir: TextInputEditText? = null
    private var editEff: TextInputEditText? = null
    private var editGasFlow: TextInputEditText? = null
    private var editPGas: TextInputEditText? = null
    private var editTGas: TextInputEditText? = null
    private var editPressureIn: TextInputEditText? = null
    private var editPressureOut: TextInputEditText? = null
    private var editDraftFurnace: TextInputEditText? = null
    private var editDraftAfterBoiler: TextInputEditText? = null
    private var editGasHeat: TextInputEditText? = null

    private var boilerType = "water"
    private var serial = ""
    private var model = ""
    private var power = 0.0
    private var burner = ""
    private var objectName = ""
    private var address = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurements)

        boilerType = intent.getStringExtra("boilerType") ?: "water"
        serial = intent.getStringExtra("serial") ?: ""
        model = intent.getStringExtra("model") ?: ""
        power = intent.getDoubleExtra("power", 0.0)
        burner = intent.getStringExtra("burner") ?: ""
        objectName = intent.getStringExtra("object") ?: ""
        address = intent.getStringExtra("address") ?: ""

        container = findViewById(R.id.container)
        createUI()
    }

    private fun createUI() {
        container.removeAllViews()

        if (boilerType == "water") {
            editTIn = addEditText("Температура воды на входе (°C)")
            editTOut = addEditText("Температура воды на выходе (°C)")
            editWaterFlow = addEditText("Расход воды (м³/ч)")
            editPressureIn = addEditText("Давление на входе (бар)")
            editPressureOut = addEditText("Давление на выходе (бар)")
        } else {
            editSteamP = addEditText("Давление пара (бар)")
            editSteamFlow = addEditText("Расход пара (т/ч)")
        }

        // Разрежение
        editDraftFurnace = addEditText("Разрежение в топке (мм вод.ст.)")
        editDraftAfterBoiler = addEditText("Разрежение за котлом (мм вод.ст.)")
        editCO = addEditText("CO (ppm)")
        editO2 = addEditText("O₂ (%)")
        editCO2 = addEditText("CO₂ (%)")
        editNO = addEditText("NO (ppm)")
        editAlpha = addEditText("Коэффициент избытка воздуха (α)")
        editTAir = addEditText("Температура воздуха (°C)")
        editTFlue = addEditText("Температура уходящих газов (°C)")
        editPAir = addEditText("Давление воздуха (мбар)")
        editEff = addEditText("КПД (%)")
        editGasFlow = addEditText("Расход газа (м³/ч)")
        editPGas = addEditText("Давление газа (мбар)")
        editTGas = addEditText("Температура газа (°C)")

// Теплота сгорания
        editGasHeat = addEditText("Низшая теплота сгорания (ккал/м³)")
        editGasHeat?.setText("8100")

        btnReport = Button(this).apply {
            text = "Сформировать отчёт"
            setOnClickListener { generateReport() }
            setPadding(0, 32, 0, 32)
        }
        container.addView(btnReport)
    }

    private fun addEditText(hint: String): TextInputEditText {
        val layout = TextInputLayout(this).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 0, 0, 32)
        }
        val editText = TextInputEditText(this)
        layout.addView(editText)
        container.addView(layout)
        return editText
    }


    private fun generateReport() {
        val gasHeat = intent.getDoubleExtra("gasHeat", 8100.0)
        val gasPressure = intent.getDoubleExtra("gasPressure", 1013.0)
        val gasTemperature = intent.getDoubleExtra("gasTemperature", 0.0)
        val tIn = editTIn?.text.toString().toDoubleOrNull() ?: 0.0
        val tOut = editTOut?.text.toString().toDoubleOrNull() ?: 0.0
        val waterFlow = editWaterFlow?.text.toString().toDoubleOrNull() ?: 0.0

        val pressureIn = editPressureIn?.text.toString().toDoubleOrNull() ?: 0.0
        val pressureOut = editPressureOut?.text.toString().toDoubleOrNull() ?: 0.0
        val draftFurnace = editDraftFurnace?.text.toString().toDoubleOrNull() ?: 0.0
        val draftAfterBoiler = editDraftAfterBoiler?.text.toString().toDoubleOrNull() ?: 0.0
        val steamP = editSteamP?.text.toString().toDoubleOrNull() ?: 0.0
        val steamFlow = editSteamFlow?.text.toString().toDoubleOrNull() ?: 0.0

        val co = editCO?.text.toString().toDoubleOrNull() ?: 0.0
        val o2 = editO2?.text.toString().toDoubleOrNull() ?: 0.0
        val co2 = editCO2?.text.toString().toDoubleOrNull() ?: 0.0
        val no = editNO?.text.toString().toDoubleOrNull() ?: 0.0
        val alphaStr = editAlpha?.text.toString().trim()
        val alpha = if (alphaStr.isNotEmpty()) {
            alphaStr.toDoubleOrNull() ?: calcAlpha(o2)
        } else {
            calcAlpha(o2)
        }
        val tAir = editTAir?.text.toString().toDoubleOrNull() ?: 0.0
        val tFlue = editTFlue?.text.toString().toDoubleOrNull() ?: 0.0
        val pAir = editPAir?.text.toString().toDoubleOrNull() ?: 0.0
        val eff = editEff?.text.toString().toDoubleOrNull() ?: 0.0
        val gasFlow = editGasFlow?.text.toString().toDoubleOrNull() ?: 0.0
        val pGas = editPGas?.text.toString().toDoubleOrNull() ?: 0.0
        val tGas = editTGas?.text.toString().toDoubleOrNull() ?: 0.0

        val data = BoilerData(
            boilerType = boilerType,
            serialNumber = serial,
            boilerModel = model,
            power = power,
            burnerModel = burner,
            objectName = objectName,
            address = address,
            tIn = tIn,
            tOut = tOut,
            waterFlow = waterFlow,
            pressureIn = pressureIn,
            pressureOut = pressureOut,
            draftFurnace = draftFurnace,
            draftAfterBoiler = draftAfterBoiler,
            steamPressure = steamP,
            steamFlow = steamFlow,
            co = co,
            o2 = o2,
            co2 = co2,
            no = no,
            alpha = alpha,
            tAir = tAir,
            tFlue = tFlue,
            pAir = pAir,
            efficiency = eff,
            gasFlow = gasFlow,
            pGas = pGas,
            tGas = tGas,
            gasLowerHeat = gasHeat,
            gasPressure = gasPressure,
            gasTemperature = gasTemperature
        )

        val file = ReportGenerator.generatePDFReport(this, data)
        if (file != null) {
            Toast.makeText(this, "Отчёт сохранён:\n${file.name}", Toast.LENGTH_LONG).show()
            openFile(file)
        } else {
            Toast.makeText(this, "Ошибка при создании отчёта", Toast.LENGTH_LONG).show()
        }
    }

    private fun calcAlpha(o2: Double): Double {
        if (o2 <= 0 || o2 >= 21) return 1.0
        return 21.0 / (21.0 - o2)
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Отправить отчёт"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
        }
    }
}