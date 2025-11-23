package com.example.boilertester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ModelAdapter(
    private val onDeleteClick: (Any, Boolean) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    private var boilerList: List<BoilerModel> = emptyList()
    private var burnerList: List<BurnerModel> = emptyList()
    private var isBoilerList = true

    fun updateBoilers(list: List<BoilerModel>) {
        boilerList = list
        isBoilerList = true
        notifyDataSetChanged()
    }

    fun updateBurners(list: List<BurnerModel>) {
        burnerList = list
        isBoilerList = false
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = if (isBoilerList) boilerList.size else burnerList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (isBoilerList) {
            val model = boilerList[position]
            holder.bind(model.name, model.power) { onDeleteClick(model, true) }
        } else {
            val model = burnerList[position]
            holder.bind(model.name, model.power) { onDeleteClick(model, false) }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textName)
        private val textRange: TextView = itemView.findViewById(R.id.textRange)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(name: String, power: Double, onDelete: () -> Unit) {
            textName.text = name
            textRange.text = "Мощность: ${"%.1f".format(power)} кВт"
            btnDelete.setOnClickListener { onDelete() }
        }
    }
}