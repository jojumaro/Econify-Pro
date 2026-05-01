package com.jjmr.econifypro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.Category

class CategoryAdapter(
    private var categories: MutableList<Category>,
    private val onLongClick: (Category) -> Unit // Ahora recibimos solo una función para el click largo
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvCategoryName)
        val desc: TextView = view.findViewById(R.id.tvCategoryDesc)
        // Ya no necesitamos btnDelete aquí, las opciones salen al mantener pulsado
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.name.text = category.name
        holder.desc.text = if (category.description.isNullOrEmpty()) "Sin descripción" else category.description

        // Configuramos el click largo en todo el item
        holder.itemView.setOnLongClickListener {
            onLongClick(category)
            true // Indica que el evento ha sido consumido
        }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<Category>) {
        this.categories = newCategories.toMutableList()
        notifyDataSetChanged()
    }
}