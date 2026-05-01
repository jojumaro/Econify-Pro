package com.jjmr.econifypro.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.jjmr.econifypro.R
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.Goal
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddGoalFragment(
    private val goalToEdit: Goal? = null,
    private val onGoalSaved: () -> Unit
) : androidx.fragment.app.DialogFragment() { // Cambiado a DialogFragment

    private val typeOptions = listOf(
        Triple("Ahorro Total", "SAVING", "Suma todos tus ingresos menos gastos."),
        Triple("Ahorro Mensual", "MONTHLY_SAVING", "Solo cuenta el balance neto de este mes."),
        Triple("Planificación Anual", "ANNUAL_SAVING", "Objetivo de ahorro para todo el año."),
        Triple("Hito de Inversión", "INVESTMENT", "Metas de inversión o depósitos."),
        Triple("Pago de Deuda", "DEBT_REDUCTION", "Enfocado en reducir el balance negativo.")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_add_goal, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Magia para que sea flotante y centrado
            setBackgroundDrawableResource(android.R.color.transparent)
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<TextInputEditText>(R.id.etGoalName)
        val etTarget = view.findViewById<TextInputEditText>(R.id.etTargetAmount)
        val etInitial = view.findViewById<TextInputEditText>(R.id.etInitialSavings)
        val etDeadline = view.findViewById<TextInputEditText>(R.id.etDeadline)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerGoalType)
        val tvDesc = view.findViewById<TextView>(R.id.tvTypeDescription)
        val btnSave = view.findViewById<Button>(R.id.btnSaveGoal)
        val btnClose = view.findViewById<android.widget.ImageButton>(R.id.btnCloseGoal) // Nuevo botón cerrar

        // Configurar Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeOptions.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                tvDesc.text = typeOptions[position].third
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Cargar datos si es edición
        goalToEdit?.let {
            etName.setText(it.name)
            etTarget.setText(it.targetAmount.toString())
            etInitial.setText(it.currentAmount.toString())
            etDeadline.setText(it.deadline)
            val index = typeOptions.indexOfFirst { opt -> opt.second == it.goalType }
            if (index != -1) spinnerType.setSelection(index)
        }

        etDeadline.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha límite")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(selection))
                etDeadline.setText(dateString)
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        btnClose?.setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val targetStr = etTarget.text.toString()
            val initialStr = etInitial.text.toString()
            val deadline = etDeadline.text.toString()
            val selectedTypeCode = typeOptions[spinnerType.selectedItemPosition].second

            if (name.isEmpty() || targetStr.isEmpty()) {
                Toast.makeText(context, "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ejecutarGuardado(name, targetStr.toDoubleOrNull() ?: 0.0, initialStr.toDoubleOrNull() ?: 0.0, deadline, selectedTypeCode)
        }
    }

    private fun ejecutarGuardado(name: String, target: Double, initial: Double, deadline: String, type: String) {
        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        // 1. Recomendación: Obtener la fecha actual para nuevas metas
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val fechaHoy = sdf.format(java.util.Date())

        val goalData = Goal(
            id = goalToEdit?.id ?: 0,
            name = name,
            targetAmount = target,
            currentAmount = initial,
            deadline = if (deadline.isNotEmpty()) deadline else null,
            // 2. Recomendación: Si editas, mantén la fecha original. Si es nueva, pon la de hoy.
            startDate = goalToEdit?.startDate ?: fechaHoy,
            goalType = type
        )

        val apiService = NetworkConfig.getApiService(requireContext())
        val call = if (goalToEdit == null) apiService.createGoal(token, goalData)
        else apiService.updateGoal(token, goalToEdit!!.id, goalData) // Usamos !! porque ya validamos que no es null

        call.enqueue(object : retrofit2.Callback<Goal> {
            override fun onResponse(call: retrofit2.Call<Goal>, response: retrofit2.Response<Goal>) {
                if (response.isSuccessful) {
                    // Usamos requireActivity().findViewById para que el Snackbar se enganche a la actividad principal
                    // y no muera al cerrar este Fragment.
                    val rootView = requireActivity().findViewById<View>(android.R.id.content)

                    com.google.android.material.snackbar.Snackbar.make(
                        rootView,
                        "Meta guardada correctamente",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).apply {
                        // Lo anclamos al menú inferior de la actividad principal
                        anchorView = requireActivity().findViewById(R.id.bottom_navigation)
                    }.show()

                    onGoalSaved()
                    dismiss()
                } else {
                    // Error del servidor (ej: falta de permisos de Admin o error en SIGLUM)
                    com.google.android.material.snackbar.Snackbar.make(
                        requireView(),
                        "Error al guardar: ${response.code()}",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<Goal>, t: Throwable) {
                com.google.android.material.snackbar.Snackbar.make(
                    requireView(),
                    "Error de red: ${t.message}",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }
}