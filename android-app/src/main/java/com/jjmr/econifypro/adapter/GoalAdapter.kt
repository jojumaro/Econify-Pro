package com.jjmr.econifypro.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.Goal

class GoalAdapter(
    private var goals: List<Goal>,
    private val onEditClick: (Goal) -> Unit,  // <--- Añadido para Editar
    private val onLongClick: (Goal) -> Unit   // <--- Para Borrar
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvGoalName)
        val progress: ProgressBar = view.findViewById(R.id.pbGoal)
        val progressText: TextView = view.findViewById(R.id.tvProgressText)
        val deadline: TextView = view.findViewById(R.id.tvDeadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.name.text = goal.name ?: "Meta sin nombre"
        holder.deadline.text = "Límite: ${goal.deadline ?: "Sin fecha"}"

        val percentage = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100).toInt()
        } else {
            0
        }

        holder.progress.progress = percentage.coerceAtMost(100)
        holder.progressText.text = "${String.format("%.2f", goal.currentAmount)}€ de ${String.format("%.2f", goal.targetAmount)}€ ($percentage%)"

        // CONFIGURACIÓN DE CLICS
        holder.itemView.setOnClickListener {
            onEditClick(goal) // Ahora sí existe la referencia
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(goal)
            true
        }
    }

    override fun getItemCount() = goals.size

    fun updateList(newGoals: List<Goal>) {
        this.goals = newGoals
        notifyDataSetChanged()
    }
}