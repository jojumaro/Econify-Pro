package com.jjmr.econifypro.model
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val access: String,
    val refresh: String,

    @SerializedName("firstname")
    val firstname: String?,

    @SerializedName("lastname")
    val lastname: String?
)