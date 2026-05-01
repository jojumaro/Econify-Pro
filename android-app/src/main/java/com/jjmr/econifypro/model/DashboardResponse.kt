package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    val summary: SummaryData,
    val categories: List<CategoryData>, // Para el PieChart de sectores
    val goals: List<Goal>?,         // Para el carrusel de metas
    @SerializedName("recent_transactions")
    val recentTransactions: List<Transaction>?,
)

data class CategoryData(
    val name: String,
    val amount: Float,
    val color_hex: String // Para que el color coincida con la Web
)