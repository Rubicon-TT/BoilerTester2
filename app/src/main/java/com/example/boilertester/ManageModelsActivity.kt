package com.example.boilertester

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ManageModelsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var boilerAdapter: ModelAdapter
    private lateinit var burnerAdapter: ModelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_models)

        db = AppDatabase.getDatabase(this)

        // Котлы
        val boilerList = findViewById<RecyclerView>(R.id.recyclerBoilers)
        boilerList.layoutManager = LinearLayoutManager(this)
        boilerAdapter = ModelAdapter { model, isBoiler ->
            if (isBoiler) deleteBoiler(model as BoilerModel) else deleteBurner(model as BurnerModel)
        }
        boilerList.adapter = boilerAdapter

        // Горелки
        val burnerList = findViewById<RecyclerView>(R.id.recyclerBurners)
        burnerList.layoutManager = LinearLayoutManager(this)
        burnerAdapter = ModelAdapter { model, isBoiler ->
            if (isBoiler) deleteBoiler(model as BoilerModel) else deleteBurner(model as BurnerModel)
        }
        burnerList.adapter = burnerAdapter

        // Кнопки добавления
        findViewById<android.widget.Button>(R.id.btnAddBoiler).setOnClickListener { addBoiler() }
        findViewById<android.widget.Button>(R.id.btnAddBurner).setOnClickListener { addBurner() }

        loadModels()
    }

    private fun loadModels() {
        lifecycleScope.launch {
            val boilerList = db.dao().getAllBoilerModels()
            boilerAdapter.updateBoilers(boilerList)

            val burnerList = db.dao().getAllBurnerModels()
            burnerAdapter.updateBurners(burnerList)
        }
    }

    private fun addBoiler() {
        showModelDialog(true)
    }

    private fun addBurner() {
        showModelDialog(false)
    }

    private fun showModelDialog(isBoiler: Boolean) {
        val view = layoutInflater.inflate(R.layout.dialog_model, null)
        val editName = view.findViewById<TextInputEditText>(R.id.editName)
        val editPower = view.findViewById<TextInputEditText>(R.id.editPower)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)
        builder.setTitle(if (isBoiler) "Новая модель котла" else "Новая модель горелки")

        builder.setPositiveButton("Сохранить") { _, _ ->
            val name = editName.text.toString().trim()
            val power = editPower.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && power > 0) {
                lifecycleScope.launch {
                    if (isBoiler) {
                        db.dao().insertBoilerModel(BoilerModel(name, power))
                    } else {
                        db.dao().insertBurnerModel(BurnerModel(name, power))
                    }
                    loadModels()
                }
            }
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun deleteBoiler(model: BoilerModel) {
        lifecycleScope.launch {
            db.dao().deleteBoilerModel(model)
            loadModels()
        }
    }

    private fun deleteBurner(model: BurnerModel) {
        lifecycleScope.launch {
            db.dao().deleteBurnerModel(model)
            loadModels()
        }
    }
}