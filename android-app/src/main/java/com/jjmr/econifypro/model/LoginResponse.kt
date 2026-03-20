package com.jjmr.econifypro.model

data class LoginResponse(
    val access: String,  // Este es el token que usaremos para las transacciones
    val refresh: String  // Este sirve para renovar la sesión sin pedir password
)