package com.jjmr.econifypro.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jjmr.econifypro.R
import com.jjmr.econifypro.adapter.CategoryAdapter
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.Category
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategoriesFragment : Fragment() {

    private lateinit var adapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)

        recyclerView = view.findViewById(R.id.rvCategories)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Callback para el click largo que muestra las opciones
        adapter = CategoryAdapter(mutableListOf()) { category ->
            mostrarOpcionesDialog(category)
        }

        recyclerView.adapter = adapter
        loadCategories()

        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddCategory)
            .setOnClickListener { showCategoryDialog(null) }

        return view
    }

    private fun mostrarOpcionesDialog(category: Category) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_options, null)
        val dialog = android.app.AlertDialog.Builder(requireContext()).setView(view).create()

        // Configuramos los textos específicos para Categorías
        view.findViewById<TextView>(R.id.tvOptionTitle).text = category.name

        // Cambiamos "Editar movimiento" por "Editar categoría"
        val tvEdit = view.findViewById<TextView>(R.id.tvEditLabel)
        tvEdit.text = "Editar categoría"

        view.findViewById<LinearLayout>(R.id.optionEdit).setOnClickListener {
            showCategoryDialog(category)
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.optionDelete).setOnClickListener {
            confirmDeleteCategory(category)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun loadCategories() {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).getCategories(token)
            .enqueue(object : Callback<List<Category>> {
                override fun onResponse(call: Call<List<Category>>, response: Response<List<Category>>) {
                    if (response.isSuccessful && response.body() != null) {
                        adapter.updateData(response.body()!!)
                    }
                }
                override fun onFailure(call: Call<List<Category>>, t: Throwable) {
                    showSnackbar("Error al cargar categorías")
                }
            })
    }

    // He unificado Add y Edit en una sola función para evitar código duplicado
    private fun showCategoryDialog(category: Category?) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etCategoryDesc)

        category?.let {
            etName.setText(it.name)
            etDesc.setText(it.description)
        }

        builder.setView(dialogView)
            .setTitle(if (category == null) "Nueva Categoría" else "Editar Categoría")
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString()
                val desc = etDesc.text.toString()
                val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)

                if (name.isNotEmpty()) {
                    val catData = Category(
                        id = category?.id ?: 0,
                        name = name,
                        description = desc,
                        user = sharedPref.getInt("user_id", -1)
                    )
                    if (category == null) saveCategory(catData) else updateCategory(catData)
                } else {
                    showSnackbar("El nombre es obligatorio")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).createCategory(token, category)
            .enqueue(object : Callback<Category> {
                override fun onResponse(call: Call<Category>, response: Response<Category>) {
                    if (response.isSuccessful) {
                        showSnackbar("Categoría creada")
                        loadCategories()
                    }
                }
                override fun onFailure(call: Call<Category>, t: Throwable) { showSnackbar("Error de red") }
            })
    }

    private fun updateCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).updateCategory(token, category.id!!, category)
            .enqueue(object : Callback<Category> {
                override fun onResponse(call: Call<Category>, response: Response<Category>) {
                    if (response.isSuccessful) {
                        showSnackbar("Categoría actualizada")
                        loadCategories()
                    }
                }
                override fun onFailure(call: Call<Category>, t: Throwable) { showSnackbar("Error de red") }
            })
    }

    private fun confirmDeleteCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar '${category.name}'?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("ELIMINAR") { _, _ ->
                NetworkConfig.getApiService(requireContext()).deleteCategory(token, category.id!!)
                    .enqueue(object : Callback<okhttp3.ResponseBody> {
                        override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                            if (response.isSuccessful) {
                                showSnackbar("Categoría eliminada")
                                loadCategories()
                            }
                        }
                        override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) { showSnackbar("Error al borrar") }
                    })
            }
            .setNegativeButton("CANCELAR", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).apply {
            anchorView = requireActivity().findViewById(R.id.bottom_navigation)
        }.show()
    }
}