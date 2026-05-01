package com.jjmr.econifypro.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jjmr.econifypro.R
import com.jjmr.econifypro.adapter.GoalAdapter
import com.jjmr.econifypro.model.Goal
import com.jjmr.econifypro.api.NetworkConfig
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private lateinit var rvGoals: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var spinnerGoalYear: AutoCompleteTextView
    private lateinit var chipGroupGoalType: ChipGroup
    private lateinit var adapter: GoalAdapter

    // Variable para almacenar los datos originales de la API
    private lateinit var listaCompletaMetas: List<Goal>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar vistas
        rvGoals = view.findViewById(R.id.rvGoals)
        fabAddGoal = view.findViewById(R.id.fabAddGoal)
        spinnerGoalYear = view.findViewById(R.id.spinnerGoalYear)
        chipGroupGoalType = view.findViewById(R.id.chipGroupGoalType)

        // 2. Configurar RecyclerView
        setupRecyclerView()

        // 3. Configurar Listeners de Filtros
        setupFilterListeners()

        // 4. Botón añadir
        fabAddGoal.setOnClickListener {
            val addGoalSheet = AddGoalFragment(null) { loadGoals() }
            addGoalSheet.show(parentFragmentManager, "AddGoalFragment")
        }

        // 5. Cargar datos
        loadGoals()
    }

    private fun setupRecyclerView() {
        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        adapter = GoalAdapter(
            emptyList(),
            onEditClick = { goal ->
                val editSheet = AddGoalFragment(goal) { loadGoals() }
                editSheet.show(parentFragmentManager, "EditGoal")
            },
            onLongClick = { goal ->
                mostrarOpcionesGoal(goal)
            }
        )
        rvGoals.adapter = adapter
    }

    private fun setupFilterListeners() {
        // Listener para el AutoCompleteTextView (Año)
        spinnerGoalYear.setOnItemClickListener { _, _, _, _ ->
            filtrarMetas()
        }

        // Listener para los Chips de Tipo
        chipGroupGoalType.setOnCheckedChangeListener { _, _ ->
            filtrarMetas()
        }
    }

    private fun loadGoals() {
        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).getGoals(token).enqueue(object : Callback<List<Goal>> {
            override fun onResponse(call: Call<List<Goal>>, response: Response<List<Goal>>) {
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyList()
                    listaCompletaMetas = body

                    // Rellenamos el selector de años
                    actualizarSpinnerAnios(body)

                    // Mostramos la lista filtrada inicialmente
                    filtrarMetas()
                }
            }
            override fun onFailure(call: Call<List<Goal>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarSpinnerAnios(metas: List<Goal>) {
        val aniosDisponibles = metas.mapNotNull { meta ->
            meta.startDate?.split("-")?.firstOrNull()
        }.distinct().sortedDescending().toMutableList()

        // Si no hay metas, ponemos el año actual por defecto
        if (aniosDisponibles.isEmpty()) {
            aniosDisponibles.add(Calendar.getInstance().get(Calendar.YEAR).toString())
        }

        val adapterAnios = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, aniosDisponibles)
        spinnerGoalYear.setAdapter(adapterAnios)

        // Establecer el primer año sin abrir el desplegable
        if (aniosDisponibles.isNotEmpty()) {
            spinnerGoalYear.setText(aniosDisponibles[0], false)
        }
    }

    private fun filtrarMetas() {
        if (!::listaCompletaMetas.isInitialized) return

        val anioSeleccionado = spinnerGoalYear.text.toString().trim()
        val tipoSeleccionado = when (chipGroupGoalType.checkedChipId) {
            R.id.chipMonthly -> "MONTHLY_SAVING"
            R.id.chipYearly -> "ANNUAL_SAVING"
            else -> "TODAS"
        }

        val listaFiltrada = listaCompletaMetas.filter { meta ->
            val fechaMeta = meta.startDate ?: ""
            // Si la fecha está vacía permitimos que pase para evitar listas vacías por errores de datos
            val coincideAnio = if (fechaMeta.isEmpty()) true else fechaMeta.contains(anioSeleccionado)

            val coincideTipo = if (tipoSeleccionado == "TODAS") true
            else meta.goalType?.uppercase() == tipoSeleccionado.uppercase()

            coincideAnio && coincideTipo
        }

        adapter.updateList(listaFiltrada)
    }

    private fun mostrarOpcionesGoal(goal: Goal) {
        val view = layoutInflater.inflate(R.layout.layout_goal_options, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<TextView>(R.id.tvGoalOptionTitle).text = goal.name

        view.findViewById<View>(R.id.optionEditGoal).setOnClickListener {
            val editSheet = AddGoalFragment(goal) { loadGoals() }
            editSheet.show(parentFragmentManager, "EditGoal")
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.optionDeleteGoal).setOnClickListener {
            dialog.dismiss()
            confirmarEliminacionGoal(goal)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun confirmarEliminacionGoal(goal: Goal) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Meta")
            .setMessage("¿Estás seguro de que quieres borrar '${goal.name}'? Esta acción no se puede deshacer.")
            .setPositiveButton("ELIMINAR") { _, _ -> ejecutarBorrado(goal) }
            .setNegativeButton("CANCELAR", null)
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            android.graphics.Color.RED
        )
    }

    private fun ejecutarBorrado(goal: Goal) {
        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).deleteGoal(token, goal.id).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    com.google.android.material.snackbar.Snackbar.make(
                        requireView(),
                        "Meta eliminada correctamente",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).apply {
                        // Esto es vital para que flote sobre el menú inferior
                        anchorView = requireActivity().findViewById(R.id.bottom_navigation)
                    }.show() // Solo un show aquí al final

                    loadGoals()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                com.google.android.material.snackbar.Snackbar.make(
                    requireView(),
                    "Error al borrar: ${t.message}",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).apply {
                    anchorView = requireActivity().findViewById(R.id.bottom_navigation)
                    setBackgroundTint(resources.getColor(R.color.dash_red, null))
                }.show()
            }
        })
    }
}