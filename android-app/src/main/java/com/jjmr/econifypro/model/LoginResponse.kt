package com.jjmr.econifypro.model
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val access: String,
    val refresh: String,

    @SerializedName("firstname")
    val firstname: String?,

    @SerializedName("lastname")
    val lastname: String?,

    // --- NUEVOS CAMPOS ---
    @SerializedName("security_question_1")
    val securityQuestion1: Int?,

    @SerializedName("security_answer_1")
    val securityAnswer1: String?,

    @SerializedName("security_question_2")
    val securityQuestion2: Int?,

    @SerializedName("security_answer_2")
    val securityAnswer2: String?
)