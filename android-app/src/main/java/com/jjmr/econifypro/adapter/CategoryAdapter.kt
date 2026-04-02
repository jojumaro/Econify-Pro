package com.jjmr.econifypro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.Category

class CategoryAdapter(
    private var categories: MutableList<Category>,
    private val onDeleteClick: (Category) -> Unit,
    private val onEditClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvCategoryName)
        val desc: TextView = view.findViewById(R.id.tvCategoryDesc)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.name.text = category.name
        holder.desc.text = category.description ?: "Sin descripción"

        holder.btnDelete.setOnClickListener { onDeleteClick(category) }
        holder.itemView.setOnClickListener { onEditClick(category) }
    }

    override fun getItemCount() = categories.size

    fun updateData(newCategories: List<Category>) {
        this.categories = newCategories.toMutableList()
        notifyDataSetChanged()
    }
}