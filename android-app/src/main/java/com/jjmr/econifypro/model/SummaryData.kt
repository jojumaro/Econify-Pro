package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class SummaryData(
    val ingresos: Double,
    val gastos: Double,
    val balance: Double,
    @SerializedName("ahorro_porcentaje")
    val ahorro_porcentaje: Int
)