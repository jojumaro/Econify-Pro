package com.jjmr.econifypro.model

data class TokenResponse(
    val access: String,
    val refresh: String,
    val email: String,
    val firstname: String,
    val lastname: String
)