package com.jjmr.econifypro.model

import com.google.gson.annotations.SerializedName

data class SecurityQuestion(
    val id: Int,
    @SerializedName("question_text") val questionText: String
) {
    // Sobrescribimos toString para que el Spinner muestre el texto
    override fun toString(): String = questionText
}