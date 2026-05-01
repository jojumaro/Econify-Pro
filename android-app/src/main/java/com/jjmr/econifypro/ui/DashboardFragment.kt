package com.jjmr.econifypro.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jjmr.econifypro.R
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.DashboardResponse
import com.jjmr.econifypro.utils.EuroValueFormatter
import com.jjmr.econifypro.utils.showSnackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var pieChart: PieChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", Context.MODE_PRIVATE)
        val firstName = sharedPref.getString("user_firstname", "Usuario")
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        view.findViewById<TextView>(R.id.tvGreeting).text = "Hola, $firstName"
        pieChart = view.findViewById(R.id.chartExpenses)

        view.findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        setupNavigationLinks()
        setupPieChart()
        loadDashboardData(token)
    }

    private fun setupNavigationLinks() {
        val rootView = view ?: return
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)

        rootView.findViewById<TextView>(R.id.btnViewAllGoals).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, GoalsFragment())
                .addToBackStack(null)
                .commit()
            bottomNav?.selectedItemId = R.id.nav_goals
        }

        rootView.findViewById<TextView>(R.id.tvViewAllTransactions).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, TransactionsFragment())
                .addToBackStack(null)
                .commit()
            bottomNav?.selectedItemId = R.id.nav_transactions
        }
    }

    private fun setupPieChart() {
        pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 65f
            description.isEnabled = false
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            animateY(1200)
        }
    }

    private fun loadDashboardData(token: String) {
        NetworkConfig.getApiService(requireContext()).getDashboard(token).enqueue(object : Callback<DashboardResponse> {
            override fun onResponse(call: Call<DashboardResponse>, response: Response<DashboardResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { updateUI(it) }
                } else {
                    // Cambio de Toast por Snackbar (DRY)
                    showSnackbar("No hemos podido actualizar tus finanzas.")
                }
            }
            override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                // Cambio de Toast por Snackbar (DRY)
                showSnackbar("Error de conexión.")
            }
        })
    }

    private fun updateUI(data: DashboardResponse) {
        val localeES = Locale("es", "ES")
        val rootView = view ?: return
        val formatter = EuroValueFormatter()

        // --- 1. Resumen de Cabecera ---
        val expensesTv = rootView.findViewById<TextView>(R.id.tvMonthlyExpenses)
        val balanceTv = rootView.findViewById<TextView>(R.id.tvTotalBalance)

        val totalGastos = data.summary.gastos
        val totalBalance = data.summary.balance
        val ahorroReal = data.summary.ingresos - totalGastos

        expensesTv.text = String.format(localeES, "-%.2f€", totalGastos)
        expensesTv.setTextColor(ContextCompat.getColor(requireContext(), if (totalGastos > 0) R.color.dash_red else R.color.text_primary))

        balanceTv.text = String.format(localeES, "%.2f€", totalBalance)
        if (totalBalance >= 0) {
            balanceTv.text = "+${balanceTv.text}"
            balanceTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dash_green))
        } else {
            balanceTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dash_red))
        }

        // --- 2. Gráfico ---
        val entries = ArrayList<PieEntry>()
        val colorsList = ArrayList<Int>()

        if (totalGastos <= 0 && ahorroReal <= 0) {
            entries.add(PieEntry(1f, ""))
            colorsList.add(Color.LTGRAY)
            pieChart.centerText = "¡Empieza tu plan de ahorro!\nRegistra un movimiento."
        } else {
            pieChart.centerText = ""
            if (totalGastos > 0) {
                entries.add(PieEntry(totalGastos.toFloat(), "Gastos"))
                colorsList.add(ContextCompat.getColor(requireContext(), R.color.dash_red))
            }
            if (ahorroReal > 0) {
                entries.add(PieEntry(ahorroReal.toFloat(), "Ahorro"))
                colorsList.add(ContextCompat.getColor(requireContext(), R.color.dash_green))
            }
        }
        pieChart.data = PieData(PieDataSet(entries, "").apply {
            colors = colorsList
            valueFormatter = PercentFormatter(pieChart)
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        })
        pieChart.invalidate()

        // --- 3. Metas ---
        val goalsContainer = rootView.findViewById<LinearLayout>(R.id.containerGoals)
        goalsContainer?.removeAllViews()

        if (data.goals.isNullOrEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_no_data_placeholder, goalsContainer, false)
            emptyView.findViewById<TextView>(R.id.tvPlaceholderMessage).text = "No tienes metas activas."
            emptyView.setOnClickListener { rootView.findViewById<TextView>(R.id.btnViewAllGoals)?.performClick() }
            goalsContainer?.addView(emptyView)
        } else {
            data.goals?.forEach { goal ->
                val goalView = layoutInflater.inflate(R.layout.item_goal_card, goalsContainer, false)
                goalView.findViewById<TextView>(R.id.tvGoalTitle).text = goal.name
                val pb = goalView.findViewById<ProgressBar>(R.id.pbGoal)

                pb.max = goal.targetAmount.toInt()
                pb.progress = goal.currentAmount.toInt()

                val actual = formatter.getFormattedValue(goal.currentAmount.toFloat())
                val objetivo = formatter.getFormattedValue(goal.targetAmount.toFloat())
                goalView.findViewById<TextView>(R.id.tvGoalAmount).text = "$actual / $objetivo"
                goalsContainer?.addView(goalView)
            }
        }

        // --- 4. Movimientos ---
        val transContainer = rootView.findViewById<LinearLayout>(R.id.containerRecentTransactions)
        transContainer?.removeAllViews()

        if (data.recentTransactions.isNullOrEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_no_data_placeholder, transContainer, false)
            emptyView.findViewById<TextView>(R.id.tvPlaceholderMessage).text = "Aún no hay movimientos."
            emptyView.setOnClickListener { rootView.findViewById<TextView>(R.id.tvViewAllTransactions)?.performClick() }
            transContainer?.addView(emptyView)
        } else {
            data.recentTransactions?.take(3)?.forEach { transaction ->
                val row = layoutInflater.inflate(R.layout.item_transaction_mini, transContainer, false)
                row.findViewById<TextView>(R.id.tvMiniName).text = transaction.description
                val tvAmount = row.findViewById<TextView>(R.id.tvMiniAmount)

                val isGasto = transaction.type.equals("GASTO", ignoreCase = true)
                tvAmount.text = String.format(localeES, if (isGasto) "-%.2f€" else "+%.2f€", transaction.amount)
                tvAmount.setTextColor(ContextCompat.getColor(requireContext(), if (isGasto) R.color.dash_red else R.color.dash_green))

                transContainer?.addView(row)
            }
        }
    }
}