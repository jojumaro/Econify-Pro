package com.jjmr.econifypro.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        adapter = CategoryAdapter(
            mutableListOf(),
            onDeleteClick = { category ->
                confirmDeleteCategory(category)
            },
            onEditClick = { category ->
                showEditCategoryDialog(category)
            }
        )

        recyclerView.adapter = adapter
        loadCategories()

        val fab = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddCategory)
        fab.setOnClickListener {
            showAddCategoryDialog()
        }

        return view
    }

    private fun loadCategories() {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", "") ?: ""
        println("DEBUG_TOKEN_ENVIADO: Bearer $token")
        NetworkConfig.getApiService(requireContext()).getCategories("Bearer $token")
            .enqueue(object : Callback<List<Category>> {
                override fun onResponse(call: Call<List<Category>>, response: Response<List<Category>>) {
                    if (response.isSuccessful) {
                        response.body()?.let { adapter.updateData(it) }
                    }
                }
                override fun onFailure(call: Call<List<Category>>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error al cargar categorías", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAddCategoryDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)

        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etCategoryName)
        val etDesc = dialogView.findViewById<android.widget.EditText>(R.id.etCategoryDesc)

        builder.setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString()
                val desc = etDesc.text.toString()

                if (name.isNotEmpty()) {
                    saveCategory(Category(name = name, description = desc))
                } else {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", "") ?: ""

        NetworkConfig.getApiService(requireContext())
            .createCategory("Bearer $token", category)
            .enqueue(object : Callback<Category> {
                override fun onResponse(call: Call<Category>, response: Response<Category>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Categoría guardada", Toast.LENGTH_SHORT).show()
                        loadCategories() // Recargamos la lista para ver la nueva
                    } else {
                        Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Category>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmDeleteCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", "") ?: ""

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Categoría")
            .setMessage("¿Estás seguro de que quieres eliminar '${category.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                NetworkConfig.getApiService(requireContext())
                    .deleteCategory("Bearer $token", category.id!!) // Asegúrate de tener este método en ApiService
                    .enqueue(object : Callback<okhttp3.ResponseBody> {
                        override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show()
                                loadCategories() // Recargamos la lista
                            }
                        }
                        override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                            Toast.makeText(requireContext(), "Error al borrar", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)

        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etCategoryName)
        val etDesc = dialogView.findViewById<android.widget.EditText>(R.id.etCategoryDesc)

        // Rellenamos los datos actuales
        etName.setText(category.name)
        etDesc.setText(category.description)

        builder.setView(dialogView)
            .setTitle("Editar Categoría")
            .setPositiveButton("Actualizar") { _, _ ->
                val name = etName.text.toString()
                val desc = etDesc.text.toString()

                if (name.isNotEmpty()) {
                    // Creamos un objeto con los nuevos datos pero el MISMO ID
                    val updatedCategory = Category(id = category.id, name = name, description = desc)
                    updateCategory(updatedCategory)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateCategory(category: Category) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("access_token", "") ?: ""

        NetworkConfig.getApiService(requireContext())
            .updateCategory("Bearer $token", category.id!!, category)
            .enqueue(object : Callback<Category> {
                override fun onResponse(call: Call<Category>, response: Response<Category>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Categoría actualizada", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }
                override fun onFailure(call: Call<Category>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
                }
            })
    }
}