package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("description")
    val description: String,

    @SerializedName("date")
    val date: String? = null,

    @SerializedName("type")
    val type: String, // IMPORTANTE: Debe ser "GASTO" o "INGRESO" (mayúsculas) como en Django

    @SerializedName("category")
    val category: Int, // Enviamos el ID de la categoría

    @SerializedName("goal")
    val goal: Int? = null, // ID de la meta (opcional, para que no dé Unresolved Reference)

    @SerializedName("user")
    val user: Int
)