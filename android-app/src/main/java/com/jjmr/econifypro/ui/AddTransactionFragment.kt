package com.jjmr.econifypro.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jjmr.econifypro.R
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.Category
import com.jjmr.econifypro.model.Goal
import com.jjmr.econifypro.model.Transaction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment(
    private val transactionToEdit: Transaction? = null,
    private val onTransactionAdded: () -> Unit
) : DialogFragment() {

    private var categoriesList: List<Category> = listOf()
    private var goalsList: List<Goal> = listOf()

    private lateinit var autoCategory: AutoCompleteTextView
    private lateinit var autoGoal: AutoCompleteTextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_add_transaction, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoCategory = view.findViewById(R.id.autoCompleteCategory)
        autoGoal = view.findViewById(R.id.autoCompleteGoal)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnAddCategoryQuick = view.findViewById<ImageButton>(R.id.btnAddCategoryQuick)

        loadRemoteData()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        transactionToEdit?.let {
            etAmount.setText(it.amount.toString())
            etDescription.setText(it.description)
            etDate.setText(it.date)
            if (it.type == "GASTO") toggleGroup.check(R.id.btnExpense)
            else toggleGroup.check(R.id.btnIncome)
        } ?: run {
            etDate.setText(sdf.format(Date()))
            toggleGroup.check(R.id.btnExpense)
        }

        etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                etDate.setText(sdf.format(calendar.time))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        btnAddCategoryQuick.setOnClickListener { showQuickAddCategoryDialog() }
        btnClose.setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val description = etDescription.text.toString()
            val date = etDate.text.toString()
            val type = if (toggleGroup.checkedButtonId == R.id.btnExpense) "GASTO" else "INGRESO"

            val selectedCatName = autoCategory.text.toString()
            val categoryId = categoriesList.find { it.name == selectedCatName }?.id ?: 0

            val selectedGoalName = autoGoal.text.toString()
            val goalId = goalsList.find { it.name == selectedGoalName }?.id

            if (amount > 0 && description.isNotEmpty() && categoryId != 0) {
                saveTransaction(amount, description, type, categoryId, goalId, date)
            } else {
                showSnackbar("Rellena los campos obligatorios")
            }
        }
    }

    private fun showQuickAddCategoryDialog() {
        val input = EditText(requireContext())
        input.hint = "Ej: Gimnasio, Suscripciones..."
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(64, 24, 64, 24)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva Categoría")
            .setView(container)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) createCategoryApi(name)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createCategoryApi(name: String) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"
        val userId = sharedPref.getInt("user_id", -1)

        val newCategory = Category(id = 0, name = name, user = userId)
        NetworkConfig.getApiService(requireContext()).createCategory(token, newCategory)
            .enqueue(object : Callback<Category> {
                override fun onResponse(call: Call<Category>, response: Response<Category>) {
                    if (response.isSuccessful) {
                        showSnackbar("Categoría creada!")
                        loadRemoteData()
                        autoCategory.setText(name, false)
                    }
                }
                override fun onFailure(call: Call<Category>, t: Throwable) {
                    showSnackbar("Error al crear categoría")
                }
            })
    }

    private fun loadRemoteData() {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"
        val apiService = NetworkConfig.getApiService(requireContext())

        apiService.getCategories(token).enqueue(object : Callback<List<Category>> {
            override fun onResponse(call: Call<List<Category>>, response: Response<List<Category>>) {
                if (response.isSuccessful && response.body() != null) {
                    categoriesList = response.body()!!
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriesList.map { it.name })
                    autoCategory.setAdapter(adapter)

                    transactionToEdit?.let { trans ->
                        val catName = categoriesList.find { it.id == trans.category }?.name
                        autoCategory.setText(catName, false)
                    }
                }
            }
            override fun onFailure(call: Call<List<Category>>, t: Throwable) {}
        })

        apiService.getGoals(token).enqueue(object : Callback<List<Goal>> {
            override fun onResponse(call: Call<List<Goal>>, response: Response<List<Goal>>) {
                if (response.isSuccessful && response.body() != null) {
                    goalsList = response.body()!!.filter { goal ->
                        val type = goal.goalType.uppercase()
                        type != "ANNUAL_SAVING" && type != "MONTHLY_SAVING"
                    }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, goalsList.map { it.name })
                    autoGoal.setAdapter(adapter)

                    transactionToEdit?.goal?.let { gId ->
                        val gName = goalsList.find { it.id == gId }?.name
                        if (gName != null) {
                            autoGoal.setText(gName, false)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<Goal>>, t: Throwable) {}
        })
    }

    private fun saveTransaction(amount: Double, description: String, type: String, categoryId: Int, goalId: Int?, date: String) {
        val sharedPref = requireActivity().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        val transactionData = Transaction(
            id = transactionToEdit?.id ?: 0,
            amount = amount,
            description = description,
            type = type,
            category = categoryId,
            goal = goalId,
            user = sharedPref.getInt("user_id", -1),
            date = date
        )

        val apiService = NetworkConfig.getApiService(requireContext())
        val call = if (transactionToEdit == null) apiService.createTransaction(token, transactionData)
        else apiService.updateTransaction(token, transactionToEdit.id, transactionData)

        call.enqueue(object : Callback<Transaction> {
            override fun onResponse(call: Call<Transaction>, response: Response<Transaction>) {
                if (response.isSuccessful) {
                    // Usamos el content de la actividad para que el mensaje sobreviva al dismiss()
                    val rootView = requireActivity().findViewById<View>(android.R.id.content)
                    Snackbar.make(rootView, "Transacción guardada", Snackbar.LENGTH_LONG).apply {
                        anchorView = requireActivity().findViewById(R.id.bottom_navigation)
                    }.show()

                    onTransactionAdded()
                    dismiss()
                }
            }
            override fun onFailure(call: Call<Transaction>, t: Throwable) {
                showSnackbar("Error al guardar")
            }
        })
    }

    private fun showSnackbar(message: String) {
        val viewForSnack = view ?: requireActivity().findViewById(android.R.id.content)
        Snackbar.make(viewForSnack, message, Snackbar.LENGTH_LONG).apply {
            anchorView = requireActivity().findViewById(R.id.bottom_navigation)
        }.show()
    }
}