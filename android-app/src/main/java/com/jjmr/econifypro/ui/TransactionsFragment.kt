package com.jjmr.econifypro.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jjmr.econifypro.R
import com.jjmr.econifypro.adapter.TransactionAdapter
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.model.Transaction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.Color
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class TransactionsFragment : Fragment(R.layout.fragment_transactions) {
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var loader: ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvTotalIncomes: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvDelta: TextView
    private lateinit var chipGroup: ChipGroup

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var selectedCategoryId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Vistas
        chipGroup = view.findViewById(R.id.chipGroupCategories)
        tvTotalIncomes = view.findViewById(R.id.tvTotalIncomes)
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        tvDelta = view.findViewById(R.id.tvDelta)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerYear = view.findViewById(R.id.spinnerYear)
        recyclerView = view.findViewById(R.id.rvTransactions)
        loader = view.findViewById(R.id.loader)
        fabAdd = view.findViewById(R.id.fabAddTransaction)

        // Configurar RecyclerView
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        adapter = TransactionAdapter(mutableListOf()) { mostrarOpcionesDialog(it) }
        recyclerView.adapter = adapter

        // Listener de Scroll
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount && firstVisibleItem >= 0) {
                        loadTransactions(currentPage + 1)
                    }
                }

                if (dy > 10 && fabAdd.isShown) fabAdd.hide()
                else if (dy < -10 && !fabAdd.isShown) fabAdd.show()
            }
        })

        setupFilters()
        loadCategoryFilters()
        loadTransactions(1)

        fabAdd.setOnClickListener {
            AddTransactionFragment { resetAndLoad() }.show(parentFragmentManager, "AddTag")
        }
    }

    private fun resetAndLoad() {
        currentPage = 1
        isLastPage = false
        loadTransactions(1)
    }

    private fun loadTransactions(page: Int) {
        if (isLoading) return
        isLoading = true
        loader.visibility = View.VISIBLE

        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        val month = spinnerMonth.selectedItemPosition + 1
        val year = spinnerYear.selectedItem.toString().toInt()

        NetworkConfig.getApiService(requireContext())
            .getTransactions(token, month, year, selectedCategoryId, page)
            .enqueue(object : Callback<List<Transaction>> {
                override fun onResponse(call: Call<List<Transaction>>, response: Response<List<Transaction>>) {
                    loader.visibility = View.GONE
                    isLoading = false

                    if (response.isSuccessful && response.body() != null) {
                        val items = response.body()!!
                        if (items.isEmpty() || (page > 1 && items.size < 10)) {
                            isLastPage = true
                        } else {
                            if (page == 1) adapter.updateAll(items)
                            else adapter.addTransactions(items)
                            currentPage = page
                        }
                        calculateSummary(adapter.currentList)
                    }
                }

                override fun onFailure(call: Call<List<Transaction>>, t: Throwable) {
                    loader.visibility = View.GONE
                    isLoading = false
                    showSnackbar("Error de conexión")
                }
            })
    }

    private fun setupFilters() {
        val months = arrayOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        spinnerMonth.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear downTo 2023).map { it.toString() }
        spinnerYear.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)

        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH))

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { resetAndLoad() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerMonth.onItemSelectedListener = listener
        spinnerYear.onItemSelectedListener = listener
    }

    private fun loadCategoryFilters() {
        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"

        NetworkConfig.getApiService(requireContext()).getCategories(token).enqueue(object : Callback<List<com.jjmr.econifypro.model.Category>> {
            override fun onResponse(call: Call<List<com.jjmr.econifypro.model.Category>>, response: Response<List<com.jjmr.econifypro.model.Category>>) {
                if (response.isSuccessful && response.body() != null) {
                    chipGroup.removeAllViews()
                    val allChip = Chip(context).apply {
                        text = "Todas"; isCheckable = true; isChecked = true
                        setOnClickListener { selectedCategoryId = null; resetAndLoad() }
                    }
                    chipGroup.addView(allChip)

                    response.body()!!.forEach { cat ->
                        val chip = Chip(context).apply {
                            text = cat.name; isCheckable = true
                            setOnClickListener { selectedCategoryId = cat.id; resetAndLoad() }
                        }
                        chipGroup.addView(chip)
                    }
                }
            }
            override fun onFailure(call: Call<List<com.jjmr.econifypro.model.Category>>, t: Throwable) {}
        })
    }

    private fun calculateSummary(transactions: List<Transaction>) {
        val incomes = transactions.filter { it.type == "INGRESO" }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == "GASTO" }.sumOf { it.amount }
        val balance = incomes - expenses

        tvTotalIncomes.text = "+${String.format("%.2f", incomes)}€"
        tvTotalExpenses.text = "-${String.format("%.2f", expenses)}€"
        tvDelta.text = "${String.format("%.2f", balance)}€"
        tvDelta.setTextColor(if (balance >= 0) Color.parseColor("#43A047") else Color.parseColor("#E53935"))
    }

    private fun mostrarOpcionesDialog(transaction: Transaction) {
        val view = layoutInflater.inflate(R.layout.layout_transaction_options, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        view.findViewById<TextView>(R.id.tvOptionTitle).text = transaction.description

        view.findViewById<LinearLayout>(R.id.optionEdit).setOnClickListener {
            AddTransactionFragment(transaction) { resetAndLoad() }.show(parentFragmentManager, "EditTag")
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.optionDelete).setOnClickListener {
            confirmarEliminacion(transaction)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun confirmarEliminacion(t: Transaction) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar transacción?")
            .setMessage("Se borrará '${t.description}'. Esta acción no se puede deshacer.")
            .setPositiveButton("ELIMINAR") { _, _ -> eliminarDeApi(t.id) }
            .setNegativeButton("CANCELAR", null)
            .show()

        // Botón rojo para seguridad del Admin
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.RED)
    }

    private fun eliminarDeApi(id: Int) {
        val sharedPref = requireContext().getSharedPreferences("EconifyPrefs", android.content.Context.MODE_PRIVATE)
        val token = "Bearer ${sharedPref.getString("access_token", "")}"
        NetworkConfig.getApiService(requireContext()).deleteTransaction(token, id).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {
                    showSnackbar("Transacción eliminada")
                    resetAndLoad()
                }
            }
            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                showSnackbar("Error al eliminar")
            }
        })
    }

    // Función auxiliar para no repetir código de Snackbar
    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).apply {
            anchorView = requireActivity().findViewById(R.id.bottom_navigation)
        }.show()
    }
}