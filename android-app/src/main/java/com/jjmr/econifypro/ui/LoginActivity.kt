package com.jjmr.econifypro.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import com.jjmr.econifypro.model.LoginRequest
import com.jjmr.econifypro.api.NetworkConfig
import com.jjmr.econifypro.R
import com.jjmr.econifypro.model.TokenResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.ProgressBar

class LoginActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Referencias actualizadas según tu XML
        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)
        val loginProgress = findViewById<ProgressBar>(R.id.loginProgress)

        // 2. PRUEBA DE NAVEGACIÓN (Esto es lo que querías probar)
        goToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 3. Lógica de Login (Actualizada con los nuevos IDs)
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Rellena los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true, loginButton, loginProgress)
            val request = LoginRequest(email, pass)

            NetworkConfig.getApiService(this).login(request).enqueue(object : Callback<TokenResponse> {
            //NetworkConfig.apiService.login(request).enqueue(object : Callback<TokenResponse> {
                override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {

                    setLoading(false, loginButton, loginProgress)

                    if (response.isSuccessful) {
                        val body = response.body()
                        val token = body?.access
                        val name = body?.firstname // Recuperamos el nombre real

                        val sharedPref = getSharedPreferences("EconifyPrefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("access_token", token)
                            putString("user_firstname", name) // ¡Vital para el saludo!
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, "¡Bienvenido a Econify!", Toast.LENGTH_SHORT).show()

                        // 2. Navegar al Dashboard
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    setLoading(false, loginButton, loginProgress)
                    Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}