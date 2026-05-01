package com.jjmr.econifypro.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.Transaction

class TransactionAdapter(
    private var transactions: MutableList<Transaction>,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    // Propiedad para que el fragmento pueda leer la lista actual para los totales
    val currentList: List<Transaction> get() = transactions

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.tvDescription.text = transaction.description
        holder.tvDate.text = transaction.date ?: "Sin fecha"

        if (transaction.type == "GASTO") {
            holder.tvAmount.text = "-${String.format("%.2f", transaction.amount)}€"
            holder.tvAmount.setTextColor(Color.parseColor("#E53935"))
        } else {
            holder.tvAmount.text = "+${String.format("%.2f", transaction.amount)}€"
            holder.tvAmount.setTextColor(Color.parseColor("#43A047"))
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    // MÉTODO CLAVE: Añade transacciones al final sin resetear el scroll
    fun addTransactions(newItems: List<Transaction>) {
        val startPosition = transactions.size
        transactions.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    // MÉTODO CLAVE: Limpia la lista para cuando cambiamos de mes/filtro
    fun updateAll(newList: List<Transaction>) {
        transactions = newList.toMutableList()
        notifyDataSetChanged()
    }
}